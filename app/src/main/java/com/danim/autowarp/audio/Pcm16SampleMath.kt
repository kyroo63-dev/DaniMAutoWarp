package com.danim.autowarp.audio

import kotlin.math.roundToInt

/** PCM16 normalization and saturation, independent of Android's audio APIs. */
object Pcm16SampleMath {
    fun normalized(sample: Short): Float = sample.toInt() / 32768f

    fun encoded(sample: Float): Short {
        if (!sample.isFinite()) return 0
        return (sample.coerceIn(-1f, 32767f / 32768f) * 32768f).roundToInt().toShort()
    }

    /** Interpolate source PCM16 using a fractional frame index. */
    fun interpolated(samples: ShortArray, channels: Int, channel: Int, framePosition: Double): Float {
        require(channels in 1..2 && channel in 0 until channels)
        require(samples.isNotEmpty() && samples.size % channels == 0)
        require(framePosition.isFinite())
        val frames = samples.size / channels
        val position = framePosition.coerceIn(0.0, (frames - 1).toDouble())
        val first = position.toInt()
        val next = (first + 1).coerceAtMost(frames - 1)
        val fraction = (position - first).toFloat()
        return normalized(samples[first * channels + channel]) * (1f - fraction) +
            normalized(samples[next * channels + channel]) * fraction
    }
}
