package com.danim.autowarp.analysis

/** Independent conservative gate for consecutive pulse evidence.
 * A single strong hit or a cluster in one pulse slot must not authorize a warp.
 * This gate never increases confidence and leaves raw detections intact.
 */
object KickSequenceGate {
    data class Result(val supportedCount: Int, val confidenceMultiplier: Double, val evaluable: Boolean)

    fun assess(support: List<Double>, minimumCandidates: Int = 6,
               minimumSupported: Int = 3, threshold: Double = 0.30): Result {
        require(minimumCandidates >= 1 && minimumSupported >= 1)
        require(threshold.isFinite() && threshold in 0.0..1.0)
        require(support.all { it.isFinite() && it in 0.0..1.0 })
        val count = support.count { it >= threshold }
        val evaluable = support.size >= minimumCandidates
        return Result(count, if (evaluable && count < minimumSupported) 0.0 else 1.0, evaluable)
    }
}
