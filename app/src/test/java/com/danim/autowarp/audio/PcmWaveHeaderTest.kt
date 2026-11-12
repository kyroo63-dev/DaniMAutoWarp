package com.danim.autowarp.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class PcmWaveHeaderTest {
    private fun wav(): ByteArray = ByteArrayOutputStream().also {
        PcmWave.write(PcmAudio(44100, 2, floatArrayOf(0f, 0.5f, -0.5f, 0f)), it)
    }.toByteArray()

    @Test fun validStereoRoundTrip() {
        val audio = PcmWave.read(wav().inputStream())
        assertEquals(2, audio.channels)
        assertEquals(2, audio.frames)
    }

    @Test fun rejectsIncorrectBlockAlignment() {
        val bytes = wav()
        bytes[32] = 2
        assertThrows(IllegalArgumentException::class.java) { PcmWave.read(bytes.inputStream()) }
    }

    @Test fun rejectsIncorrectByteRate() {
        val bytes = wav()
        bytes[28] = 0
        bytes[29] = 0
        bytes[30] = 0
        bytes[31] = 0
        assertThrows(IllegalArgumentException::class.java) { PcmWave.read(bytes.inputStream()) }
    }
}
