#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Stubs.kt" <<'KT'
package com.danim.autowarp.analysis
data class WarpMarker(val sourceSeconds: Double, val targetSeconds: Double, val strength: Float = 1f)
data class TransientEvent(val seconds: Double, val confidence: Double)
KT
cat > "$TMP/Quality.kt" <<'KT'
package com.danim.autowarp.audio
enum class WarpQuality(val grainMs: Int, val searchMs: Int, val step: Int) { FAST(48,6,8), BALANCED(72,10,4), HIGH(96,14,2) }
data class PcmAudio(val sampleRate: Int, val channels: Int, val samples: FloatArray) { val frames get() = samples.size / channels }
KT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.*
import java.io.ByteArrayOutputStream
import kotlin.math.*
fun main() {
    val rate=8000; val frames=rate*3; val hits=listOf(0.5,1.0,1.5,2.0,2.5)
    val pcm=FloatArray(frames) { i ->
        val t=i.toDouble()/rate
        hits.sumOf { h -> val d=t-h
            if (d>=0 && d<0.09) 0.75*exp(-d*65)*sin(2*PI*75*d)+0.15*exp(-d*180)*sin(2*PI*1600*d) else 0.0
        }.toFloat().coerceIn(-0.99f,0.99f)
    }
    val src=object:PcmFrameSource {
        override val sampleRate=rate; override val channels=1; override val frameCount=frames.toLong()
        override fun readFrames(firstFrame:Long,count:Int)=pcm.copyOfRange(firstFrame.toInt(),firstFrame.toInt()+count)
    }
    for (duration in listOf(2.85,3.0,3.15)) {
      for (mode in TransientAnchorMode.entries) {
        val out=ByteArrayOutputStream()
        val report=StreamingWsolaProcessor.renderWav(src,listOf(WarpMarker(0.0,0.0),WarpMarker(3.0,duration)),out,WarpQuality.BALANCED,hits.map { TransientEvent(it,0.95) }, anchorMode=mode)
        val bytes=out.toByteArray()
        check(bytes.size==44+report.outputFrames.toInt()*2)
        val rendered=FloatArray(report.outputFrames.toInt()) { i ->
            val p=44+i*2; (((bytes[p].toInt() and 255) or (bytes[p+1].toInt() shl 8)).toShort().toInt()/32768f)
        }
        val positions=hits.map { it to it*duration/3.0 }
        val fidelity=TransientFidelityReport.analyze(pcm,rendered,rate,positions)
        check(fidelity.rows.size==5)
        check(fidelity.rows.all { it.timingErrorMs.isFinite() && it.energyRatio.isFinite() })
        check(fidelity.maxAbsoluteTimingErrorMs<35.0) { "Attack outside tolerance: $fidelity" }
        check(fidelity.meanEnergyRatio>0.05) { "Severe energy loss: $fidelity" }
        check(TransientFidelityReport.csv(fidelity).lines().size==7)
        println("mode=$mode duration=$duration attacks=${fidelity.rows.size} maxErrorMs=${"%.2f".format(fidelity.maxAbsoluteTimingErrorMs)} meanEnergyRatio=${"%.3f".format(fidelity.meanEnergyRatio)}")
      }
    }
    println("PASS: all three WSOLA anchor modes, per-kick fidelity and CSV")
}
KT
kotlinc "$TMP/Stubs.kt" "$TMP/Quality.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWsolaProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/TransientFidelity.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/TransientFidelityReport.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
