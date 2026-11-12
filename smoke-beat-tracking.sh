#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Source.kt" <<'KOTLIN'
package com.danim.autowarp.audio
interface PcmFrameSource { val sampleRate:Int; val channels:Int; val frameCount:Long; fun readFrames(startFrame:Long, frames:Int):FloatArray }
class PcmAudioFrameSource(private val audio:PcmAudio):PcmFrameSource {
 override val sampleRate get()=audio.sampleRate
 override val channels get()=audio.channels
 override val frameCount get()=audio.frames.toLong()
 override fun readFrames(startFrame:Long,frames:Int):FloatArray = audio.samples.copyOfRange((startFrame*channels).toInt(), ((startFrame+frames)*channels).toInt())
}
KOTLIN
cat > "$TMP/BeatSmoke.kt" <<'KOTLIN'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.*
import kotlin.math.*
fun main() {
 val sr=8000; val duration=14.0; val samples=FloatArray((sr*duration).toInt())
 val kicks=mutableListOf<Double>(); var t=0.32; var beat=0
 while(t<duration-0.3) {
  kicks.add(t)
  val start=(t*sr).toInt()
  for(i in 0 until 600) { val ix=start+i; if(ix>=samples.size) break
   val env=exp(-i/125.0)
   samples[ix]+=(0.75*env*sin(2*PI*(75.0-20.0*i/600.0)*i/sr)).toFloat()
  }
  // Add a short high-frequency snare on alternate beats.
  if(beat%2==1) for(i in 0 until 120) { val ix=start+i; if(ix<samples.size) samples[ix]+=(0.09*(1-i/120.0)*sin(2*PI*1900*i/sr)).toFloat() }
  t+=0.5+0.014*sin(beat*0.29); beat++
 }
 val result=BeatAnalyzer.analyze(PcmAudio(sr,1,samples))
 check(result.bpm.isFinite() && result.bpm in 100.0..140.0) { "Unexpected BPM ${result.bpm}" }
 check(result.beatTimes.size>=12) { "Too few beats ${result.beatTimes.size}" }
 check(result.markers.zipWithNext().all { (a,b)->a.sourceSeconds<b.sourceSeconds && a.targetSeconds<b.targetSeconds }) { "Non-monotonic markers" }
 check(result.markers.all { it.sourceSeconds.isFinite() && it.targetSeconds.isFinite() })
 check(result.tempoCurve.all { it.first.isFinite() && it.second.isFinite() })
 println("PASS: varying-tempo kick analysis BPM=${"%.2f".format(result.bpm)} beats=${result.beatTimes.size} markers=${result.markers.size}")
}
KOTLIN
ANALYSIS=app/src/main/java/com/danim/autowarp/analysis
kotlinc "$TMP/Source.kt" app/src/main/java/com/danim/autowarp/audio/PcmWave.kt \
 "$ANALYSIS/BeatAnalyzer.kt" "$ANALYSIS/KickOverlapEvidence.kt" "$ANALYSIS/KickDensityGate.kt" "$ANALYSIS/KickRhythmEvidence.kt" "$ANALYSIS/KickSequenceGate.kt" "$ANALYSIS/KickPulseAudit.kt" "$ANALYSIS/KickPhaseDrift.kt" "$ANALYSIS/KickPhaseSegments.kt" "$ANALYSIS/KickPhaseEvidence.kt" "$ANALYSIS/SpectralKickEvidence.kt" "$ANALYSIS/BarAlignmentAnalyzer.kt" \
 "$ANALYSIS/WarpSafetyGate.kt" "$ANALYSIS/WarpDiagnostics.kt" "$ANALYSIS/ZoneConfidence.kt" "$ANALYSIS/TempoContinuityGate.kt" \
 "$TMP/BeatSmoke.kt" -include-runtime -d "$TMP/beat.jar"
java -jar "$TMP/beat.jar"
