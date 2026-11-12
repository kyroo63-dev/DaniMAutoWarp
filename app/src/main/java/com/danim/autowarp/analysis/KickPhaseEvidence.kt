package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.min

/** Measures how many predicted beat positions are backed by actual kick attacks.
 * Interpolated beats alone must not grant confidence to an automatic warp.
 */
object KickPhaseEvidence {
    data class Result(val supportRatio: Double, val medianErrorMs: Double, val reliable: Boolean)

    fun assess(beats: List<Double>, kicks: List<Double>, periodSeconds: Double): Result {
        if (beats.size < 8 || kicks.size < 4 || !periodSeconds.isFinite() || periodSeconds <= 0.0 ||
            beats.any { !it.isFinite() } || beats.zipWithNext().any { it.first >= it.second } ||
            kicks.any { !it.isFinite() })
            return Result(0.0, Double.POSITIVE_INFINITY, false)
        val sorted = kicks.sorted()
        val tolerance = min(0.055, periodSeconds * 0.12)
        val errors = ArrayList<Double>()
        // Match each observed attack at most once. A dense, duplicated beatgrid must
        // not gain confidence by repeatedly reusing the same physical kick.
        var nextKick = 0
        for (beat in beats) {
            while (nextKick < sorted.size && sorted[nextKick] < beat - tolerance) nextKick++
            if (nextKick < sorted.size && abs(sorted[nextKick] - beat) <= tolerance) {
                errors.add(abs(sorted[nextKick] - beat) * 1000.0)
                nextKick++
            }
        }
        val ratio = errors.size.toDouble() / beats.size
        val median = if (errors.isEmpty()) Double.POSITIVE_INFINITY else errors.sorted()[errors.size / 2]
        return Result(ratio, median, ratio >= 0.55 && median <= 35.0)
    }
}
