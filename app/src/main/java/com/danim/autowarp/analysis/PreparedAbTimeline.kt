package com.danim.autowarp.analysis

/** Immutable, validated timeline for low-allocation A/B preview sample lookup. */
class PreparedAbTimeline(markers: List<WarpMarker>) {
    private val points = markers.sortedBy { it.targetSeconds }
    init {
        require(points.size >= 2)
        require(points.all { it.targetSeconds.isFinite() && it.sourceSeconds.isFinite() && it.strength.isFinite() && it.strength in 0f..1f })
        require(points.zipWithNext().all { (a,b) -> a.targetSeconds < b.targetSeconds && a.sourceSeconds < b.sourceSeconds })
    }
    fun sourceAt(targetSeconds: Double): Double {
        require(targetSeconds.isFinite())
        val t = targetSeconds.coerceIn(points.first().targetSeconds, points.last().targetSeconds)
        var lo = 0
        var hi = points.size - 2
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (t > points[mid + 1].targetSeconds) lo = mid + 1 else hi = mid
        }
        val a = points[lo]; val b = points[lo + 1]
        val fraction = (t - a.targetSeconds) / (b.targetSeconds - a.targetSeconds)
        val source = a.sourceSeconds + (b.sourceSeconds - a.sourceSeconds) * fraction
        val strength = (a.strength + (b.strength - a.strength) * fraction).toDouble().coerceIn(0.0, 1.0)
        return (t * (1.0 - strength) + source * strength).coerceIn(points.first().sourceSeconds, points.last().sourceSeconds)
    }
}
