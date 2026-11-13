package com.danim.autowarp.audio

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class WaveformPeakCacheTest {
    private fun source(values: FloatArray) = object : PcmFrameSource {
        override val sampleRate = 44100
        override val channels = 1
        override val frameCount = values.size.toLong()
        override fun readFrames(firstFrame: Long, count: Int): FloatArray =
            values.copyOfRange(firstFrame.toInt(), firstFrame.toInt() + count)
    }

    @Test fun retainsPeakAcrossBucketBoundaries() {
        val values = FloatArray(512)
        values[0] = -0.9f
        values[3] = 0.7f
        values[511] = -0.6f
        val result = WaveformPeakCache.build(source(values), 256)
        assertEquals(2, result.framesPerBucket)
        assertEquals(256, result.peaks.size)
        assertEquals(0.9f, result.peaks[0], 0.00001f)
        assertEquals(0.7f, result.peaks[1], 0.00001f)
        assertEquals(0.6f, result.peaks[255], 0.00001f)
    }

    @Test fun emptyAudioReturnsEmptyPeaks() {
        assertEquals(0, WaveformPeakCache.build(source(floatArrayOf())).peaks.size)
    }

    @Test fun savesAndLoadsWaveform() {
        val file = File.createTempFile("danim-waveform", ".bin")
        try {
            val original = WaveformPeakCache.build(source(floatArrayOf(0.1f, -0.8f, 0.2f)))
            WaveformPeakCache.save(file, original)
            val loaded = WaveformPeakCache.load(file)
            assertEquals(original.framesPerBucket, loaded.framesPerBucket)
            assertArrayEquals(original.peaks, loaded.peaks, 0.00001f)
        } finally { file.delete() }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeFrameCount() {
        val invalid = object : PcmFrameSource {
            override val sampleRate = 44100
            override val channels = 1
            override val frameCount = -1L
            override fun readFrames(firstFrame: Long, count: Int) = floatArrayOf()
        }
        WaveformPeakCache.build(invalid)
    }
}
