package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.roundToInt

/** Conservative diagnostic gate. Scores are features, NOT calibrated probabilities.
 * A candidate must show spectral evidence and independent periodic neighbours.
 * This module never authorizes a production warp.
 */
object KickCandidateEvidenceGate {
    data class Candidate(val seconds: Double, val bassShare: Double, val spectralScore: Double)
    data class Decision(val candidate: Candidate, val supportedNeighbours: Int, val accepted: Boolean)
    data class Report(val decisions: List<Decision>, val accepted: Int, val rejected: Int)

    fun inspect(candidates: List<Candidate>, bpm: Double,
                minBassShare: Double = 0.65, minSpectralScore: Double = 0.60,
                beatTolerance: Double = 0.16): Report {
        require(bpm.isFinite() && bpm in 40.0..240.0)
        require(minBassShare in 0.0..1.0 && minSpectralScore in 0.0..1.0)
        require(beatTolerance.isFinite() && beatTolerance > 0.0 && beatTolerance < 0.5)
        require(candidates.all { it.seconds.isFinite() && it.seconds >= 0.0 &&
            it.bassShare.isFinite() && it.bassShare in 0.0..1.0 &&
            it.spectralScore.isFinite() && it.spectralScore in 0.0..1.0 })
        val sorted = candidates.sortedBy { it.seconds }
        val period = 60.0 / bpm
        val decisions = sorted.mapIndexed { index, c ->
            val neighbours = sorted.indices.count { j ->
                if (j == index) false else {
                    val distance = abs(sorted[j].seconds - c.seconds)
                    val beats = (distance / period).roundToInt()
                    beats in 1..2 && abs(distance / period - beats) <= beatTolerance
                }
            }
            Decision(c, neighbours, c.bassShare >= minBassShare &&
                c.spectralScore >= minSpectralScore && neighbours >= 2)
        }
        return Report(decisions, decisions.count { it.accepted }, decisions.count { !it.accepted })
    }
}
