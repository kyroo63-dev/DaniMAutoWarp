package com.danim.autowarp.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class AbPreviewRangeTest {
    private val markers = listOf(WarpMarker(10.0, 10.0), WarpMarker(30.0, 30.0))
    @Test fun clampsStartWithinTimeline() {
        assertEquals(10.0, AbPreviewRanges.select(markers, 0.0).startSeconds, 0.000001)
        assertEquals(22.0, AbPreviewRanges.select(markers, 100.0).startSeconds, 0.000001)
    }
    @Test fun shortTimelineUsesAvailableDuration() {
        val short = listOf(WarpMarker(5.0, 5.0), WarpMarker(8.0, 8.0))
        val range = AbPreviewRanges.select(short, 7.0)
        assertEquals(5.0, range.startSeconds, 0.000001)
        assertEquals(3.0, range.durationSeconds, 0.000001)
    }
}
