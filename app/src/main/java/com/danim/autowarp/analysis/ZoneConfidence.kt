package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Heuristic evidence score, not a calibrated probability of beat correctness. */
data class ConfidenceZone(
    val startSeconds: Double,
    val endSeconds: Double,
    val score: Double,
    val supportedBeats: Int,
    val totalBeats: Int,
    val competingPercussion: Int,
    val requiresReview: Boolean
)

object ZoneConfidenceAnalyzer {
    /** Fixed-duration zones, with each beat assigned to exactly one zone. */
    fun analyze(durationSeconds: Double, beats: List<Double>, events: List<TransientEvent>,
                zoneSeconds: Double = 16.0): List<ConfidenceZone> {
        if (!durationSeconds.isFinite() || durationSeconds <= 0.0 ||
            !zoneSeconds.isFinite() || zoneSeconds <= 0.0) return emptyList()
        val validBeats = beats.filter { it.isFinite() && it >= 0.0 && it < durationSeconds }.sorted()
        val validEvents = events.filter { it.seconds.isFinite() && it.seconds >= 0.0 &&
            it.seconds < durationSeconds && it.confidence.isFinite() }
        val result = ArrayList<ConfidenceZone>()
        var start = 0.0
        while (start < durationSeconds) {
            val end = min(durationSeconds, start + zoneSeconds)
            val localBeats = validBeats.filter { it >= start && it < end }
            val localEvents = validEvents.filter { it.seconds >= start && it.seconds < end }
            val kicks = localEvents.filter { it.kind == TransientKind.KICK }
            val competitors = localEvents.filter { it.kind == TransientKind.SNARE || it.kind == TransientKind.PERCUSSION }
            val supported = localBeats.count { beat -> kicks.any { abs(it.seconds - beat) <= 0.075 && it.confidence >= 0.25 } }
            val supportRatio = if (localBeats.isEmpty()) 0.0 else supported.toDouble() / localBeats.size
            val kickQuality = if (kicks.isEmpty()) 0.0 else kicks.map { it.confidence.coerceIn(0.0, 1.0) }.average()
            val competingOnGrid = competitors.count { e -> localBeats.any { abs(e.seconds - it) <= 0.075 } }
            val competitionPenalty = min(0.20, competingOnGrid.toDouble() / max(1, localBeats.size) * 0.20)
            val coveragePenalty = if (localBeats.size < 4) 0.20 else 0.0
            val score = (0.75 * supportRatio + 0.25 * kickQuality - competitionPenalty - coveragePenalty).coerceIn(0.0, 1.0)
            result.add(ConfidenceZone(start, end, score, supported, localBeats.size,
                competingOnGrid, score < 0.60))
            start = end
        }
        return result
    }
}
