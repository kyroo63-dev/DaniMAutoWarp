package com.danim.autowarp.audio

import kotlin.math.abs
import kotlin.math.sqrt

/** Constant-memory PCM diagnostics for long renders; no perceptual quality claims. */
object StreamingQualityAnalyzer {
    fun analyze(source: PcmFrameSource, rendered: PcmFrameSource, chunkFrames: Int = 4096,
                discontinuityThreshold: Double = 0.35): RenderQualityReport {
        require(source.sampleRate == rendered.sampleRate && source.channels == rendered.channels)
        require(chunkFrames > 0 && discontinuityThreshold > 0.0)
        val channels = rendered.channels
        var peak = 0.0
        var squares = 0.0
        var sum = 0.0
        var clipped = 0
        var jumps = 0
        var finite = true
        var validCount = 0L
        var offset = 0L
        val previous = DoubleArray(channels)
        var hasPrevious = false
        while (offset < rendered.frameCount) {
            val count = minOf(chunkFrames.toLong(), rendered.frameCount - offset).toInt()
            val samples = rendered.readFrames(offset, count)
            require(samples.size == count * channels) { "Lectura PCM incompleta" }
            for (frame in 0 until count) {
                var frameJump = 0.0
                for (channel in 0 until channels) {
                    val x = samples[frame * channels + channel].toDouble()
                    if (!x.isFinite()) { finite = false; continue }
                    peak = maxOf(peak, abs(x))
                    if (abs(x) >= 0.999) clipped++
                    squares += x * x
                    sum += x
                    validCount++
                    if (hasPrevious && previous[channel].isFinite())
                        frameJump = maxOf(frameJump, abs(x - previous[channel]))
                    previous[channel] = x
                }
                if (hasPrevious && frameJump > discontinuityThreshold) jumps++
                hasPrevious = true
            }
            offset += count
        }
        val n = validCount.coerceAtLeast(1L).toDouble()
        val durationDelta = abs(source.frameCount - rendered.frameCount).toDouble() * 1000.0 / source.sampleRate
        return RenderQualityReport(peak, sqrt(squares / n), sum / n, clipped, jumps, finite, durationDelta)
    }
}
