package com.danim.autowarp.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbCrossfadeSafetyTest {
    @Test fun identicalSignalsRemainUnityDuringRapidSwitching() {
        val fade = AbCrossfade(48000, 12)
        fade.reset(false)
        repeat(3000) { frame ->
            val selected = (frame / 53) % 2 == 1
            val left = fade.mix(0.4f, 0.4f, selected)
            val right = fade.mix(-0.4f, -0.4f, selected)
            assertEquals(0.4f, left, 1e-6f)
            assertEquals(-0.4f, right, 1e-6f)
            fade.advanceFrame()
        }
    }
    @Test fun invalidSamplesDoNotPropagate() {
        val fade = AbCrossfade(44100)
        fade.reset(false)
        repeat(2000) {
            val value = fade.mix(Float.NaN, Float.POSITIVE_INFINITY, true)
            assertTrue(value.isFinite())
            fade.advanceFrame()
        }
    }
}
