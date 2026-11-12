package com.danim.autowarp.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PrecisionWarpEditorTest {
    private val anchors = listOf(WarpMarker(0.0, 0.0), WarpMarker(1.0, 1.1), WarpMarker(2.0, 2.0))

    @Test fun nudgeMovesOnlySelectedSource() {
        val edited = PrecisionWarpEditor.nudgeSource(anchors, 1, 10.0)
        assertEquals(1.01, edited[1].sourceSeconds, 1e-9)
        assertEquals(1.1, edited[1].targetSeconds, 1e-9)
        assertEquals(anchors[0], edited[0])
        assertEquals(anchors[2], edited[2])
    }
    @Test fun clampsToNeighbouringAnchor() {
        val edited = PrecisionWarpEditor.nudgeSource(anchors, 1, 5000.0)
        assertEquals(1.999, edited[1].sourceSeconds, 1e-9)
    }
    @Test fun endpointsAreProtected() {
        assertThrows(IllegalArgumentException::class.java) { PrecisionWarpEditor.nudgeSource(anchors, 0, 10.0) }
    }
}
