package com.danim.autowarp.analysis

import com.danim.autowarp.audio.PcmAudio
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

class VaryingTempoBeatAnalyzerTest {
    @Test fun kickTrackWithGradualVinylLikeDriftProducesMonotonicWarpMarkers() {
        val sr = 8000
        val samples = FloatArray(sr * 14)
        var t = 0.32
        var beat = 0
        while (t < 13.7) {
            val start = (t * sr).toInt()
            for (i in 0 until 600) {
                val index = start + i
                if (index >= samples.size) break
                samples[index] += (0.75 * exp(-i / 125.0) *
                    sin(2 * PI * (75.0 - 20.0 * i / 600.0) * i / sr)).toFloat()
            }
            if (beat % 2 == 1) for (i in 0 until 120) {
                val index = start + i
                if (index < samples.size) samples[index] +=
                    (0.09 * (1 - i / 120.0) * sin(2 * PI * 1900 * i / sr)).toFloat()
            }
            t += 0.5 + 0.014 * sin(beat * 0.29)
            beat++
        }
        val result = BeatAnalyzer.analyze(PcmAudio(sr, 1, samples))
        assertTrue("BPM ${result.bpm}", result.bpm in 100.0..140.0)
        assertTrue("Insufficient tracked beats", result.beatTimes.size >= 12)
        assertTrue("Non-monotonic warp anchors", result.markers.zipWithNext().all { (a, b) ->
            a.sourceSeconds < b.sourceSeconds && a.targetSeconds < b.targetSeconds
        })
        assertTrue(result.markers.all { it.sourceSeconds.isFinite() && it.targetSeconds.isFinite() })
    }
}
