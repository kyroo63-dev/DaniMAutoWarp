package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.round

/** Diagnostic-only joint spectral/grid evidence. Does not validate musical kick identity. */
object KickSpectralGridAudit {
    data class Report(
        val gridMatched: Int,
        val spectralSupported: Int,
        val jointlySupported: Int,
        val distinctJointBeats: Int,
        val longestJointRun: Int,
        val jointCoverage: Double,
        val reliable: Boolean
    )

    fun inspect(
        candidates: List<KickCandidateCluster.Candidate>,
        bpm: Double,
        phaseSeconds: Double,
        minimumSpectralScore: Double = 0.30,
        toleranceFraction: Double = 0.16
    ): Report {
        require(bpm.isFinite() && bpm in 60.0..240.0)
        require(minimumSpectralScore.isFinite() && minimumSpectralScore in 0.0..1.0)
        require(toleranceFraction.isFinite() && toleranceFraction > 0.0 && toleranceFraction < 0.25)
        require(candidates.all { it.seconds.isFinite() && it.seconds >= 0.0 && it.evidence.isFinite() && it.evidence in 0.0..1.0 })
        if (candidates.isEmpty()) return Report(0, 0, 0, 0, 0, 0.0, false)
        require(phaseSeconds.isFinite())
        val period = 60.0 / bpm
        fun matched(t: Double): Boolean {
            val delta = (t - phaseSeconds) / period
            return abs(delta - round(delta)) <= toleranceFraction + 1e-12
        }
        val gridMatched = candidates.count { matched(it.seconds) }
        val spectralSupported = candidates.count { it.evidence >= minimumSpectralScore }
        val joint = candidates.filter { matched(it.seconds) && it.evidence >= minimumSpectralScore }
        val slots = joint.map { round((it.seconds - phaseSeconds) / period).toLong() }.distinct().sorted()
        var run = 0
        var longest = 0
        var previous: Long? = null
        for (slot in slots) {
            run = if (previous != null && slot == previous + 1) run + 1 else 1
            longest = maxOf(longest, run)
            previous = slot
        }
        val span = if (slots.isEmpty()) 0L else slots.last() - slots.first() + 1
        val coverage = if (span == 0L) 0.0 else slots.size.toDouble() / span
        // Require both a long sequence and spectral support; still only a heuristic.
        val reliable = slots.size >= 6 && longest >= 5 && coverage >= 0.70 &&
            joint.size.toDouble() / candidates.size >= 0.60
        return Report(gridMatched, spectralSupported, joint.size, slots.size, longest, coverage, reliable)
    }
}
