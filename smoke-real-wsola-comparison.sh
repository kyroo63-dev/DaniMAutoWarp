#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
INPUT="${1:-/mnt/data/Javi Golo & Andres Gomes - Expanden.flac}"
[[ -f "$INPUT" ]] || { echo "SKIP: missing recording $INPUT"; exit 2; }
command -v ffmpeg >/dev/null; command -v kotlinc >/dev/null
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
ffmpeg -nostdin -hide_banner -loglevel error -y -ss 75 -t 8 -i "$INPUT" -ac 1 -ar 8000 -c:a pcm_s16le "$TMP/real.wav"
cat > "$TMP/Stubs.kt" <<'KT'
package com.danim.autowarp.analysis
data class WarpMarker(val sourceSeconds: Double, val targetSeconds: Double, val strength: Float = 1f)
data class TransientEvent(val seconds: Double, val confidence: Double)
KT
cat > "$TMP/Quality.kt" <<'KT'
package com.danim.autowarp.audio
enum class WarpQuality(val grainMs: Int, val searchMs: Int, val step: Int) { FAST(48,6,8), BALANCED(72,10,4), HIGH(96,14,2) }
data class PcmAudio(val sampleRate:Int,val channels:Int,val samples:FloatArray) { val frames get()=samples.size/channels }
KT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.*
import java.io.*
import kotlin.math.*
fun main(args:Array<String>) {
  Pcm16WavFileSource(File(args[0])).use { src ->
    val rate=src.sampleRate
    check(rate==8000 && src.channels==1)
    val original=src.readFrames(0,src.frameCount.toInt())
    val duration=src.frameCount.toDouble()/rate
    // Local high-energy candidates are proxies, not verified kick classifications.
    val candidates=(1..13).map { i ->
      val center=(i*0.5*rate).toInt()
      val start=(center-0.11*rate).toInt().coerceAtLeast(0)
      val end=(center+0.11*rate).toInt().coerceAtMost(original.lastIndex)
      (start..end).maxByOrNull { abs(original[it]) }!!.toDouble()/rate
    }.distinct().filter { it>0.1 && it<duration-0.1 }.sorted()
    check(candidates.size>=8)
    println("REAL INPUT: ${duration}s, candidates=${candidates.size} (energy proxies, not kick labels)")
    for (factor in listOf(0.95,1.0,1.05)) {
      val summaries=mutableMapOf<TransientAnchorMode,TransientFidelityReport.Summary>()
      for (mode in TransientAnchorMode.entries) {
        val bytes=ByteArrayOutputStream()
        val report=StreamingWsolaProcessor.renderWav(src,listOf(WarpMarker(0.0,0.0),WarpMarker(duration,duration*factor)),bytes,WarpQuality.BALANCED,candidates.map { TransientEvent(it,0.95) },anchorMode=mode)
        val data=bytes.toByteArray()
        check(data.size.toLong()==44L+report.outputFrames*2L)
        val processed=FloatArray(report.outputFrames.toInt()) { i ->
          val p=44+i*2; (((data[p].toInt() and 255) or (data[p+1].toInt() shl 8)).toShort().toInt()/32768f)
        }
        val positions=candidates.map { it to it*factor }
        val summary=TransientFidelityReport.analyze(original,processed,rate,positions)
        check(summary.rows.size==candidates.size && summary.invalidEnergyRows==0)
        check(summary.meanEnergyRatio.isFinite() && summary.meanAbsoluteTimingErrorMs.isFinite())
        check(report.maxSourceFramesBuffered < rate)
        summaries[mode]=summary
        File(args[1],"real_wsola_${factor}_${mode}.csv").writeText(TransientFidelityReport.csv(summary))
        println("factor=$factor mode=$mode meanErrorMs=${"%.2f".format(summary.meanAbsoluteTimingErrorMs)} maxErrorMs=${"%.2f".format(summary.maxAbsoluteTimingErrorMs)} energyRatio=${"%.3f".format(summary.meanEnergyRatio)}")
      }
      val legacy=summaries.getValue(TransientAnchorMode.LEGACY)
      val adaptive=summaries.getValue(TransientAnchorMode.ADAPTIVE)
      println("DELTA adaptive-legacy factor=$factor errorMs=${"%.2f".format(adaptive.meanAbsoluteTimingErrorMs-legacy.meanAbsoluteTimingErrorMs)} energy=${"%.3f".format(adaptive.meanEnergyRatio-legacy.meanEnergyRatio)}")
    }
    println("PASS: real recording, 9 renders, bounded memory, valid output and per-event CSV. Quality improvement NOT asserted.")
  }
}
KT
kotlinc "$TMP/Stubs.kt" "$TMP/Quality.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWsolaProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/TransientFidelity.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/TransientFidelityReport.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar" "$TMP/real.wav" "$TMP"
