package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

/** Independent periodicity check on detected kick attacks; never fabricates beats. */
object KickPulseAudit {
    data class Result(val phaseSeconds: Double, val supportRatio: Double,
                      val medianErrorMs: Double, val reliable: Boolean)

    fun assess(kicks: List<Double>, periodSeconds: Double): Result {
        if (kicks.size < 12 || !periodSeconds.isFinite() || periodSeconds !in 0.30..0.80 ||
            kicks.any { !it.isFinite() || it < 0.0 })
            return Result(0.0, 0.0, Double.POSITIVE_INFINITY, false)
        val sorted = kicks.distinct().sorted()
        if (sorted.size < 12) return Result(0.0, 0.0, Double.POSITIVE_INFINITY, false)
        val tolerance = min(0.045, periodSeconds * 0.10)
        // Each attack proposes a phase. Choose the phase that maximizes supporting
        // attacks, not the most common subdivision of a synthesized beatgrid.
        val phases = sorted.map { it % periodSeconds }
        var bestPhase = 0.0
        var bestCount = -1
        var bestError = Double.POSITIVE_INFINITY
        for (phase in phases) {
            var count = 0
            var error = 0.0
            for (t in sorted) {
                val cycles = round((t - phase) / periodSeconds)
                val delta = abs(t - phase - cycles * periodSeconds)
                if (delta <= tolerance) { count++; error += delta }
            }
            if (count > bestCount || (count == bestCount && error < bestError)) {
                bestPhase = phase; bestCount = count; bestError = error
            }
        }
        val errors = sorted.map { abs(it - bestPhase - round((it - bestPhase) / periodSeconds) * periodSeconds) }
            .filter { it <= tolerance }.sorted()
        val median = if (errors.isEmpty()) Double.POSITIVE_INFINITY else errors[errors.size / 2] * 1000.0
        val ratio = bestCount.toDouble() / sorted.size
        // Many techno mixes have hats or layered kicks: the fraction is diagnostic,
        // not a standalone authorization to warp.
        return Result(bestPhase, ratio, median, ratio >= 0.30 && median <= 30.0)
    }
}
