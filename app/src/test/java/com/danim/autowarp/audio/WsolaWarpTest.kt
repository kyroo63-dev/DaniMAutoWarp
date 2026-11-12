package com.danim.autowarp.audio

import com.danim.autowarp.analysis.TransientEvent
import com.danim.autowarp.analysis.TransientKind
import com.danim.autowarp.analysis.WarpMarker
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class WsolaWarpTest {
    @Test fun endAnchorPreservesFullTrackDuration() {
        val sr = 8000
        val samples = FloatArray(sr * 2) { i -> (0.2 * sin(2 * PI * 110 * i / sr)).toFloat() }
        val input = PcmAudio(sr, 1, samples)
        val output = WsolaWarp.render(input, listOf(WarpMarker(0.0, 0.0), WarpMarker(1.0, 1.0), WarpMarker(2.0, 2.0)))
        assertEquals(input.frames, output.frames)
        assertEquals(input.sampleRate, output.sampleRate)
    }

    @Test fun transientAnchorsAreCountedAndKeepDuration() {
        val sr = 8000
        val samples = FloatArray(sr * 3) { i -> (0.12 * sin(2 * PI * 90 * i / sr)).toFloat() }
        val input = PcmAudio(sr, 1, samples)
        val markers = listOf(WarpMarker(0.0, 0.0), WarpMarker(1.5, 1.45), WarpMarker(3.0, 3.0))
        val events = listOf(TransientEvent(0.72, TransientKind.KICK, 0.95), TransientEvent(2.1, TransientKind.SNARE, 0.8))
        val report = WsolaWarp.renderDetailed(input, markers, events, WarpQuality.BALANCED)
        assertEquals(input.frames, report.output.frames)
        assertTrue("At least one transient anchor should lock", report.lockedTransients >= 1)
        assertTrue(report.averageStretchRatio > 0.0)
    }

    @Test fun stereoChannelsRemainLinkedAndOutputFinite() {
        val sr = 8000
        val frames = sr * 2
        val samples = FloatArray(frames * 2) { i ->
            val frame = i / 2
            (if (i % 2 == 0) 0.2 * sin(2 * PI * 110 * frame / sr) else 0.18 * sin(2 * PI * 110 * frame / sr)).toFloat()
        }
        val input = PcmAudio(sr, 2, samples)
        val output = WsolaWarp.render(input, listOf(WarpMarker(0.0, 0.0), WarpMarker(1.0, 0.98), WarpMarker(2.0, 2.0)), quality = WarpQuality.HIGH)
        assertEquals(2, output.channels)
        assertTrue(output.samples.all { it.isFinite() })
        assertEquals(frames, output.frames)
    }
}
