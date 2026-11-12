#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
INPUT="${1:-/mnt/data/Javi Golo & Andres Gomes - Expanden.flac}"
command -v ffmpeg >/dev/null
command -v kotlinc >/dev/null
[[ -f "$INPUT" ]] || { echo "SKIP: falta el FLAC real: $INPUT"; exit 2; }
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
# Fragmento real, sin distribuir la grabación en el paquete.
ffmpeg -nostdin -hide_banner -loglevel error -y -ss 75 -t 8 -i "$INPUT" -ac 2 -ar 44100 -c:a pcm_s16le "$TMP/input.wav"
cat > "$TMP/Marker.kt" <<'KT'
package com.danim.autowarp.analysis
data class WarpMarker(val sourceSeconds: Double, val targetSeconds: Double, val strength: Float = 1f)
KT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.WarpMarker
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
fun main(args:Array<String>) {
    val input=File(args[0]); val original=StreamingRenderAudit.inspect(input)
    Pcm16WavFileSource(input).use { source ->
        val duration=source.frameCount.toDouble()/source.sampleRate
        check(duration in 7.9..8.1)
        fun render(name:String, markers:List<WarpMarker>):StreamingRenderAuditReport {
            val out=File(args[1],name)
            val report=FileOutputStream(out).use { StreamingWarpProcessor.renderWav(source,markers,it,blockFrames=4096) }
            val audit=StreamingRenderAudit.inspect(out,source.frameCount)
            check(report.outputFrames==source.frameCount)
            check(report.maxSourceFramesBuffered<12000)
            check(audit.headerConsistent && audit.frames==source.frameCount && audit.channels==2)
            println("$name clipped=${audit.clippedSamples} peak=${audit.peak} jumps=${audit.discontinuities}")
            check(audit.durationDifferenceMs==0.0)
            return audit
        }
        val identity=render("identity.wav",listOf(WarpMarker(0.0,0.0),WarpMarker(duration,duration)))
        val warped=render("warped.wav",listOf(WarpMarker(0.0,0.0),WarpMarker(duration*0.5,duration*0.5+0.025),WarpMarker(duration,duration)))
        val baselineComparison=RenderComparisonAudit.compare(original,identity)
        val warpComparison=RenderComparisonAudit.compare(original,warped)
        check(baselineComparison.compatible && warpComparison.compatible)
        check(baselineComparison.durationDeltaMs < 0.01 && warpComparison.durationDeltaMs < 0.01)
        println("Comparison identity: addedClips=${baselineComparison.addedClippedSamples}, addedJumps=${baselineComparison.addedDiscontinuities}, warnings=${baselineComparison.warnings}")
        println("Comparison warped: addedClips=${warpComparison.addedClippedSamples}, addedJumps=${warpComparison.addedDiscontinuities}, warnings=${warpComparison.warnings}")
        check(abs(identity.rms-original.rms)<0.005) { "RMS de identidad alterado" }
        println("PASS real FLAC: duration=${"%.3f".format(duration)}s originalRms=${"%.4f".format(original.rms)} identityRms=${"%.4f".format(identity.rms)} warpedRms=${"%.4f".format(warped.rms)}")
        println("Avisos heurísticos: original=${original.hasWarnings} identity=${identity.hasWarnings} warped=${warped.hasWarnings}; discontinuities original=${original.discontinuities}, warped=${warped.discontinuities}")
        println("NOTA: interpolación cúbica no preserva tono; no se afirma corrección de beatgrid ni ausencia de artefactos.")
    }
}
KT
kotlinc "$TMP/Marker.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingRenderAudit.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/RenderComparisonAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar" "$TMP/input.wav" "$TMP"
