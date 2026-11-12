package com.danim.autowarp.analysis

import kotlin.math.abs

/** Rejects beat tracks whose bar-scale tempo abruptly changes, e.g. half/double-time errors. */
object TempoContinuityGate {
    data class Assessment(val reliable: Boolean, val checkedBars: Int, val maxRelativeJump: Double)

    fun assess(beatTimes: List<Double>, maxAdjacentBarJump: Double = 0.10): Assessment {
        require(maxAdjacentBarJump > 0.0 && maxAdjacentBarJump < 1.0)
        if (beatTimes.size < 13 || beatTimes.any { !it.isFinite() } ||
            beatTimes.zipWithNext().any { (a, b) -> b <= a }) return Assessment(false, 0, Double.POSITIVE_INFINITY)
        val periods = (0 until beatTimes.size - 4 step 4).map { i ->
            (beatTimes[i + 4] - beatTimes[i]) / 4.0
        }
        if (periods.size < 3 || periods.any { it <= 0.0 || !it.isFinite() })
            return Assessment(false, periods.size, Double.POSITIVE_INFINITY)
        val jumps = periods.zipWithNext().map { (a, b) -> abs(b - a) / a }
        val maxJump = jumps.maxOrNull() ?: 0.0
        return Assessment(maxJump <= maxAdjacentBarJump, periods.size, maxJump)
    }
}
