package com.danim.autowarp.audio

import com.danim.autowarp.analysis.TransientEvent
import com.danim.autowarp.analysis.WarpMarker
import kotlin.math.*

enum class WarpQuality(val grainMs: Int, val searchMs: Int, val step: Int) {
    FAST(48, 6, 8), BALANCED(72, 10, 4), HIGH(96, 14, 2)
}

data class WarpRenderReport(
    val output: PcmAudio,
    val lockedTransients: Int,
    val averageStretchRatio: Double,
    val maxStretchRatio: Double
)

/**
 * Transient-aware WSOLA renderer. Stereo channels share one alignment decision to avoid
 * image wandering. Source/destination anchors and mapped transient positions constrain
 * the search. This remains a native Kotlin reference engine, not a commercial elastique clone.
 */
object WsolaWarp {
    fun render(
        input: PcmAudio,
        markers: List<WarpMarker>,
        transients: List<TransientEvent> = emptyList(),
        quality: WarpQuality = WarpQuality.BALANCED
    ): PcmAudio = renderDetailed(input, markers, transients, quality).output

    fun renderDetailed(
        input: PcmAudio,
        markers: List<WarpMarker>,
        transients: List<TransientEvent> = emptyList(),
        quality: WarpQuality = WarpQuality.BALANCED
    ): WarpRenderReport {
        require(markers.size >= 2) { "Se necesitan al menos dos marcadores" }
        val base = markers.sortedBy { it.targetSeconds }
        require(base.all { it.sourceSeconds in 0.0..input.durationSeconds && it.targetSeconds >= 0.0 }) { "Marcador fuera del audio" }
        require(base.zipWithNext().all { it.second.targetSeconds > it.first.targetSeconds && it.second.sourceSeconds > it.first.sourceSeconds }) {
            "Los marcadores deben ser estrictamente crecientes"
        }
        val hasEnd = base.last().sourceSeconds >= input.durationSeconds - 1.5 / input.sampleRate
        val duration = if (hasEnd) base.last().targetSeconds else base.last().targetSeconds +
            (base.last().targetSeconds - base[base.lastIndex - 1].targetSeconds).coerceAtLeast(0.25)
        val outputFrames = (duration * input.sampleRate).roundToInt().coerceAtLeast(1)

        fun mapSourceToTarget(sourceSeconds: Double): Double {
            var lo = 0; var hi = base.lastIndex - 1
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (sourceSeconds > base[mid + 1].sourceSeconds) lo = mid + 1 else hi = mid }
            val a = base[lo]; val b = base[min(lo + 1, base.lastIndex)]
            val f = ((sourceSeconds - a.sourceSeconds) / (b.sourceSeconds - a.sourceSeconds)).coerceIn(0.0, 1.0)
            return a.targetSeconds + (b.targetSeconds - a.targetSeconds) * f
        }

        // Add high-confidence transient anchors to the map. Nearby beat anchors take priority.
        val transientAnchors = transients.asSequence()
            .filter { it.confidence >= 0.48 && it.seconds > 0.015 && it.seconds < input.durationSeconds - 0.015 }
            .map { WarpMarker(it.seconds, mapSourceToTarget(it.seconds), it.confidence.toFloat()) }
            .filter { candidate -> base.none { abs(it.sourceSeconds - candidate.sourceSeconds) < 0.018 } }
            .sortedBy { it.sourceSeconds }.toList()
        val all = (base + transientAnchors).sortedBy { it.sourceSeconds }
        val anchors = all.fold(ArrayList<WarpMarker>()) { acc, marker ->
            if (acc.isEmpty() || (marker.sourceSeconds > acc.last().sourceSeconds && marker.targetSeconds > acc.last().targetSeconds)) acc.add(marker)
            acc
        }

        fun sourceAt(targetFrame: Int): Int {
            val t = targetFrame.toDouble() / input.sampleRate
            var lo = 0; var hi = anchors.lastIndex - 1
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (t > anchors[mid + 1].targetSeconds) lo = mid + 1 else hi = mid }
            val a = anchors[lo]; val b = anchors[min(lo + 1, anchors.lastIndex)]
            val f = ((t - a.targetSeconds) / (b.targetSeconds - a.targetSeconds)).coerceIn(0.0, 1.0)
            return ((a.sourceSeconds + (b.sourceSeconds - a.sourceSeconds) * f) * input.sampleRate)
                .roundToInt().coerceIn(0, input.frames - 1)
        }
        fun localRatio(frame: Int): Double {
            val t = frame.toDouble() / input.sampleRate
            var lo = 0; var hi = anchors.lastIndex - 1
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (t > anchors[mid + 1].targetSeconds) lo = mid + 1 else hi = mid }
            val a = anchors[lo]; val b = anchors[min(lo + 1, anchors.lastIndex)]
            return ((b.sourceSeconds - a.sourceSeconds) / (b.targetSeconds - a.targetSeconds)).coerceIn(0.25, 4.0)
        }

        val grain = (input.sampleRate * quality.grainMs / 1000).coerceIn(256, 8192)
        val hop = (grain / 4).coerceAtLeast(64)
        val search = (input.sampleRate * quality.searchMs / 1000).coerceAtLeast(16)
        val accum = DoubleArray(outputFrames * input.channels)
        val weights = DoubleArray(outputFrames)
        var outPos = 0
        var locked = 0
        var ratioSum = 0.0
        var ratioMax = 0.0
        var grains = 0
        val maxStart = (input.frames - grain).coerceAtLeast(0)

        while (outPos < outputFrames) {
            val expected = sourceAt(outPos)
            val ratio = localRatio(outPos)
            ratioSum += ratio; ratioMax = max(ratioMax, max(ratio, 1.0 / ratio)); grains++
            val targetTime = outPos.toDouble() / input.sampleRate
            val nearAnchor = anchors.minByOrNull { abs(it.targetSeconds - targetTime) }
                ?.takeIf { abs(it.targetSeconds - targetTime) <= hop.toDouble() / input.sampleRate * 0.48 }
            val radius = if (nearAnchor != null) 0 else min(search, (search * sqrt(ratio)).roundToInt().coerceAtLeast(8))
            if (nearAnchor != null && transientAnchors.any { abs(it.sourceSeconds - nearAnchor.sourceSeconds) < 1e-7 }) locked++
            val low = (expected - radius).coerceIn(0, maxStart)
            val high = (expected + radius).coerceIn(low, maxStart)
            var best = expected.coerceIn(low, high)
            var bestScore = Double.NEGATIVE_INFINITY
            if (outPos > 0 && nearAnchor == null) {
                val overlap = min(grain - hop, outPos)
                val stride = quality.step
                var candidate = low
                while (candidate <= high) {
                    var dot = 0.0; var aa = 0.0; var bb = 0.0
                    var n = 0
                    while (n < overlap) {
                        val oi = outPos - overlap + n
                        val denom = weights[oi].coerceAtLeast(1e-9)
                        var prev = 0.0; var next = 0.0
                        for (c in 0 until input.channels) {
                            prev += accum[oi * input.channels + c] / denom
                            next += input.samples[(candidate + n) * input.channels + c]
                        }
                        prev /= input.channels; next /= input.channels
                        // Slightly emphasize the beginning of the overlap, where transient shape matters most.
                        val w = 0.35 + 0.65 * (n.toDouble() / overlap.coerceAtLeast(1))
                        dot += prev * next * w; aa += prev * prev * w; bb += next * next * w
                        n += max(2, input.sampleRate / 3000)
                    }
                    val score = dot / sqrt(max(1e-18, aa * bb))
                    if (score > bestScore) { bestScore = score; best = candidate }
                    candidate += stride
                }
                // Fine refinement around the best coarse candidate.
                if (quality == WarpQuality.FAST) Unit else {
                    val refineLo = max(low, best - stride); val refineHi = min(high, best + stride)
                    for (candidate in refineLo..refineHi) {
                        var dot = 0.0; var aa = 0.0; var bb = 0.0
                        val overlap = min(grain - hop, outPos)
                        var n = 0
                        while (n < overlap) {
                            val oi = outPos - overlap + n; val denom = weights[oi].coerceAtLeast(1e-9)
                            var p = 0.0; var q = 0.0
                            for (c in 0 until input.channels) { p += accum[oi * input.channels + c] / denom; q += input.samples[(candidate + n) * input.channels + c] }
                            p /= input.channels; q /= input.channels
                            dot += p * q; aa += p * p; bb += q * q
                            n += max(2, input.sampleRate / 3000)
                        }
                        val score = dot / sqrt(max(1e-18, aa * bb))
                        if (score > bestScore) { bestScore = score; best = candidate }
                    }
                }
            }
            val len = min(grain, min(outputFrames - outPos, input.frames - best))
            if (len <= 0) break
            for (n in 0 until len) {
                // Sine-squared window gives a smooth, energy-stable overlap-add.
                val phase = if (len <= 1) 0.5 else n.toDouble() / (len - 1)
                val w = sin(PI * phase).pow(2.0).coerceAtLeast(1e-5)
                val oi = outPos + n
                weights[oi] += w
                for (c in 0 until input.channels) accum[oi * input.channels + c] += input.samples[(best + n) * input.channels + c] * w
            }
            outPos += hop
        }
        val output = FloatArray(outputFrames * input.channels)
        for (i in 0 until outputFrames) for (c in 0 until input.channels) {
            output[i * input.channels + c] = if (weights[i] > 1e-8) (accum[i * input.channels + c] / weights[i]).toFloat().coerceIn(-1f, 1f) else 0f
        }
        return WarpRenderReport(PcmAudio(input.sampleRate, input.channels, output), locked,
            if (grains > 0) ratioSum / grains else 1.0, ratioMax)
    }
}
