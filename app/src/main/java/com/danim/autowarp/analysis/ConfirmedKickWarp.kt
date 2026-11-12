package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.roundToInt

/** Rebuilds a conservative warp from manually classified kick onsets.
 * The constant-tempo grid from the original analysis remains the reference.
 */
object ConfirmedKickWarp {
    data class Outcome(val markers: List<WarpMarker>, val matched: Int, val rejected: Int)

    fun rebuild(analysis: AnalysisResult, events: List<TransientEvent>, duration: Double): Outcome {
        require(duration.isFinite() && duration > 0.0)
        val beats = analysis.beatTimes.filter { it.isFinite() }.sorted()
        if (beats.size < 3 || analysis.bpm <= 0.0) return Outcome(analysis.markers, 0, 0)
        val period = 60.0 / analysis.bpm
        val origin = beats.first()
        val tolerance = (period * 0.22).coerceIn(0.035, 0.12)
        val matched = mutableMapOf<Int, TransientEvent>()
        for (event in events.filter { it.kind == TransientKind.KICK && it.seconds.isFinite() && it.seconds in 0.0..duration }) {
            val index = ((event.seconds - origin) / period).roundToInt()
            if (index !in beats.indices) continue
            val target = origin + index * period
            if (abs(event.seconds - beats[index]) > tolerance && abs(event.seconds - target) > tolerance) continue
            val old = matched[index]
            if (old == null || event.confidence > old.confidence) matched[index] = event
        }
        if (matched.size < 3) return Outcome(analysis.markers, matched.size, 0)
        val proposed = matched.toSortedMap().map { (index, event) ->
            WarpMarker(event.seconds, origin + index * period, event.confidence.toFloat().coerceIn(0f, 1f))
        }
        val safe = WarpSafetyGate.sanitize(proposed, duration)
        return Outcome(safe.markers, matched.size, safe.rejected)
    }
}
