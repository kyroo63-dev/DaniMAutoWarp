#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
FLAC="${1:?Provide a FLAC input path}"
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
cat > "$TMP/Real.kt" <<'KOTLIN'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.*
import java.io.File
fun main(args:Array<String>) {
 for(path in args) {
  val bytes=File(path).readBytes(); val samples=FloatArray(bytes.size/4)
  java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(samples)
  val r=BeatAnalyzer.analyze(PcmAudio(8000,1,samples))
  check(r.confidence in 0.0..0.95 && r.confidence.isFinite())
  check(r.markers.zipWithNext().all { (a,b)-> a.sourceSeconds < b.sourceSeconds && a.targetSeconds < b.targetSeconds })
  // A low-confidence analysis must never introduce an automatic time deformation.
  if (r.confidence < 0.20) check(r.markers.all { kotlin.math.abs(it.sourceSeconds - it.targetSeconds) < 1e-8 })
  check(r.kickPhaseSegments != null && r.kickPhaseWindows.size == 5)
  check(!r.kickPhaseSegments!!.eligibleForAutomaticWarp)
  println("  integrated phase segments=${r.kickPhaseSegments!!.segments.size} rejected=${r.kickPhaseSegments!!.rejectedWindows}")
  val drift=KickPhaseDrift.inspect(r.kickTimes, r.bpm, samples.size / 8000.0)
  val reliable=drift.count { it.reliable }
  val jumps=drift.zipWithNext().mapNotNull { (a,b) -> KickPhaseDrift.phaseDifferenceMs(a,b,r.bpm) }
  println("  phase windows=${drift.size} reliable=$reliable phase transitions=${jumps.size} maxAdjacentShiftMs=${jumps.maxOfOrNull { kotlin.math.abs(it) }?.let { "%.1f".format(it) } ?: "n/a"}")
  println("${File(path).name}: BPM=${"%.2f".format(r.bpm)} confidence=${"%.3f".format(r.confidence)} markers=${r.markers.size} kicks=${r.kickTimes.size}")
 }
}
KOTLIN
A=app/src/main/java/com/danim/autowarp/analysis
kotlinc "$TMP/Source.kt" app/src/main/java/com/danim/autowarp/audio/PcmWave.kt "$A/BeatAnalyzer.kt" "$A/KickPulseAudit.kt" "$A/KickPhaseDrift.kt" "$A/KickPhaseSegments.kt" "$A/KickPhaseEvidence.kt" "$A/SpectralKickEvidence.kt" "$A/BarAlignmentAnalyzer.kt" "$A/WarpSafetyGate.kt" "$A/WarpDiagnostics.kt" "$A/ZoneConfidence.kt" "$A/TempoContinuityGate.kt" "$TMP/Real.kt" -include-runtime -d "$TMP/real.jar"
# Sample six minute-long windows across the complete 8m27s recording.
paths=()
for start in 0 80 160 240 320 420; do
  ffmpeg -nostdin -loglevel error -ss "$start" -t 60 -i "$FLAC" -ac 1 -ar 8000 -f f32le "$TMP/at_${start}s.f32"
  paths+=("$TMP/at_${start}s.f32")
done
java -Xmx1g -jar "$TMP/real.jar" "${paths[@]}"
