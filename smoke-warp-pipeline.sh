#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/com/danim/autowarp/analysis"
cat > "$TMP/com/danim/autowarp/analysis/Models.kt" <<'KOTLIN'
package com.danim.autowarp.analysis
data class WarpMarker(val sourceSeconds: Double, val targetSeconds: Double, val strength: Float = 1f)
enum class TransientKind { KICK, SNARE, HI_HAT, PERCUSSION }
data class TransientEvent(val seconds: Double, val kind: TransientKind, val confidence: Double)
KOTLIN
cat > "$TMP/Pipeline.kt" <<'KOTLIN'
import com.danim.autowarp.analysis.WarpMarker
import com.danim.autowarp.audio.*
import java.io.ByteArrayOutputStream
import kotlin.math.*
fun main() {
    val sr=8000
    val frames=sr*2
    val stereo=FloatArray(frames*2)
    for(i in 0 until frames) {
        val pulse=if(i%4000<70) (1.0-i%4000/70.0)*0.7 else 0.0
        val tone=sin(2*PI*220*i/sr)*0.12
        stereo[2*i]=(pulse+tone).toFloat()
        stereo[2*i+1]=(pulse-tone).toFloat()
    }
    val input=PcmAudio(sr,2,stereo)
    val markers=listOf(WarpMarker(0.0,0.0),WarpMarker(0.9,1.0),WarpMarker(2.0,2.2))
    val rendered=WsolaWarp.render(input,markers,quality=WarpQuality.FAST)
    check(rendered.channels==2 && rendered.sampleRate==sr)
    check(abs(rendered.frames-17600)<=1) { "Wrong duration ${rendered.frames}" }
    check(rendered.samples.all { it.isFinite() && abs(it)<=1.0f })
    check(rendered.samples.any { abs(it)>0.01f })
    val bytes=ByteArrayOutputStream().also { PcmWave.write(rendered,it) }.toByteArray()
    val decoded=PcmWave.read(bytes.inputStream())
    check(decoded.frames==rendered.frames && decoded.channels==2)
    check(decoded.samples.any { abs(it)>0.01f })
    check(runCatching { WsolaWarp.render(input,listOf(WarpMarker(0.0,0.0),WarpMarker(1.0,0.0))) }.isFailure)
    println("PASS: stereo WSOLA -> PCM16 WAV -> decode; duration, finite samples, marker validation")
}
KOTLIN
kotlinc "$TMP/com/danim/autowarp/analysis/Models.kt" app/src/main/java/com/danim/autowarp/audio/PcmWave.kt app/src/main/java/com/danim/autowarp/audio/WsolaWarp.kt "$TMP/Pipeline.kt" -include-runtime -d "$TMP/pipeline.jar"
java -jar "$TMP/pipeline.jar"
