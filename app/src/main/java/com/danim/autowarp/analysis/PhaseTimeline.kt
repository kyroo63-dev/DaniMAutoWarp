package com.danim.autowarp.analysis

/** Display-only projection of phase windows; never authorizes audio processing. */
object PhaseTimeline {
    data class Span(val left: Float, val right: Float, val reliable: Boolean)
    fun project(windows: List<KickPhaseDrift.Window>, start: Double, end: Double): List<Span> {
        if (!start.isFinite() || !end.isFinite() || end <= start) return emptyList()
        return windows.asSequence().filter { it.startSeconds.isFinite() && it.endSeconds.isFinite() && it.endSeconds > it.startSeconds }
            .filter { it.endSeconds > start && it.startSeconds < end }
            .map { w -> Span(((w.startSeconds.coerceAtLeast(start) - start) / (end - start)).toFloat().coerceIn(0f, 1f),
                ((w.endSeconds.coerceAtMost(end) - start) / (end - start)).toFloat().coerceIn(0f, 1f), w.reliable) }
            .filter { it.right > it.left }.toList()
    }
}
