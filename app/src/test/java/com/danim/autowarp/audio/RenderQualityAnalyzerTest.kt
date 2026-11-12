package com.danim.autowarp.audio

import org.junit.Assert.*
import org.junit.Test

class RenderQualityAnalyzerTest {
    @Test fun reportsCleanSignalAndDuration() {
        val source = PcmAudio(8000, 1, FloatArray(800) { i -> (0.2 * kotlin.math.sin(2.0 * Math.PI * 440.0 * i / 8000.0)).toFloat() })
        val rendered = PcmAudio(8000, 1, FloatArray(800) { i -> (0.2 * kotlin.math.sin(2.0 * Math.PI * 440.0 * i / 8000.0)).toFloat() })
        val report = RenderQualityAnalyzer.analyze(source, rendered)
        assertTrue(report.finiteSamples)
        assertEquals(0.2, report.peak, 1e-6)
        assertEquals(0.0, report.durationDifferenceMs, 1e-6)
        assertFalse(report.hasWarnings)
    }

    @Test fun detectsClippingAndLargeDiscontinuity() {
        val source = PcmAudio(8000, 1, FloatArray(4))
        val rendered = PcmAudio(8000, 1, floatArrayOf(0f, 0f, 1f, -1f))
        val report = RenderQualityAnalyzer.analyze(source, rendered)
        assertEquals(2, report.clippedSamples)
        assertTrue(report.discontinuities >= 1)
        assertTrue(report.hasWarnings)
    }
}
