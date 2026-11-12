package com.danim.autowarp.analysis

/** Read-only preview of a confirmed kick's prospective grid alignment. */
object ManualGridPreview {
    data class Preview(
        val accepted: Boolean,
        val sourceSeconds: Double,
        val currentTargetSeconds: Double?,
        val proposedTargetSeconds: Double?,
        val shiftMs: Double?,
        val leftStretchPercent: Double?,
        val rightStretchPercent: Double?,
        val reason: String?
    )

    fun inspect(markers: List<WarpMarker>, kickSeconds: Double, bpm: Double,
                phaseMs: Double = 0.0, confirmedByUser: Boolean = true): Preview {
        val result = ManualBeatGrid.align(markers, kickSeconds, bpm, phaseMs, confirmedByUser)
        val left = markers.indexOfLast { it.sourceSeconds < kickSeconds }
        val right = markers.indexOfFirst { it.sourceSeconds > kickSeconds }
        val exact = markers.firstOrNull { kotlin.math.abs(it.sourceSeconds - kickSeconds) < 0.000001 }
        val current = if (exact != null) exact.targetSeconds else if (left >= 0 && right > left) {
            val a = markers[left]; val b = markers[right]
            val t = (kickSeconds - a.sourceSeconds) / (b.sourceSeconds - a.sourceSeconds)
            a.targetSeconds + t * (b.targetSeconds - a.targetSeconds)
        } else null
        val target = result.targetSeconds
        val leftRatio = if (target != null && left >= 0) {
            val a = markers[left]; 100.0 * ((target - a.targetSeconds) / (kickSeconds - a.sourceSeconds) - 1.0)
        } else null
        val rightRatio = if (target != null && right >= 0) {
            val b = markers[right]; 100.0 * ((b.targetSeconds - target) / (b.sourceSeconds - kickSeconds) - 1.0)
        } else null
        return Preview(result.accepted, kickSeconds, current, target,
            if (current != null && target != null) (target - current) * 1000.0 else null,
            leftRatio, rightRatio, result.reason)
    }
}
