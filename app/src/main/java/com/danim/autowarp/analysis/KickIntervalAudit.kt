package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.roundToInt

/** Diagnostic only. No output from this class authorizes time stretching.
 * Describes whether consecutive detected kicks resemble an independently supplied beat period.
 */
object KickIntervalAudit {
    data class Report(val candidateCount: Int, val closePairs: Int, val pulsePairs: Int,
                      val missedPulsePairs: Int, val irregularPairs: Int, val longestPulseRun: Int,
                      val medianIntervalSeconds: Double?)

    fun inspect(times: List<Double>, referenceBpm: Double, tolerance: Double = 0.12): Report {
        require(referenceBpm.isFinite() && referenceBpm > 0.0)
        require(tolerance.isFinite() && tolerance in 0.0..0.5)
        require(times.all { it.isFinite() && it >= 0.0 })
        val sorted = times.distinct().sorted()
        if (sorted.size < 2) return Report(sorted.size, 0, 0, 0, 0, if (sorted.isEmpty()) 0 else 1, null)
        val period = 60.0 / referenceBpm
        var close = 0; var pulse = 0; var missed = 0; var irregular = 0
        var run = 1; var longest = 1
        val intervals = sorted.zipWithNext { a, b -> b - a }
        for (delta in intervals) {
            val multiple = (delta / period).roundToInt()
            when {
                delta < period * 0.55 -> { close++; run = 1 }
                multiple in 1..4 && abs(delta - multiple * period) <= tolerance * period -> {
                    if (multiple == 1) { pulse++; run++; longest = maxOf(longest, run) }
                    else { missed++; run = 1 }
                }
                else -> { irregular++; run = 1 }
            }
        }
        val ordered = intervals.sorted()
        val median = if (ordered.size % 2 == 1) ordered[ordered.size / 2]
                     else (ordered[ordered.size / 2 - 1] + ordered[ordered.size / 2]) / 2.0
        return Report(sorted.size, close, pulse, missed, irregular, longest, median)
    }
}
