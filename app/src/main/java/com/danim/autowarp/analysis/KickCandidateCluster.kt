package com.danim.autowarp.analysis

/** Diagnostic candidate grouping: keeps the strongest evidence within each nearby-onset cluster.
 * It does not alter BeatAnalyzer or authorize automatic warp.
 */
object KickCandidateCluster {
    data class Candidate(val seconds: Double, val evidence: Double)
    data class Report(val retained: List<Candidate>, val suppressed: Int, val clusterCount: Int)

    fun group(candidates: List<Candidate>, bpm: Double, proximityFraction: Double = 0.55): Report {
        require(bpm.isFinite() && bpm > 0.0)
        require(proximityFraction.isFinite() && proximityFraction > 0.0 && proximityFraction < 1.0)
        require(candidates.all { it.seconds.isFinite() && it.seconds >= 0.0 && it.evidence.isFinite() && it.evidence in 0.0..1.0 })
        if (candidates.isEmpty()) return Report(emptyList(), 0, 0)
        val threshold = 60.0 / bpm * proximityFraction
        val sorted = candidates.sortedWith(compareBy<Candidate> { it.seconds }.thenByDescending { it.evidence })
        val clusters = mutableListOf<MutableList<Candidate>>()
        for (candidate in sorted) {
            // Anchor to the first onset, preventing a chain of nearby percussion
            // attacks from merging separate beat pulses into one long cluster.
            if (clusters.isEmpty() || candidate.seconds - clusters.last().first().seconds >= threshold)
                clusters.add(mutableListOf(candidate))
            else clusters.last().add(candidate)
        }
        val retained = clusters.map { cluster ->
            cluster.minWith(compareByDescending<Candidate> { it.evidence }.thenBy { it.seconds })
        }.sortedBy { it.seconds }
        return Report(retained, sorted.size - retained.size, clusters.size)
    }
}
