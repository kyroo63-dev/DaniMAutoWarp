#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Source.kt" <<'KT'
package com.danim.autowarp.audio
interface PcmFrameSource { val sampleRate:Int; val channels:Int; val frameCount:Long; fun readFrames(startFrame:Long, frames:Int):FloatArray }
class PcmAudioFrameSource(private val a:PcmAudio):PcmFrameSource {
 override val sampleRate get()=a.sampleRate
 override val channels get()=a.channels
 override val frameCount get()=a.frames.toLong()
 override fun readFrames(startFrame:Long,frames:Int)=a.samples.copyOfRange((startFrame*channels).toInt(),((startFrame+frames)*channels).toInt())
}
KT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.*
fun main() {
 for (samples in listOf(FloatArray(8000*5), FloatArray(8000*5) { if (it%1000==0) 0.8f else 0f })) {
  val r=BeatAnalyzer.analyze(PcmAudio(8000,1,samples))
  check(r.confidence == 0.0 || r.markers.all { it.sourceSeconds == it.targetSeconds })
  if (r.beatTimes.isEmpty()) { check(r.confidence == 0.0); check(r.markers.size == 2) }
  println("PASS: low-confidence safe fallback BPM=${r.bpm} markers=${r.markers.size}")
 }
}
KT
A=app/src/main/java/com/danim/autowarp/analysis
kotlinc "$TMP/Source.kt" app/src/main/java/com/danim/autowarp/audio/PcmWave.kt "$A/BeatAnalyzer.kt" "$A/KickPulseAudit.kt" "$A/KickPhaseDrift.kt" "$A/KickPhaseSegments.kt" "$A/KickPhaseEvidence.kt" "$A/SpectralKickEvidence.kt" "$A/BarAlignmentAnalyzer.kt" "$A/WarpSafetyGate.kt" "$A/WarpDiagnostics.kt" "$A/ZoneConfidence.kt" "$A/TempoContinuityGate.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
