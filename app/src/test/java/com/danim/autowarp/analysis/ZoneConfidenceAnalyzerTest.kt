package com.danim.autowarp.analysis

import org.junit.Assert.*
import org.junit.Test

class ZoneConfidenceAnalyzerTest {
    @Test fun goodKickEvidenceScoresHigherThanMissingKicks() {
        val beats = (0 until 64).map { it * 0.5 }
        val kicks = beats.filter { it < 16.0 }.map { TransientEvent(it, TransientKind.KICK, 0.95) }
        val zones = ZoneConfidenceAnalyzer.analyze(32.0, beats, kicks)
        assertEquals(2, zones.size)
        assertTrue(zones[0].score > 0.9)
        assertFalse(zones[0].requiresReview)
        assertTrue(zones[1].requiresReview)
        assertEquals(0, zones[1].supportedBeats)
    }

    @Test fun rejectsInvalidDurationAndDoesNotDoubleCountBoundary() {
        assertTrue(ZoneConfidenceAnalyzer.analyze(Double.NaN, emptyList(), emptyList()).isEmpty())
        val beats = listOf(15.5, 16.0)
        val events = beats.map { TransientEvent(it, TransientKind.KICK, 0.8) }
        val zones = ZoneConfidenceAnalyzer.analyze(32.0, beats, events)
        assertEquals(1, zones[0].totalBeats)
        assertEquals(1, zones[1].totalBeats)
    }
}
