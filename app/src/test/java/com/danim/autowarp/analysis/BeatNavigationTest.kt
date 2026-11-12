package com.danim.autowarp.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class BeatNavigationTest {
    @Test fun jumpsByBeatAndBar() {
        assertEquals(0.5, BeatNavigation.shift(0.0, 120.0, 1, 100.0), 1e-8)
        assertEquals(2.0, BeatNavigation.shift(0.0, 120.0, 4, 100.0), 1e-8)
        assertEquals(0.0, BeatNavigation.shift(0.0, 120.0, -4, 100.0), 1e-8)
        assertEquals(8.0, BeatNavigation.shift(7.0, 120.0, 4, 8.0), 1e-8)
    }
}
