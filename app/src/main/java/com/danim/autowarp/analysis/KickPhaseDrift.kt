package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.round

/** Diagnostic only: measures phase support in independent windows; never authorizes warp. */
object KickPhaseDrift {
    data class Window(val startSeconds: Double, val endSeconds: Double,
                      val kickCount: Int, val supportRatio: Double,
                      val medianErrorMs: Double, val phaseOffsetMs: Double?,
                      val reliable: Boolean)

    fun inspect(kicks: List<Double>, bpm: Double, durationSeconds: Double,
                windowSeconds: Double = 12.0): List<Window> {
        require(bpm.isFinite() && bpm in 75.0..200.0)
        require(durationSeconds.isFinite() && durationSeconds > 0.0)
        require(windowSeconds.isFinite() && windowSeconds in 4.0..120.0)
        val period = 60.0 / bpm
        val valid = kicks.filter { it.isFinite() && it >= 0.0 && it <= durationSeconds }.distinct().sorted()
        val windows = mutableListOf<Window>()
        var start = 0.0
        while (start < durationSeconds) {
            val end = (start + windowSeconds).coerceAtMost(durationSeconds)
            val subset = valid.filter { it >= start && (it < end || end == durationSeconds && it <= end) }
            val audit = KickPulseAudit.assess(subset, period)
            // Circular phase: signed offset relative to the first observed reliable window
            // is calculated by callers; here the absolute local phase is exposed.
            windows += Window(start, end, subset.size, audit.supportRatio,
                audit.medianErrorMs, if (audit.reliable) audit.phaseSeconds * 1000.0 else null,
                audit.reliable)
            start = end
        }
        return windows
    }

    fun phaseDifferenceMs(a: Window, b: Window, bpm: Double): Double? {
        val x = a.phaseOffsetMs ?: return null
        val y = b.phaseOffsetMs ?: return null
        val periodMs = 60000.0 / bpm
        return (y - x) - round((y - x) / periodMs) * periodMs
    }
}
