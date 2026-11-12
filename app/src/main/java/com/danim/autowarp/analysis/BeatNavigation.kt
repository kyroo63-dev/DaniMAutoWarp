package com.danim.autowarp.analysis

import kotlin.math.floor

/** Navigates a constant-tempo reference grid without cumulative floating point drift. */
object BeatNavigation {
    fun shift(startSeconds: Double, bpm: Double, beats: Int, maxStartSeconds: Double): Double {
        require(startSeconds.isFinite() && maxStartSeconds.isFinite() && maxStartSeconds >= 0)
        if (!bpm.isFinite() || bpm <= 0.0) return startSeconds.coerceIn(0.0, maxStartSeconds)
        val beatSeconds = 60.0 / bpm
        val index = floor(startSeconds / beatSeconds + 1e-8).toLong()
        return ((index + beats) * beatSeconds).coerceIn(0.0, maxStartSeconds)
    }
}
