package com.danim.autowarp.analysis

import kotlin.math.abs

/** Diagnostic only. Measures tempo agreement between two chronological halves.
 * Uses only adjacent intervals near the reference pulse: sparse / half-time
 * patterns remain inconclusive instead of being silently promoted.
 */
object KickTempoHoldoutAudit {
    data class Report(
        val earlyPairs: Int, val latePairs: Int,
        val earlyBpm: Double, val lateBpm: Double,
        val relativeDrift: Double, val sufficientEvidence: Boolean,
        val consistent: Boolean
    )

    fun inspect(times: List<Double>, referenceBpm: Double,
                intervalTolerance: Double = 0.20,
                maxRelativeDrift: Double = 0.025): Report {
        require(referenceBpm.isFinite() && referenceBpm in 60.0..240.0)
        require(intervalTolerance.isFinite() && intervalTolerance > 0.0 && intervalTolerance < 0.40)
        require(maxRelativeDrift.isFinite() && maxRelativeDrift > 0.0 && maxRelativeDrift < 0.20)
        require(times.all { it.isFinite() && it >= 0.0 })
        val sorted = times.distinct().sorted()
        val split = sorted.size / 2
        val period = 60.0 / referenceBpm
        fun intervals(part: List<Double>) = part.zipWithNext().map { it.second - it.first }
            .filter { it in period * (1 - intervalTolerance)..period * (1 + intervalTolerance) }
            .sorted()
        fun median(values: List<Double>): Double = if (values.isEmpty()) Double.NaN
            else if (values.size % 2 == 1) values[values.size / 2]
            else (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
        val early = intervals(sorted.take(split))
        val late = intervals(sorted.drop(split))
        val bpmEarly = 60.0 / median(early)
        val bpmLate = 60.0 / median(late)
        val enough = early.size >= 3 && late.size >= 3
        val drift = if (enough) abs(bpmLate - bpmEarly) / referenceBpm else Double.NaN
        return Report(early.size, late.size, bpmEarly, bpmLate, drift, enough,
            enough && drift <= maxRelativeDrift)
    }
}
