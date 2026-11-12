package com.danim.autowarp.analysis

import org.junit.Assert.*
import org.junit.Test

class WarpZoneToolsTest {
    private val markers = listOf(
        WarpMarker(0.0, 0.0, 1f), WarpMarker(1.0, 0.9, 1f),
        WarpMarker(2.0, 1.8, 1f), WarpMarker(3.0, 3.0, 1f)
    )
    @Test fun markerStrengthIsClampedAndEndpointsStayAnchored() {
        val changed = WarpZoneTools.setMarkerStrength(markers, 1, 1.5f)
        assertEquals(1f, changed[1].strength)
        assertEquals(1f, changed.first().strength)
        assertEquals(1f, changed.last().strength)
        assertEquals(0f, WarpZoneTools.setMarkerStrength(markers, 2, -1f)[2].strength)
    }
    @Test fun zoneStrengthChangesOnlySelectedInteriorMarkers() {
        val changed = WarpZoneTools.setZoneStrength(markers, 1, 2, 0.35f)
        assertEquals(1f, changed.first().strength)
        assertEquals(0.35f, changed[1].strength, 0.0001f)
        assertEquals(0.35f, changed[2].strength, 0.0001f)
        assertEquals(1f, changed.last().strength)
    }
}
