package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.round

/** Diagnostic grid audit. A fitted grid is NOT evidence that events are actual kicks. */
object KickGridContinuityAudit {
    data class Report(
        val phaseSeconds: Double,
        val matchedCandidates: Int,
        val distinctMatchedBeats: Int,
        val unmatchedCandidates: Int,
        val missingInteriorBeats: Int,
        val longestConsecutiveRun: Int,
        val coverage: Double,
        val reliable: Boolean
    )

    fun inspect(times: List<Double>, bpm: Double, toleranceFraction: Double = 0.16): Report {
        require(bpm.isFinite() && bpm in 60.0..240.0)
        require(toleranceFraction.isFinite() && toleranceFraction > 0.0 && toleranceFraction < 0.25)
        require(times.all { it.isFinite() && it >= 0.0 })
        if (times.isEmpty()) return Report(Double.NaN, 0, 0, 0, 0, 0, 0.0, false)
        val period = 60.0 / bpm
        val tolerance = period * toleranceFraction
        val sorted = times.sorted()
        // Use a candidate phase, maximizing distinct occupied beat slots rather than raw hit count.
        val phase = sorted.map { ((it % period) + period) % period }.maxWithOrNull(
            compareBy<Double> { p ->
                sorted.filter { phaseError(it, p, period) <= tolerance }
                    .map { round((it - p) / period).toLong() }.distinct().size
            }.thenBy { p ->
                -sorted.filter { phaseError(it, p, period) <= tolerance }
                    .sumOf { phaseError(it, p, period) }
            }.thenBy { -it }
        )!!
        val matched = sorted.filter { phaseError(it, phase, period) <= tolerance }
        val slots = matched.map { round((it - phase) / period).toLong() }.distinct().sorted()
        val missing = if (slots.size < 2) 0 else (slots.last() - slots.first() + 1 - slots.size).toInt()
        var run = 0
        var longest = 0
        var previous: Long? = null
        for (slot in slots) {
            run = if (previous != null && slot == previous + 1) run + 1 else 1
            if (run > longest) longest = run
            previous = slot
        }
        val coverage = if (slots.isEmpty()) 0.0 else slots.size.toDouble() / (slots.size + missing)
        val reliable = slots.size >= 4 && longest >= 3 && coverage >= 0.60 && matched.size.toDouble() / sorted.size >= 0.60
        return Report(phase, matched.size, slots.size, sorted.size - matched.size, missing, longest, coverage, reliable)
    }

    private fun phaseError(t: Double, phase: Double, period: Double): Double {
        val delta = t - phase
        return abs(delta - round(delta / period) * period)
    }
}
