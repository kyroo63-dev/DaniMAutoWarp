package com.danim.autowarp.analysis

/** Calculates a playable A/B preview range inside the validated warp timeline. */
data class AbPreviewRange(val startSeconds: Double, val durationSeconds: Double)

object AbPreviewRanges {
    fun select(markers: List<WarpMarker>, requestedStartSeconds: Double, preferredDurationSeconds: Double = 8.0): AbPreviewRange {
        require(requestedStartSeconds.isFinite() && preferredDurationSeconds.isFinite() && preferredDurationSeconds > 0.0)
        val timeline = markers.sortedBy { it.targetSeconds }
        require(timeline.size >= 2)
        require(timeline.all { it.targetSeconds.isFinite() && it.sourceSeconds.isFinite() })
        require(timeline.zipWithNext().all { (a, b) -> a.targetSeconds < b.targetSeconds && a.sourceSeconds < b.sourceSeconds })
        val first = timeline.first().targetSeconds
        val last = timeline.last().targetSeconds
        require(last > first)
        // Prefer the requested start, but leave room for the full preview whenever possible.
        val duration = minOf(preferredDurationSeconds, last - first)
        val start = requestedStartSeconds.coerceIn(first, last - duration)
        return AbPreviewRange(start, duration)
    }
}
