package com.danim.autowarp.analysis

/** Conservative confidence adjustment for a kick candidate overlapping a snare/hat attack.
 * Never treats this heuristic as an instrument label or a reason to invent beat markers.
 */
object KickOverlapEvidence {
    data class Result(val confidence: Double, val competingAttack: Boolean)

    fun assess(baseConfidence: Double, lowFlux: Double, midFlux: Double,
               highFlux: Double, bassShare: Double): Result {
        require(listOf(baseConfidence, lowFlux, midFlux, highFlux, bassShare).all { it.isFinite() })
        val totalFlux = (lowFlux.coerceAtLeast(0.0) + midFlux.coerceAtLeast(0.0) +
            highFlux.coerceAtLeast(0.0)).coerceAtLeast(1e-12)
        val competingShare = (midFlux.coerceAtLeast(0.0) + highFlux.coerceAtLeast(0.0)) / totalFlux
        val competing = competingShare >= 0.70 && bassShare < 0.58
        // Limit the penalty: a real kick may coincide with a snare or cymbal.
        val penalty = if (competing) 0.78 else 1.0
        return Result((baseConfidence * penalty).coerceIn(0.0, 1.0), competing)
    }
}
