package com.danim.autowarp.audio

/** Sample-domain A/B transition. Reversing direction preserves the current gain,
 * avoiding an audible jump during rapid toggling. Call mix for each channel of a
 * frame and advanceFrame exactly once after all channels have been processed.
 */
class AbCrossfade(sampleRate: Int, transitionMillis: Int = 12) {
    init { require(sampleRate > 0 && transitionMillis in 1..1000) }
    val fadeFrames: Int = ((sampleRate.toLong() * transitionMillis) / 1000).coerceAtLeast(1).toInt()
    private var gain = 0f
    private var target = 0f
    private var step = 0f
    private var remaining = 0

    fun reset(corrected: Boolean) {
        gain = if (corrected) 1f else 0f
        target = gain
        step = 0f
        remaining = 0
    }

    fun mix(original: Float, corrected: Float, selectCorrected: Boolean): Float {
        val wanted = if (selectCorrected) 1f else 0f
        if (wanted != target) {
            target = wanted
            remaining = fadeFrames
            step = (target - gain) / fadeFrames
        }
        val effectiveGain = if (remaining > 0) (gain + step).coerceIn(0f, 1f) else gain
        val a = if (original.isFinite()) original else 0f
        val b = if (corrected.isFinite()) corrected else 0f
        return a * (1f - effectiveGain) + b * effectiveGain
    }

    fun advanceFrame() {
        if (remaining > 0) {
            gain = (gain + step).coerceIn(0f, 1f)
            remaining--
            if (remaining == 0) gain = target
        }
    }
}
