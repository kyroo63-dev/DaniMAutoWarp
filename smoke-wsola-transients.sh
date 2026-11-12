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
        var v=0.0
        for(h in hits) {
            val d=t-h
            if(d>=0 && d<0.09) v+=0.75*exp(-d*65)*sin(2*PI*75*d) + 0.15*exp(-d*180)*sin(2*PI*1600*d)
        }
        v.toFloat().coerceIn(-0.99f,0.99f)
    }
    val src=object:PcmFrameSource {
        override val sampleRate=rate; override val channels=1; override val frameCount=frames.toLong()
        override fun readFrames(firstFrame:Long,count:Int)=pcm.copyOfRange(firstFrame.toInt(),firstFrame.toInt()+count)
    }
    for (duration in listOf(2.85,3.0,3.15)) {
        val out=ByteArrayOutputStream()
        val report=StreamingWsolaProcessor.renderWav(src,listOf(WarpMarker(0.0,0.0),WarpMarker(3.0,duration)),out,WarpQuality.BALANCED,hits.map { TransientEvent(it,0.95) })
        val bytes=out.toByteArray()
        check(bytes.size==44+report.outputFrames.toInt()*2)
        check(report.outputFrames==round(duration*rate).toLong())
        check(report.maxSourceFramesBuffered<rate)
        check(report.lockedTransientAnchors>0)
        val samples=FloatArray(report.outputFrames.toInt()) { i ->
            val p=44+i*2; (((bytes[p].toInt() and 255) or (bytes[p+1].toInt() shl 8)).toShort().toInt()/32768f)
        }
        val peak=samples.maxOf { abs(it) }
        check(samples.all { it.isFinite() }); check(peak in 0.1f..1.0f)
        val errors=hits.map { hit ->
            val expected=hit*duration/3.0
            val center=(expected*rate).roundToInt()
            val from=(center-rate/25).coerceAtLeast(0); val to=(center+rate/25).coerceAtMost(samples.lastIndex)
            val local=(from..to).maxOf { abs(samples[it]) }
            check(local>0.08f) { "Transient missing near $expected s: peak=$local" }
            local
        }
        println("duration=$duration anchors=${report.lockedTransientAnchors} peak=$peak transientPeaks=${errors.joinToString(",") { "%.3f".format(it) }}")
    }
    println("PASS: WSOLA synthetic kick transients, anchors, duration, finite PCM, bounded source window")
}
KT
kotlinc "$TMP/Stubs.kt" "$TMP/Quality.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWsolaProcessor.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
