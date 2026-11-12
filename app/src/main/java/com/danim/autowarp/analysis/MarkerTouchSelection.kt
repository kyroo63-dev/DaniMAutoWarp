package com.danim.autowarp.analysis

import kotlin.math.abs

/** Pure hit-testing for narrow warp markers on a zoomable timeline. */
object MarkerTouchSelection {
    /** Returns an interior marker index, or -1 if no marker is within the touch radius. */
    fun nearest(
        markerTimes: List<Double>,
        touchX: Float,
        widthPx: Float,
        viewStartSeconds: Double,
        visibleSeconds: Double,
        radiusPx: Float
    ): Int {
        if (markerTimes.size < 3 || !touchX.isFinite() || !widthPx.isFinite() ||
            widthPx <= 0f || !viewStartSeconds.isFinite() || !visibleSeconds.isFinite() ||
            visibleSeconds <= 0.0 || !radiusPx.isFinite() || radiusPx < 0f) return -1
        var nearest = -1
        var distance = radiusPx
        for (index in 1 until markerTimes.lastIndex) {
            val seconds = markerTimes[index]
            if (!seconds.isFinite()) continue
            val x = ((seconds - viewStartSeconds) / visibleSeconds * widthPx).toFloat()
            if (!x.isFinite() || x < -radiusPx || x > widthPx + radiusPx) continue
            val delta = abs(x - touchX)
            if (delta <= distance) { distance = delta; nearest = index }
        }
        return nearest
    }
}
