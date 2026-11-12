package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.round

/** Diagnostic-only holdout test: fit the grid on early events, test later events.
 * This avoids scoring the same observations used to fit the phase.
 * It cannot establish whether aligned transients are actual kick drums.
 */
object KickHoldoutPhaseAudit {
    data class Report(
        val trainingCount: Int, val validationCount: Int,
        val validationMatched: Int, val validationRatio: Double,
        val phaseErrorSeconds: Double, val sufficientEvidence: Boolean
    )

    fun inspect(candidates: List<KickCandidateCluster.Candidate>, bpm: Double,
                toleranceFraction: Double = 0.16): Report {
        require(bpm.isFinite() && bpm in 60.0..240.0)
        require(toleranceFraction.isFinite() && toleranceFraction > 0.0 && toleranceFraction < 0.25)
        require(candidates.all { it.seconds.isFinite() && it.seconds >= 0.0 &&
            it.evidence.isFinite() && it.evidence in 0.0..1.0 })
        val sorted = candidates.sortedBy { it.seconds }
        val midpoint = sorted.size / 2
        val train = sorted.take(midpoint)
        val test = sorted.drop(midpoint)
        if (train.size < 3 || test.size < 3)
            return Report(train.size, test.size, 0, 0.0, Double.NaN, false)
        val period = 60.0 / bpm
        val tolerance = period * toleranceFraction
        fun error(t: Double, phase: Double): Double =
            abs(t - phase - round((t - phase) / period) * period)
        val phases = train.map { ((it.seconds % period) + period) % period }
        // Fit ONLY training events, weighted by independent spectral evidence.
        val phase = phases.maxWithOrNull(compareBy<Double> { p ->
            train.sumOf { if (error(it.seconds, p) <= tolerance) it.evidence else 0.0 }
        }.thenBy { p -> -train.sumOf { error(it.seconds, p) * it.evidence } })!!
        val matched = test.count { error(it.seconds, phase) <= tolerance }
        val ratio = matched.toDouble() / test.size
        // Circular phase discrepancy between separately fitted halves.
        val latePhase = test.map { ((it.seconds % period) + period) % period }
            .maxWithOrNull(compareBy<Double> { p ->
                test.sumOf { if (error(it.seconds, p) <= tolerance) it.evidence else 0.0 }
            }.thenBy { p -> -test.sumOf { error(it.seconds, p) * it.evidence } })!!
        val phaseError = error(latePhase, phase)
        return Report(train.size, test.size, matched, ratio, phaseError,
            matched >= 3 && ratio >= 0.70 && phaseError <= tolerance)
    }
}
