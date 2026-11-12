package com.danim.autowarp.analysis

import com.danim.autowarp.audio.PcmAudio
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class BeatAnalyzerTest {
    @Test fun syntheticFourOnFloorProducesBeatMarkers() {
        val sr=12000;val seconds=12;val samples=FloatArray(sr*seconds)
        // Synthetic low-frequency kick every 500 ms, with a short decaying body.
        for(beat in 0 until seconds*2) {
            val start=(beat*0.5*sr).toInt()
            for(n in 0 until minOf(sr/8,samples.size-start)) {
                val env=kotlin.math.exp(-n.toDouble()/(sr*0.035)).toFloat()
                samples[start+n]+=(0.8*env*sin(2*PI*55*n/sr)).toFloat()
            }
        }
        val result=BeatAnalyzer.analyze(PcmAudio(sr,1,samples))
        assertTrue("Expected several beat markers",result.markers.size>=10)
        assertTrue("BPM should be close to 120",result.bpm in 110.0..130.0)
        assertTrue(result.kickTimes.isNotEmpty())
    }

    @Test fun gradualVinylSpeedDriftAppearsInTempoCurve() {
        val sr = 12000
        val starts = ArrayList<Double>()
        var time = 0.2
        var beat = 0
        while (time < 15.0) {
            starts.add(time)
            time += 0.49 + beat * 0.0015 // gradual slowdown, not a fixed metronome
            beat++
        }
        val samples = FloatArray(sr * 16)
        for (seconds in starts) {
            val start = (seconds * sr).toInt()
            for (n in 0 until minOf(sr / 8, samples.size - start)) {
                val env = kotlin.math.exp(-n.toDouble() / (sr * 0.035)).toFloat()
                samples[start + n] += (0.8 * env * sin(2 * PI * 55 * n / sr)).toFloat()
            }
        }
        val result = BeatAnalyzer.analyze(PcmAudio(sr, 1, samples))
        assertTrue(result.tempoCurve.size >= 10)
        assertTrue("Local tempo range should show the vinyl drift", result.tempoMaxBpm - result.tempoMinBpm > 4.0)
        assertTrue("Phase drift should be measurable", result.microTimingMs.any { kotlin.math.abs(it) > 20.0 })
        assertEquals(16.0, result.markers.last().targetSeconds, 0.002)
        assertEquals(16.0, result.markers.last().sourceSeconds, 0.002)
        assertTrue("Four-beat bar tempo curve should be populated", result.barTempoCurve.size >= 5)
    }

    @Test fun barPhaseFindsStrongestBeatOneAndFlagsAmbiguousFourOnFloor() {
        val beatTimes = (0 until 32).map { it * 0.5 }
        val accented = beatTimes.mapIndexed { index, t ->
            TransientEvent(t, TransientKind.KICK, if (index % 4 == 2) 1.0 else 0.35)
        }
        val estimate = BarAlignmentAnalyzer.estimate(beatTimes, accented)
        assertEquals(2, estimate.downbeatBeatIndex)
        assertTrue("Accent pattern should have useful confidence", estimate.confidence > 0.25)

        val flat = beatTimes.map { TransientEvent(it, TransientKind.KICK, 0.7) }
        val ambiguous = BarAlignmentAnalyzer.estimate(beatTimes, flat)
        assertTrue("Uniform four-on-floor must not claim a confident downbeat", ambiguous.confidence < 0.01)
    }

}
