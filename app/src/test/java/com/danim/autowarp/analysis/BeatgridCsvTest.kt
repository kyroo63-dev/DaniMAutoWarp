package com.danim.autowarp.analysis

import org.junit.Assert.*
import org.junit.Test

class BeatgridCsvTest {
    @Test fun exportsBeatsAndClassifiedTransients() {
        val result = AnalysisResult(120.0, listOf(0.0, 0.5, 1.0, 1.5), emptyList(), 0.9)
        val csv = BeatgridCsv.encode(result, listOf(TransientEvent(0.12, TransientKind.HI_HAT, 0.8)))
        assertTrue(csv.contains("beat,1,0.000000"))
        assertTrue(csv.contains("beat,4,1.500000"))
        assertTrue(csv.lineSequence().any { it == "transient,1,0.120000,,,,HI_HAT,0.8000" })
    }
}
