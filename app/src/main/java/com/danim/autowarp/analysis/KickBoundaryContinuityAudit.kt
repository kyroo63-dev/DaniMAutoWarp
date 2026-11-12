package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.round

/** Diagnostic only. Never authorizes automatic warp or extrapolates through unsupported sections. */
object KickBoundaryContinuityAudit {
    data class Segment(val startSeconds: Double, val endSeconds: Double, val bpm: Double,
                       val localPhaseSeconds: Double, val reliable: Boolean)
    data class Report(val contiguous: Boolean, val eligible: Boolean,
                      val relativeTempoJump: Double, val phaseJumpSeconds: Double,
                      val continuous: Boolean)

    fun inspect(left: Segment, right: Segment, maxTempoJump: Double = 0.025,
                maxPhaseFraction: Double = 0.16): Report {
        require(maxTempoJump.isFinite() && maxTempoJump in 0.0..0.5)
        require(maxPhaseFraction.isFinite() && maxPhaseFraction > 0.0 && maxPhaseFraction < 0.5)
        fun valid(s: Segment) = s.startSeconds.isFinite() && s.endSeconds.isFinite() &&
            s.startSeconds >= 0.0 && s.endSeconds > s.startSeconds &&
            s.bpm.isFinite() && s.bpm in 60.0..240.0 && s.localPhaseSeconds.isFinite() &&
            s.localPhaseSeconds >= 0.0
        require(valid(left) && valid(right))
        val contiguous = abs(left.endSeconds - right.startSeconds) <= 0.002
        val eligible = contiguous && left.reliable && right.reliable
        val relativeJump = abs(left.bpm - right.bpm) / ((left.bpm + right.bpm) / 2.0)
        val period = 60.0 / ((left.bpm + right.bpm) / 2.0)
        // Phase is local to each segment. Convert both to absolute seconds before comparing.
        val leftAbsolute = left.startSeconds + left.localPhaseSeconds
        val rightAbsolute = right.startSeconds + right.localPhaseSeconds
        val delta = leftAbsolute - rightAbsolute
        val phaseJump = abs(delta - round(delta / period) * period)
        return Report(contiguous, eligible, relativeJump, phaseJump,
            eligible && relativeJump <= maxTempoJump && phaseJump <= period * maxPhaseFraction)
    }
}
