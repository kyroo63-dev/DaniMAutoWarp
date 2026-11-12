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
enum class WarpQuality(val grainMs: Int, val searchMs: Int, val step: Int) {
    FAST(48,6,8), BALANCED(72,10,4), HIGH(96,14,2)
}
data class PcmAudio(val sampleRate: Int, val channels: Int, val samples: FloatArray) {
    val frames get() = samples.size / channels
}
KT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.*
import java.io.ByteArrayOutputStream
import kotlin.math.*

fun estimateFrequency(bytes: ByteArray, rate: Int): Double {
    val start = 44 + rate / 2 * 2
    val end = min(bytes.size, start + rate * 2)
    var crosses = 0
    var prev = 0
    for (p in start until end step 2) {
        val v = (bytes[p].toInt() and 255) or (bytes[p+1].toInt() shl 8)
        val sample = v.toShort().toInt()
        if (prev < 0 && sample >= 0) crosses++
        prev = sample
    }
    return crosses.toDouble() * rate / ((end-start)/2)
}
fun main() {
    val rate=8000; val frames=rate*3
    val source=object:PcmFrameSource {
        override val sampleRate=rate
        override val channels=1
        override val frameCount=frames.toLong()
        override fun readFrames(firstFrame:Long,count:Int)=FloatArray(count) { i ->
            (0.55*sin(2*PI*440.0*(firstFrame+i)/rate)).toFloat()
        }
    }
    for (target in listOf(2.7,3.0,3.3)) {
        val out=ByteArrayOutputStream()
        val report=StreamingWsolaProcessor.renderWav(source,listOf(WarpMarker(0.0,0.0),WarpMarker(3.0,target)),out,WarpQuality.FAST)
        check(report.pitchPreserving)
        check(report.outputFrames==round(target*rate).toLong())
        check(out.size()==44+report.outputFrames.toInt()*2)
        val hz=estimateFrequency(out.toByteArray(),rate)
        println("target=$target frequency=$hz Hz maxBuffered=${report.maxSourceFramesBuffered}")
        check(hz in 420.0..460.0) { "La frecuencia no se ha conservado: $hz Hz" }
        check(report.maxSourceFramesBuffered < rate)
    }
    println("PASS: WSOLA streaming, tono, duración y memoria acotada")
}
KT
kotlinc "$TMP/Stubs.kt" "$TMP/Quality.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWsolaProcessor.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
