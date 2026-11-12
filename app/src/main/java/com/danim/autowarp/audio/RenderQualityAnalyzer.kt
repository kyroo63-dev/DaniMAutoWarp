package com.danim.autowarp.audio

import kotlin.math.abs
import kotlin.math.sqrt

/** Objective safety checks for a rendered PCM signal; these are diagnostics, not a perceptual quality score. */
data class RenderQualityReport(
    val peak: Double,
    val rms: Double,
    val dcOffset: Double,
    val clippedSamples: Int,
    val discontinuities: Int,
    val finiteSamples: Boolean,
    val durationDifferenceMs: Double
) {
    val hasWarnings: Boolean get() = clippedSamples > 0 || discontinuities > 0 || !finiteSamples || abs(dcOffset) > 0.02
}

object RenderQualityAnalyzer {
    fun analyze(source: PcmAudio, rendered: PcmAudio, discontinuityThreshold: Double = 0.35): RenderQualityReport {
        require(source.sampleRate == rendered.sampleRate && source.channels == rendered.channels) {
            "La fuente y el render deben tener la misma frecuencia y canales"
        }
        require(discontinuityThreshold > 0.0)
        var peak = 0.0
        var sumSquares = 0.0
        var sum = 0.0
        var clipped = 0
        var discontinuities = 0
        var finite = true
        val samples = rendered.samples
        for (i in samples.indices) {
            val x = samples[i].toDouble()
            if (!x.isFinite()) { finite = false; continue }
            val magnitude = abs(x)
            if (magnitude > peak) peak = magnitude
            if (magnitude >= 0.999) clipped++
            sum += x
            sumSquares += x * x
            if (i >= rendered.channels && i % rendered.channels == 0) {
                var frameJump = 0.0
                for (c in 0 until rendered.channels) {
                    val current = samples[i + c].toDouble()
                    val previous = samples[i - rendered.channels + c].toDouble()
                    if (current.isFinite() && previous.isFinite()) frameJump = maxOf(frameJump, abs(current - previous))
                }
                if (frameJump > discontinuityThreshold) discontinuities++
            }
        }
        val count = samples.size.coerceAtLeast(1)
        return RenderQualityReport(
            peak, sqrt(sumSquares / count), sum / count, clipped, discontinuities, finite,
            abs(source.durationSeconds - rendered.durationSeconds) * 1000.0
        )
    }
}
