package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.round

/** Diagnostic only: compares each candidate to a phase-consistent pulse.
 * Does not grant permission to warp or classify a transient as a confirmed kick.
 */
object KickRhythmEvidence {
    data class Evidence(val pulsePeriodSeconds: Double, val phaseSeconds: Double,
                        val supportRatio: Double, val candidateScores: List<Double>)

    fun assess(times: List<Double>, bpm: Double, toleranceSeconds: Double = 0.055): Evidence {
        val period = if (bpm.isFinite() && bpm in 60.0..240.0) 60.0 / bpm else Double.NaN
        if (!period.isFinite() || times.isEmpty() || times.any { !it.isFinite() })
            return Evidence(Double.NaN, Double.NaN, 0.0, List(times.size) { 0.0 })
        val tolerance = toleranceSeconds.coerceIn(0.005, period * 0.20)
        // Deterministic robust phase selection; do not let high candidate density
        // artificially create confidence by choosing a phase supported by one hit.
        val phases = times.map { ((it % period) + period) % period }
        val phase = phases.maxWithOrNull(compareBy<Double> { candidate ->
            times.count { t -> phaseError(t, candidate, period) <= tolerance }
        }.thenBy { -it }) ?: 0.0
        val scores = times.map { t ->
            (1.0 - phaseError(t, phase, period) / tolerance).coerceIn(0.0, 1.0)
        }
        return Evidence(period, phase, scores.count { it > 0.0 }.toDouble() / times.size, scores)
    }

    /** Counts distinct neighboring pulse slots, not repeated transients in one slot.
     * A lone event cannot provide evidence of a beat sequence.
     * Diagnostic only; never authorizes an automatic warp.
     */
    fun consecutiveSupport(times: List<Double>, bpm: Double,
                           toleranceSeconds: Double = 0.055,
                           minRun: Int = 3): List<Double> {
        val base = assess(times, bpm, toleranceSeconds)
        if (!base.pulsePeriodSeconds.isFinite()) return List(times.size) { 0.0 }
        val period = base.pulsePeriodSeconds
        val slots = times.indices.filter { base.candidateScores[it] > 0.0 }
            .groupBy { round((times[it] - base.phaseSeconds) / period).toLong() }
        val occupied = slots.keys
        val required = minRun.coerceAtLeast(2)
        return times.indices.map { i ->
            if (base.candidateScores[i] <= 0.0) 0.0 else {
                val slot = round((times[i] - base.phaseSeconds) / period).toLong()
                var left = slot
                var right = slot
                while (left > Long.MIN_VALUE && occupied.contains(left - 1)) left--
                while (right < Long.MAX_VALUE && occupied.contains(right + 1)) right++
                val run = (right - left + 1).coerceAtMost(required.toLong()).toDouble()
                base.candidateScores[i] * ((run - 1.0) / (required - 1.0)).coerceIn(0.0, 1.0)
            }
        }
    }

    private fun phaseError(t: Double, phase: Double, period: Double): Double {
        val delta = t - phase
        return abs(delta - round(delta / period) * period)
    }
}
