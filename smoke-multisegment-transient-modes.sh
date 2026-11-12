#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
INPUT="${1:-/mnt/data/Javi Golo & Andres Gomes - Expanden.flac}"
[[ -f "$INPUT" ]] || { echo "SKIP: recording unavailable"; exit 2; }
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

cat > "$TMP/Stubs.kt" <<'KT'
package com.danim.autowarp.analysis
data class WarpMarker(val sourceSeconds:Double,val targetSeconds:Double,val strength:Float=1f)
data class TransientEvent(val seconds:Double,val confidence:Double)
KT
cat > "$TMP/Quality.kt" <<'KT'
package com.danim.autowarp.audio
enum class WarpQuality(val grainMs:Int,val searchMs:Int,val step:Int) { FAST(48,6,8),BALANCED(72,10,4),HIGH(96,14,2) }
data class PcmAudio(val sampleRate:Int,val channels:Int,val samples:FloatArray) { val frames get()=samples.size/channels }
KT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.*
import java.io.*
import kotlin.math.*
fun mono(samples:FloatArray)=FloatArray(samples.size/2) { (samples[2*it]+samples[2*it+1])*0.5f }
fun main(args:Array<String>) {
 Pcm16WavFileSource(File(args[0])).use { src ->
  check(src.sampleRate==44100 && src.channels==2)
  val original=mono(src.readFrames(0,src.frameCount.toInt()))
  val rate=src.sampleRate; val duration=src.frameCount.toDouble()/rate
  // Local maxima near half-second beats: high-energy proxies, NOT verified kick events.
  val candidates=(1..5).map { i ->
   val center=(i*0.5*rate).toInt()
   val start=(center-0.08*rate).toInt().coerceAtLeast(0)
   val end=(center+0.08*rate).toInt().coerceAtMost(original.lastIndex)
   (start..end).maxByOrNull { abs(original[it]) }!!.toDouble()/rate
  }.distinct().sorted()
  check(candidates.size>=4)
  println("Stereo 44.1kHz; candidate high-energy events=${candidates.size}; unverified kick labels")
  for(factor in listOf(0.99,1.01)) {
   val summaries=mutableMapOf<TransientAnchorMode,TransientFidelityReport.Summary>()
   for(mode in TransientAnchorMode.entries) {
    val out=ByteArrayOutputStream()
    val report=StreamingWsolaProcessor.renderWav(src,listOf(WarpMarker(0.0,0.0),WarpMarker(duration,duration*factor)),out,WarpQuality.FAST,candidates.map { TransientEvent(it,0.95) },anchorMode=mode)
    val bytes=out.toByteArray()
    check(bytes.size.toLong()==44L+report.outputFrames*4L)
    check(report.maxSourceFramesBuffered<rate && report.grainsProcessed>0)
    val processed=FloatArray(report.outputFrames.toInt()*2) { i ->
      val p=44+i*2
      (((bytes[p].toInt() and 255) or (bytes[p+1].toInt() shl 8)).toShort().toInt()/32768f)
    }
    val summary=TransientFidelityReport.analyze(original,mono(processed),rate,candidates.map { it to it*factor })
    check(summary.rows.size==candidates.size && summary.invalidEnergyRows==0)
    check(summary.meanEnergyRatio.isFinite() && summary.meanAbsoluteTimingErrorMs.isFinite())
    summaries[mode]=summary
    File(args[1],"${args[2]}_${factor}_${mode}.csv").writeText(TransientFidelityReport.csv(summary))
    println("factor=$factor mode=$mode errorMs=${"%.2f".format(summary.meanAbsoluteTimingErrorMs)} maxErrorMs=${"%.2f".format(summary.maxAbsoluteTimingErrorMs)} energyRatio=${"%.3f".format(summary.meanEnergyRatio)} anchors=${report.lockedTransientAnchors}")
   }
   val legacy=summaries.getValue(TransientAnchorMode.LEGACY)
   val adaptive=summaries.getValue(TransientAnchorMode.ADAPTIVE)
   println("DELTA adaptive-legacy factor=$factor errorMs=${"%.2f".format(adaptive.meanAbsoluteTimingErrorMs-legacy.meanAbsoluteTimingErrorMs)} energyRatio=${"%.3f".format(adaptive.meanEnergyRatio-legacy.meanEnergyRatio)}")
  }
  println("PASS: 6 real-audio stereo 44.1kHz renders with event anchors and CSV; quality improvement not assumed")
 }
}
KT
kotlinc "$TMP/Stubs.kt" "$TMP/Quality.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWsolaProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/TransientFidelity.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/TransientFidelityReport.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
for offset in 30 75 150 240 360; do
  ffmpeg -nostdin -hide_banner -loglevel error -y -ss "$offset" -t 3 -i "$INPUT" -ac 2 -ar 44100 -c:a pcm_s16le "$TMP/source.wav"
  echo "===== source offset ${offset}s ====="
  java -jar "$TMP/test.jar" "$TMP/source.wav" "$TMP" "$offset"
done
