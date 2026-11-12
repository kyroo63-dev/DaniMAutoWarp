package com.danim.autowarp.analysis

import kotlin.math.abs

/** Joins only adjacent, independently reliable phase windows. Never bridges an unreliable gap. */
object KickPhaseSegments {
    data class Segment(val startSeconds: Double, val endSeconds: Double,
                       val windowCount: Int, val maximumPhaseJumpMs: Double)
    data class Report(val segments: List<Segment>, val rejectedWindows: Int,
                      val discontinuities: Int, val eligibleForAutomaticWarp: Boolean)

    fun inspect(windows: List<KickPhaseDrift.Window>, bpm: Double,
                maxJumpMs: Double = 35.0): Report {
        require(bpm.isFinite() && bpm > 0.0)
        require(maxJumpMs.isFinite() && maxJumpMs > 0.0)
        val ordered = windows.sortedBy { it.startSeconds }
        val segments = mutableListOf<Segment>()
        var start: Double? = null
        var end = 0.0
        var count = 0
        var peak = 0.0
        var previous: KickPhaseDrift.Window? = null
        var rejected = 0
        var jumps = 0
        fun flush() {
            if (start != null && count > 0) segments += Segment(start!!, end, count, peak)
            start = null; count = 0; peak = 0.0; previous = null
        }
        for (w in ordered) {
            val valid = w.reliable && w.phaseOffsetMs?.isFinite() == true &&
                w.startSeconds.isFinite() && w.endSeconds.isFinite() &&
                w.endSeconds > w.startSeconds
            if (!valid) { rejected++; flush(); continue }
            val prev = previous
            val contiguous = prev != null && abs(prev.endSeconds - w.startSeconds) < 0.001
            val delta = if (contiguous) KickPhaseDrift.phaseDifferenceMs(prev!!, w, bpm) else null
            if (prev != null && (delta == null || abs(delta) > maxJumpMs)) {
                if (delta != null) jumps++
                flush()
            }
            if (start == null) start = w.startSeconds
            end = w.endSeconds
            count++
            if (delta != null && abs(delta) <= maxJumpMs) peak = maxOf(peak, abs(delta))
            previous = w
        }
        flush()
        // A phase-consistent region is diagnostic evidence, not permission to stretch audio.
        return Report(segments, rejected, jumps, false)
    }
}
