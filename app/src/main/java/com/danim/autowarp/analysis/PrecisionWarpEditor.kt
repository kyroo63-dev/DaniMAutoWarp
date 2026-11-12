package com.danim.autowarp.analysis

import kotlin.math.abs

/** Pure, non-destructive editor for precise source-anchor movements. */
object PrecisionWarpEditor {
    fun nudgeSource(markers: List<WarpMarker>, index: Int, deltaMilliseconds: Double): List<WarpMarker> {
        require(deltaMilliseconds.isFinite())
        require(index > 0 && index < markers.lastIndex) { "Only interior anchors can move" }
        val current = markers[index]
        val lower = markers[index - 1].sourceSeconds + 0.001
        val upper = markers[index + 1].sourceSeconds - 0.001
        if (lower > upper) return markers
        val proposed = (current.sourceSeconds + deltaMilliseconds / 1000.0).coerceIn(lower, upper)
        if (abs(proposed - current.sourceSeconds) < 1e-10) return markers
        return markers.toMutableList().also { it[index] = current.copy(sourceSeconds = proposed) }
    }
    /** Insert a source anchor, mapping its target by piecewise-linear interpolation. */
    fun insert(markers: List<WarpMarker>, sourceSeconds: Double, minGap: Double = 0.005): List<WarpMarker> {
        require(sourceSeconds.isFinite() && minGap > 0)
        if (markers.size < 2) return markers
        val right = markers.indexOfFirst { it.sourceSeconds > sourceSeconds }
        if (right <= 0) return markers
        val left = markers[right - 1]
        val next = markers[right]
        if (sourceSeconds - left.sourceSeconds < minGap || next.sourceSeconds - sourceSeconds < minGap) return markers
        val fraction = (sourceSeconds - left.sourceSeconds) / (next.sourceSeconds - left.sourceSeconds)
        val target = left.targetSeconds + fraction * (next.targetSeconds - left.targetSeconds)
        return markers.toMutableList().also { it.add(right, WarpMarker(sourceSeconds, target, 1f)) }
    }

    /** Remove only an interior anchor; the first and last anchors are protected. */
    fun remove(markers: List<WarpMarker>, index: Int): List<WarpMarker> =
        if (index <= 0 || index >= markers.lastIndex) markers else markers.filterIndexed { i, _ -> i != index }

    /** Snap a source anchor to the closest transient inside the tolerance window. */
    fun snap(markers: List<WarpMarker>, index: Int, transientSeconds: List<Double>, toleranceMs: Double = 35.0): List<WarpMarker> {
        require(toleranceMs.isFinite() && toleranceMs >= 0)
        if (index <= 0 || index >= markers.lastIndex) return markers
        val current = markers[index].sourceSeconds
        val closest = transientSeconds.asSequence().filter { it.isFinite() }
            .minByOrNull { abs(it - current) } ?: return markers
        if (abs(closest - current) > toleranceMs / 1000.0) return markers
        return nudgeSource(markers, index, (closest - current) * 1000.0)
    }
}
