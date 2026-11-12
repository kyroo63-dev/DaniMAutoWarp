package com.danim.autowarp.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbCrossfadeTest {
    @Test fun identicalSignalsStayAtUnityThroughRapidToggles() {
        val fade = AbCrossfade(48_000, 12)
        fade.reset(false)
        for (i in 0 until 4000) {
            val sample = fade.mix(0.65f, 0.65f, (i / 41) % 2 == 0)
            assertEquals(0.65f, sample, 1e-6f)
            fade.advanceFrame()
        }
    }

    @Test fun reversalsDoNotJumpInGain() {
        val fade = AbCrossfade(48_000, 12)
        fade.reset(false)
        var previous = 0f
        for (i in 0 until 2500) {
            val sample = fade.mix(0f, 1f, (i / 89) % 2 == 0)
            assertTrue(sample in 0f..1f)
            assertTrue(kotlin.math.abs(sample - previous) <= 1f / fade.fadeFrames + 1e-5f)
            previous = sample
            fade.advanceFrame()
        }
    }

    @Test fun stereoChannelsShareOneFrameGain() {
        val fade = AbCrossfade(44_100, 12)
        fade.reset(false)
        for (i in 0 until 600) {
            val left = fade.mix(0f, 1f, true)
            val right = fade.mix(0f, 1f, true)
            assertEquals(left, right, 0f)
            fade.advanceFrame()
        }
    }
}
