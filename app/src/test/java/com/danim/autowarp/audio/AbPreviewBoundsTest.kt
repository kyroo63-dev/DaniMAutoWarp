package com.danim.autowarp.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AbPreviewBoundsTest {
    @Test fun clipsToWarpInterval() {
        assertEquals(48000L, AbPreviewBounds.frames(48000, 96000, 10.0, 11.0))
    }
    @Test fun clipsToRenderedAudio() {
        assertEquals(1200L, AbPreviewBounds.frames(48000, 1200, 10.0, 11.0))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsSubSampleInterval() { AbPreviewBounds.frames(48000, 100, 10.0, 10.000001) }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidRange() { AbPreviewBounds.frames(48000, 100, 10.0, 9.0) }
}
