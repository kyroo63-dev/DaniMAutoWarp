package com.danim.autowarp.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class AbTimelineTest {
    @Test fun mapsCorrectedToOriginal() {
        val markers = listOf(WarpMarker(0.0, 0.0), WarpMarker(6.0, 5.0), WarpMarker(10.0, 10.0))
        assertEquals(3.0, AbTimeline.sourceAt(2.5, markers), 1e-9)
        assertEquals(8.0, AbTimeline.sourceAt(7.5, markers), 1e-9)
        assertEquals(10.0, AbTimeline.sourceAt(15.0, markers), 1e-9)
    }
    @Test fun respectsMarkerStrength() {
        val markers = listOf(WarpMarker(0.0, 0.0, 0f), WarpMarker(6.0, 5.0, 0f))
        assertEquals(2.5, AbTimeline.sourceAt(2.5, markers), 1e-9)
    }
}
