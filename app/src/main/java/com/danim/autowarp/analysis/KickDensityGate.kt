package com.danim.autowarp.analysis

/** Conservative plausibility check for over-detected kick candidates.
 * A kick candidate is not a verified instrument label. Excessive density blocks
 * automatic warp, but does not delete detections from the manual review timeline.
 */
object KickDensityGate {
    data class Result(val candidatesPerSecond: Double, val plausible: Boolean)
    /** Sliding-window check catches bursts concealed by a low whole-clip average.
     * Windows are time-based, not aligned to fixed bins; candidate timestamps are
     * assumed to be seconds from the start of the analyzed clip.
     */
    data class TimelineResult(val global: Result, val maximumWindowDensity: Double,
                              val plausible: Boolean)
    fun assessTimeline(candidateSeconds: List<Double>, durationSeconds: Double,
                       maximumPerSecond: Double = 3.5, windowSeconds: Double = 2.0): TimelineResult {
        require(durationSeconds.isFinite() && durationSeconds > 0.0)
        require(maximumPerSecond.isFinite() && maximumPerSecond > 0.0)
        require(windowSeconds.isFinite() && windowSeconds > 0.0)
        require(candidateSeconds.all { it.isFinite() && it >= 0.0 && it <= durationSeconds })
        val global = assess(candidateSeconds.size, durationSeconds, maximumPerSecond)
        val sorted = candidateSeconds.sorted()
        val width = minOf(windowSeconds, durationSeconds)
        var right = 0
        var maxCount = 0
        for (left in sorted.indices) {
            if (right < left) right = left
            while (right < sorted.size && sorted[right] - sorted[left] < width) right++
            maxCount = maxOf(maxCount, right - left)
        }
        val peak = maxCount / width
        return TimelineResult(global, peak, global.plausible && peak <= maximumPerSecond)
    }
    fun assess(candidateCount: Int, durationSeconds: Double, maximumPerSecond: Double = 3.5): Result {
        require(candidateCount >= 0 && durationSeconds.isFinite() && durationSeconds > 0.0)
        require(maximumPerSecond.isFinite() && maximumPerSecond > 0.0)
        val density = candidateCount / durationSeconds
        return Result(density, density <= maximumPerSecond)
    }
}
