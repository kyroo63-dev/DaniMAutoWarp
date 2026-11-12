package com.danim.autowarp.analysis

import kotlin.math.abs

/** Diagnostic only: estimates tempo in fixed chronological windows, without
 * interpolating over sparse sections or granting permission for automatic warp. */
object KickTempoWindowAudit {
    data class Window(val startSeconds: Double, val endSeconds: Double,
        val validPairs: Int, val bpm: Double, val sufficientEvidence: Boolean)
    data class Report(val windows: List<Window>, val supportedWindows: Int,
        val adjacentComparisons: Int, val largestAdjacentDrift: Double,
        val stable: Boolean, val sufficientEvidence: Boolean)

    fun inspect(times: List<Double>, referenceBpm: Double, durationSeconds: Double,
        windowSeconds: Double = 4.0, tolerance: Double = 0.20,
        minPairs: Int = 3, maxDrift: Double = 0.025): Report {
        require(referenceBpm.isFinite() && referenceBpm in 60.0..240.0)
        require(durationSeconds.isFinite() && durationSeconds > 0.0)
        require(windowSeconds.isFinite() && windowSeconds > 0.0)
        require(tolerance.isFinite() && tolerance > 0.0 && tolerance < 0.4)
        require(minPairs >= 2)
        require(maxDrift.isFinite() && maxDrift > 0.0 && maxDrift < 0.2)
        require(times.all { it.isFinite() && it >= 0.0 && it <= durationSeconds })
        val sorted = times.distinct().sorted()
        val period = 60.0 / referenceBpm
        val count = kotlin.math.ceil(durationSeconds / windowSeconds).toInt()
        require(count <= 100000) { "Too many windows" }
        val windows = (0 until count).map { index ->
            val start = index * windowSeconds
            val end = minOf(durationSeconds, start + windowSeconds)
            val local = sorted.filter { it >= start && (it < end || (index == count - 1 && it <= end)) }
            val intervals = local.zipWithNext().map { it.second - it.first }
                .filter { it >= period * (1 - tolerance) && it <= period * (1 + tolerance) }.sorted()
            val median = when {
                intervals.isEmpty() -> Double.NaN
                intervals.size % 2 == 1 -> intervals[intervals.size / 2]
                else -> (intervals[intervals.size / 2 - 1] + intervals[intervals.size / 2]) / 2.0
            }
            Window(start, end, intervals.size, 60.0 / median, intervals.size >= minPairs)
        }
        val comparisons = windows.zipWithNext().filter { it.first.sufficientEvidence && it.second.sufficientEvidence }
            .map { abs(it.first.bpm - it.second.bpm) / referenceBpm }
        val enough = comparisons.isNotEmpty()
        return Report(windows, windows.count { it.sufficientEvidence }, comparisons.size,
            comparisons.maxOrNull() ?: Double.NaN, enough && comparisons.all { it <= maxDrift }, enough)
    }
}
