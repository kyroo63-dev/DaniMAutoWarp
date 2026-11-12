#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

fun main(args: Array<String>) {
    val file = File(args[0])
    val rate = 44100
    val frames = 100_000
    val source = object : PcmFrameSource {
        override val sampleRate = rate
        override val channels = 2
        override val frameCount = frames.toLong()
        override fun readFrames(firstFrame: Long, count: Int): FloatArray = FloatArray(count * 2) { i ->
            (0.3 * sin(2 * Math.PI * 440.0 * (firstFrame + i / 2) / rate)).toFloat()
        }
    }
    FileOutputStream(file).use { out ->
        val report = StreamingWarpProcessor.renderWav(source, listOf(
            com.danim.autowarp.analysis.WarpMarker(0.0, 0.0),
            com.danim.autowarp.analysis.WarpMarker(frames.toDouble()/rate, frames.toDouble()/rate)
        ), out, blockFrames = 4096)
        check(report.outputFrames == frames.toLong())
    }
    val audit = StreamingRenderAudit.inspect(file, frames.toLong())
    check(audit.frames == frames.toLong() && audit.headerConsistent)
    check(audit.sampleRate == rate && audit.channels == 2)
    check(audit.durationDifferenceMs == 0.0 && audit.peak in 0.29..0.31)
    check(audit.clippedSamples == 0L && audit.discontinuities == 0L)
    check(!audit.hasWarnings)
    val truncated = File(args[1]); truncated.writeBytes(file.readBytes().copyOf(128))
    try { StreamingRenderAudit.inspect(truncated); error("Truncamiento no detectado") }
    catch (e: IllegalArgumentException) { check(e.message!!.contains("truncado")) }
    println("PASS: render WAV, cabecera, duración, niveles, continuidad, archivo truncado")
}
KT
cat > "$TMP/Marker.kt" <<'KT'
package com.danim.autowarp.analysis
data class WarpMarker(val sourceSeconds: Double, val targetSeconds: Double, val strength: Float = 1f)
KT
kotlinc "$TMP/Marker.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingRenderAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar" "$TMP/test.wav" "$TMP/truncated.wav"
