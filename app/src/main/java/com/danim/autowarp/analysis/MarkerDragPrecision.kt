package com.danim.autowarp.analysis

import kotlin.math.abs

/** Keeps a finger's initial offset from the marker to prevent jump-to-touch edits. */
object MarkerDragPrecision {
    fun proposedSource(
        originalMarkerSeconds: Double,
        initialTouchX: Float,
        currentTouchX: Float,
        widthPx: Float,
        visibleSeconds: Double,
        previousSeconds: Double,
        nextSeconds: Double,
        minimumGapSeconds: Double = 0.001
    ): Double? {
        if (!originalMarkerSeconds.isFinite() || !initialTouchX.isFinite() ||
            !currentTouchX.isFinite() || !widthPx.isFinite() || widthPx <= 0f ||
            !visibleSeconds.isFinite() || visibleSeconds <= 0.0 ||
            !previousSeconds.isFinite() || !nextSeconds.isFinite() ||
            !minimumGapSeconds.isFinite() || minimumGapSeconds <= 0.0) return null
        val low = previousSeconds + minimumGapSeconds
        val high = nextSeconds - minimumGapSeconds
        if (low >= high || originalMarkerSeconds !in low..high) return null
        val delta = (currentTouchX.toDouble() - initialTouchX.toDouble()) / widthPx * visibleSeconds
        val proposed = (originalMarkerSeconds + delta).coerceIn(low, high)
        return proposed.takeIf { it.isFinite() && abs(it - originalMarkerSeconds) >= 0.000001 }
    }
}
