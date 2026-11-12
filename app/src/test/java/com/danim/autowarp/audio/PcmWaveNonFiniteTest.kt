package com.danim.autowarp.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PcmWaveNonFiniteTest {
    @Test fun writeNonFiniteAndOutOfRangeSamples() {
        val input = PcmAudio(44100, 1, floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, -2f, 2f, 0.5f))
        val output = ByteArrayOutputStream()
        PcmWave.write(input, output)
        val decoded = PcmWave.read(ByteArrayInputStream(output.toByteArray()))
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, -1f, 32767f / 32768f, 0.5f), decoded.samples, 0.00001f)
    }
}
