package com.danim.autowarp.analysis

/** Converts corrected timeline positions to the original recording timeline for A/B comparisons. */
object AbTimeline {
    fun sourceAt(targetSeconds: Double, markers: List<WarpMarker>): Double {
        require(targetSeconds.isFinite() && targetSeconds >= 0.0)
        val sorted = markers.sortedBy { it.targetSeconds }
        require(sorted.size >= 2 && sorted.zipWithNext().all { (a, b) ->
            a.targetSeconds.isFinite() && b.targetSeconds.isFinite() &&
            a.sourceSeconds.isFinite() && b.sourceSeconds.isFinite() &&
            a.targetSeconds < b.targetSeconds && a.sourceSeconds < b.sourceSeconds
        })
        val t = targetSeconds.coerceIn(sorted.first().targetSeconds, sorted.last().targetSeconds)
        var lo = 0
        var hi = sorted.size - 2
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (t > sorted[mid + 1].targetSeconds) lo = mid + 1 else hi = mid
        }
        val a = sorted[lo]
        val b = sorted[lo + 1]
        val fraction = (t - a.targetSeconds) / (b.targetSeconds - a.targetSeconds)
        val warped = a.sourceSeconds + (b.sourceSeconds - a.sourceSeconds) * fraction
        val strength = (a.strength + (b.strength - a.strength) * fraction).toDouble().coerceIn(0.0, 1.0)
        return (t * (1.0 - strength) + warped * strength).coerceIn(sorted.first().sourceSeconds, sorted.last().sourceSeconds)
    }
}
