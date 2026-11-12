package com.danim.autowarp.analysis

import kotlin.math.abs

/** Conservative guard for automatically detected vinyl warp markers.
 * Manual editing is not constrained by this helper.
 */
object WarpSafetyGate {
    data class Result(val markers: List<WarpMarker>, val rejected: Int)

    /** Rejects implausible local speed changes and non-monotonic anchors.
     * The original duration is always preserved, including the closing anchor.
     */
    fun sanitize(markers: List<WarpMarker>, duration: Double, minRatio: Double = 0.85,
                 maxRatio: Double = 1.15): Result {
        require(duration.isFinite() && duration > 0.0)
        require(minRatio > 0 && maxRatio >= minRatio)
        val start = WarpMarker(0.0, 0.0, 1f)
        val end = WarpMarker(duration, duration, 1f)
        val accepted = mutableListOf(start)
        var rejected = 0
        for (m in markers.sortedBy { it.sourceSeconds }) {
            if (!m.sourceSeconds.isFinite() || !m.targetSeconds.isFinite() ||
                m.sourceSeconds <= 0.005 || m.sourceSeconds >= duration - 0.005 ||
                m.targetSeconds <= 0.005 || m.targetSeconds >= duration - 0.005) continue
            val previous = accepted.last()
            val sourceDelta = m.sourceSeconds - previous.sourceSeconds
            val targetDelta = m.targetSeconds - previous.targetSeconds
            if (sourceDelta < 0.08 || targetDelta <= 0.0 ||
                targetDelta / sourceDelta !in minRatio..maxRatio ||
                abs(m.targetSeconds - m.sourceSeconds) > 0.5) {
                rejected++
                continue
            }
            accepted.add(m)
        }
        // If the final segment would require extreme correction, remove recent anchors.
        while (accepted.size > 1) {
            val previous = accepted.last()
            val ratio = (duration - previous.targetSeconds) / (duration - previous.sourceSeconds)
            if (ratio in minRatio..maxRatio) break
            accepted.removeAt(accepted.lastIndex)
            rejected++
        }
        accepted.add(end)
        return Result(accepted, rejected)
    }
}
