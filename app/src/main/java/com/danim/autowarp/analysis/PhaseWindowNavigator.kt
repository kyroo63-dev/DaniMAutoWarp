package com.danim.autowarp.analysis

/** Maps waveform taps to diagnostic windows and navigates to a bounded viewport. */
object PhaseWindowNavigator {
    fun at(windows: List<KickPhaseDrift.Window>, time: Double): Int =
        if (!time.isFinite()) -1 else windows.indexOfFirst {
            it.startSeconds.isFinite() && it.endSeconds.isFinite() &&
                time >= it.startSeconds && time < it.endSeconds
        }

    fun viewport(window: KickPhaseDrift.Window, duration: Double, zoom: Double): Double {
        if (!duration.isFinite() || duration <= 0.0 || !zoom.isFinite() || zoom < 1.0) return 0.0
        val visible = duration / zoom
        val center = (window.startSeconds + window.endSeconds) / 2.0
        return (center - visible / 2.0).coerceIn(0.0, (duration - visible).coerceAtLeast(0.0))
    }

    fun nearbyKicks(kicks: List<Double>, window: KickPhaseDrift.Window): List<Double> =
        kicks.filter { it.isFinite() && it >= window.startSeconds && it < window.endSeconds }.sorted()
}
