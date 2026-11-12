package com.danim.autowarp.analysis

import kotlin.math.abs

/** Estimates which beat position is most likely the first beat of a 4/4 bar.
 * It deliberately reports low confidence when every kick is equally strong.
 */
data class BarPhaseEstimate(val downbeatBeatIndex: Int, val confidence: Double, val phaseScores: List<Double>)

object BarAlignmentAnalyzer {
    fun estimate(beatTimes: List<Double>, kickEvents: List<TransientEvent>, toleranceSeconds: Double = 0.075): BarPhaseEstimate {
        if (beatTimes.size < 8 || kickEvents.size < 8) return BarPhaseEstimate(0, 0.0, listOf(0.0, 0.0, 0.0, 0.0))
        val scores = (0..3).map { phase ->
            val strengths = beatTimes.indices.filter { it % 4 == phase }.mapNotNull { i ->
                val t = beatTimes[i]
                val nearest = kickEvents.minByOrNull { abs(it.seconds - t) } ?: return@mapNotNull null
                if (abs(nearest.seconds - t) <= toleranceSeconds) nearest.confidence.toDouble() else null
            }
            if (strengths.isEmpty()) 0.0 else strengths.average()
        }
        val ranked = scores.withIndex().sortedByDescending { it.value }
        val best = ranked.first()
        val second = ranked.getOrNull(1)?.value ?: 0.0
        val coverage = scores.count { it > 0.0 } / 4.0
        val confidence = if (best.value <= 1e-9) 0.0 else ((best.value - second).coerceAtLeast(0.0) / best.value * coverage).coerceIn(0.0, 1.0)
        return BarPhaseEstimate(best.index, confidence, scores)
    }
}
