package com.danim.autowarp.audio

import com.danim.autowarp.analysis.WarpMarker
import com.danim.autowarp.analysis.TransientEvent
import com.danim.autowarp.analysis.TransientKind
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.sin

class StreamingWarpProcessorTest {
    private class SineSource(override val sampleRate:Int=8000, override val channels:Int=2, override val frameCount:Long=sampleRate.toLong()*12):PcmFrameSource {
        override fun readFrames(firstFrame:Long,count:Int):FloatArray {
            val out=FloatArray(count*channels)
            for(i in 0 until count) { val v=sin(2.0*PI*440.0*(firstFrame+i)/sampleRate).toFloat()*0.4f; for(c in 0 until channels) out[i*channels+c]=v }
            return out
        }
    }
    @Test fun rendersLongTrackInBoundedBlocksAndKeepsStereo() {
        val src=SineSource()
        val bytes=ByteArrayOutputStream()
        val report=StreamingWarpProcessor.renderWav(src,listOf(WarpMarker(0.0,0.0),WarpMarker(12.0,12.0)),bytes,2048)
        assertEquals(96000L,report.outputFrames)
        assertEquals(47L,report.blocksWritten)
        assertTrue(report.maxSourceFramesBuffered < 5000)
        assertFalse(report.pitchPreserving)
        val wav=bytes.toByteArray()
        assertEquals("RIFF",String(wav,0,4))
        assertEquals("WAVE",String(wav,8,4))
        assertEquals(96000*2*2,wav.size-44)
    }
    @Test fun readsFileBackedPcm16WavWindows() {
        val temp = kotlin.io.path.createTempFile("danim-stream", ".wav").toFile()
        try {
            PcmWave.write(PcmAudio(8000, 1, FloatArray(800) { if (it % 2 == 0) 0.25f else -0.25f }), temp.outputStream())
            Pcm16WavFileSource(temp).use { source ->
                assertEquals(8000, source.sampleRate)
                assertEquals(800L, source.frameCount)
                val part = source.readFrames(100, 32)
                assertEquals(32, part.size)
                assertEquals(0.25f, part[0], 0.0001f)
            }
        } finally { temp.delete() }
    }


    @Test fun streamingWsolaWritesContinuousBoundedStereoWav() {
        val src = SineSource(sampleRate = 8000, channels = 2, frameCount = 32000)
        val bytes = ByteArrayOutputStream()
        val report = StreamingWsolaProcessor.renderWav(
            src,
            listOf(WarpMarker(0.0, 0.0), WarpMarker(4.0, 3.8)),
            bytes,
            WarpQuality.FAST
        )
        assertEquals(30400L, report.outputFrames)
        assertTrue(report.grainsProcessed > 10)
        assertEquals(report.grainsProcessed, report.blocksWritten)
        assertTrue(report.maxSourceFramesBuffered < 1500)
        assertTrue(report.pitchPreserving)
        val wav = bytes.toByteArray()
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals(30400 * 2 * 2, wav.size - 44)
        val pcm = java.nio.ByteBuffer.wrap(wav, 44, wav.size - 44).slice().order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val start = 4000; val end = 26400; var crossings = 0; var previous = pcm.getShort(start * 4).toInt()
        for (frame in start + 1 until end) {
            val current = pcm.getShort(frame * 4).toInt()
            if (previous <= 0 && current > 0) crossings++
            previous = current
        }
        val measuredHz = crossings * 8000.0 / (end - start)
        assertTrue("WSOLA debe mantener el tono de 440 Hz; medido=$measuredHz", measuredHz in 435.0..445.0)
    }

    @Test fun fileBackedPcm16SourceCanFeedStreamingWsolaExport() {
        val temp = kotlin.io.path.createTempFile("danim-wsola-source", ".wav").toFile()
        try {
            val samples = FloatArray(8000 * 2) { i ->
                val frame = i / 2
                (sin(2.0 * PI * 220.0 * frame / 8000.0) * 0.35).toFloat()
            }
            PcmWave.write(PcmAudio(8000, 2, samples), temp.outputStream())
            val output = ByteArrayOutputStream()
            Pcm16WavFileSource(temp).use { source ->
                val report = StreamingWsolaProcessor.renderWav(
                    source, listOf(WarpMarker(0.0, 0.0), WarpMarker(1.0, 0.95)),
                    output, WarpQuality.FAST
                )
                assertEquals(7600L, report.outputFrames)
                assertTrue(report.maxSourceFramesBuffered < 1500)
            }
            assertEquals(7600 * 2 * 2 + 44, output.size())
        } finally { temp.delete() }
    }

    @Test fun streamingWsolaLocksConfidentTransientAnchors() {
        val src = SineSource(sampleRate = 8000, channels = 2, frameCount = 32000)
        val bytes = ByteArrayOutputStream()
        val report = StreamingWsolaProcessor.renderWav(
            src,
            listOf(WarpMarker(0.0, 0.0), WarpMarker(4.0, 3.8)),
            bytes,
            WarpQuality.FAST,
            listOf(TransientEvent(1.0, TransientKind.KICK, 0.95), TransientEvent(2.0, TransientKind.SNARE, 0.9))
        )
        assertTrue("Debe fijar al menos un ataque detectado", report.lockedTransientAnchors > 0)
        assertTrue(report.maxSourceFramesBuffered < 1500)
        assertEquals(30400L, report.outputFrames)
        assertTrue(report.pitchPreserving)
    }

    @Test fun inMemoryFrameSourceReturnsOnlyRequestedWindow() {
        val audio = PcmAudio(8000, 2, FloatArray(200) { it / 200f })
        val source = PcmAudioFrameSource(audio)
        val window = source.readFrames(20, 12)
        assertEquals(24, window.size)
        assertEquals(audio.samples[40], window[0], 0.000001f)
    }

    @Test fun rejectsNonMonotonicMarkers() {
        val src=SineSource()
        try {
            StreamingWarpProcessor.renderWav(src,listOf(WarpMarker(0.0,0.0),WarpMarker(5.0,4.0),WarpMarker(4.0,8.0)),ByteArrayOutputStream())
            fail("Expected invalid marker rejection")
        } catch(_:IllegalArgumentException) { }
    }
}
