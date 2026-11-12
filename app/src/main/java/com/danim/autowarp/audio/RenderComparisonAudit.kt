package com.danim.autowarp.audio

import kotlin.math.abs

/** Compara métricas de exportación con la fuente; no pretende medir calidad perceptual. */
data class RenderComparison(
    val compatible: Boolean,
    val addedClippedSamples: Long,
    val addedDiscontinuities: Long,
    val rmsChangeDb: Double?,
    val durationDeltaMs: Double,
    val warnings: List<String>
)

object RenderComparisonAudit {
    fun compare(source: StreamingRenderAuditReport, rendered: StreamingRenderAuditReport): RenderComparison {
        val compatible = source.sampleRate == rendered.sampleRate && source.channels == rendered.channels &&
            source.headerConsistent && rendered.headerConsistent
        val addedClips = (rendered.clippedSamples - source.clippedSamples).coerceAtLeast(0L)
        val addedJumps = (rendered.discontinuities - source.discontinuities).coerceAtLeast(0L)
        val durationDelta = if (source.sampleRate > 0 && rendered.sampleRate > 0)
            abs(rendered.frames.toDouble() / rendered.sampleRate - source.frames.toDouble() / source.sampleRate) * 1000.0
            else Double.POSITIVE_INFINITY
        val db = if (source.rms > 0.0 && rendered.rms > 0.0)
            20.0 * kotlin.math.log10(rendered.rms / source.rms) else null
        val warnings = buildList {
            if (!compatible) add("Formato o cabecera incompatible")
            if (durationDelta > 2.0) add("Duración alterada más de 2 ms")
            if (addedClips > 0) add("Aumentan las muestras saturadas: +$addedClips")
            if (addedJumps > 0) add("Aumentan los saltos heurísticos: +$addedJumps")
            if (db != null && abs(db) > 1.0) add("Variación RMS mayor de 1 dB")
        }
        return RenderComparison(compatible, addedClips, addedJumps, db, durationDelta, warnings)
    }
}
