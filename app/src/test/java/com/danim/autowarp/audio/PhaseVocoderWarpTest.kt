package com.danim.autowarp.audio

import com.danim.autowarp.analysis.TransientEvent
import com.danim.autowarp.analysis.TransientKind
import com.danim.autowarp.analysis.WarpMarker
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PhaseVocoderWarpTest {
    @Test fun preservesRequestedDurationAndFiniteSamples() {
        val sr=12000
        val samples=FloatArray(sr*3) { i -> (0.35*sin(2*PI*440*i/sr)).toFloat() }
        val input=PcmAudio(sr,1,samples)
        val markers=listOf(WarpMarker(0.0,0.0),WarpMarker(1.0,1.05),WarpMarker(2.0,2.1),WarpMarker(3.0,3.15))
        val out=PhaseVocoderWarp.render(input,markers,1024)
        assertEquals(3.15,out.durationSeconds,1.0/sr)
        assertTrue(out.samples.all { it.isFinite() })
        assertTrue(out.samples.maxOf { abs(it) } > 0.05f)
    }

    @Test fun linkedStereoKeepsIdenticalChannelsIdentical() {
        val sr=8000
        val samples=FloatArray(sr*2*2) { i ->
            val frame=i/2
            (0.3*sin(2*PI*330*frame/sr)+0.12*sin(2*PI*990*frame/sr)).toFloat()
        }
        val input=PcmAudio(sr,2,samples)
        val markers=listOf(WarpMarker(0.0,0.0),WarpMarker(1.0,1.04),WarpMarker(2.0,2.08))
        val events=listOf(TransientEvent(0.5,TransientKind.KICK,0.95),TransientEvent(1.0,TransientKind.SNARE,0.8))
        val out=PhaseVocoderWarp.render(input,markers,1024,events)
        assertTrue(out.samples.all { it.isFinite() })
        for(i in 0 until out.frames) assertEquals(out.samples[i*2],out.samples[i*2+1],1e-5f)
    }

    @Test fun rejectsNonMonotonicMarkers() {
        val input=PcmAudio(8000,1,FloatArray(8000))
        val markers=listOf(WarpMarker(0.0,0.0),WarpMarker(0.8,0.5),WarpMarker(0.7,1.0))
        assertThrows(IllegalArgumentException::class.java) { PhaseVocoderWarp.render(input,markers,512) }
    }
}
