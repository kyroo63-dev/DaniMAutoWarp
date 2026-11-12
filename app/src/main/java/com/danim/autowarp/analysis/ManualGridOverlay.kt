package com.danim.autowarp.analysis

/** Positions for read-only source/target overlay in a zoomed waveform viewport. */
object ManualGridOverlay {
    data class Positions(val sourceFraction: Double?, val targetFraction: Double?)

    /** Pixel geometry for sub-pixel alignment previews; safe when one line is clipped. */
    data class Detail(val sourceX: Float?, val targetX: Float?, val separationPx: Float?, val needsMagnification: Boolean)

    fun detail(positions: Positions?, widthPx: Float, thresholdPx: Float = 12f): Detail? {
        if (positions == null || !widthPx.isFinite() || widthPx <= 0f ||
            !thresholdPx.isFinite() || thresholdPx < 0f) return null
        val source = positions.sourceFraction?.takeIf { it.isFinite() && it in 0.0..1.0 }?.let { (it * widthPx).toFloat() }
        val target = positions.targetFraction?.takeIf { it.isFinite() && it in 0.0..1.0 }?.let { (it * widthPx).toFloat() }
        if (source == null && target == null) return null
        val separation = if (source != null && target != null) kotlin.math.abs(source - target) else null
        return Detail(source, target, separation, separation != null && separation < thresholdPx)
    }

    fun project(preview: ManualGridPreview.Preview, startSeconds: Double, endSeconds: Double): Positions? {
        if (!preview.accepted || !startSeconds.isFinite() || !endSeconds.isFinite() || endSeconds <= startSeconds) return null
        val target = preview.proposedTargetSeconds ?: return null
        val source = preview.sourceSeconds
        if (!source.isFinite() || !target.isFinite()) return null
        fun fraction(time: Double): Double? = if (time >= startSeconds && time <= endSeconds)
            (time - startSeconds) / (endSeconds - startSeconds) else null
        return Positions(fraction(source), fraction(target))
    }
}
