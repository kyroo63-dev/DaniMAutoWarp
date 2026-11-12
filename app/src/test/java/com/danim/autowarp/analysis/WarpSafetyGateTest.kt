package com.danim.autowarp.analysis

import org.junit.Assert.*
import org.junit.Test

class WarpSafetyGateTest {
    @Test fun rejectsAbruptWarpAndKeepsEndpoints() {
        val input = listOf(WarpMarker(0.0, 0.0), WarpMarker(0.5, 0.5),
            WarpMarker(1.0, 2.0), WarpMarker(2.0, 2.0), WarpMarker(4.0, 4.0))
        val result = WarpSafetyGate.sanitize(input, 4.0)
        assertTrue(result.rejected > 0)
        assertEquals(0.0, result.markers.first().sourceSeconds, 0.0)
        assertEquals(4.0, result.markers.last().targetSeconds, 0.0)
        assertTrue(result.markers.zipWithNext().all { (a,b) -> b.sourceSeconds > a.sourceSeconds && b.targetSeconds > a.targetSeconds })
    }

    @Test fun preservesGradualDrift() {
        val input = listOf(WarpMarker(0.0,0.0), WarpMarker(1.0,1.02),
            WarpMarker(2.0,2.04), WarpMarker(3.0,3.03), WarpMarker(4.0,4.0))
        val result = WarpSafetyGate.sanitize(input,4.0)
        assertEquals(5,result.markers.size)
        assertEquals(0,result.rejected)
    }
}
