package com.danim.autowarp.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FlacPcm16ExportTest {
    @Test fun exportsFlacHeaderAndStreamInfo() {
        val wav = File.createTempFile("danim-flac-test", ".wav")
        val flac = File.createTempFile("danim-flac-test", ".flac")
        try {
            wav.outputStream().use { PcmWave.write(PcmAudio(44100, 2, FloatArray(8192)), it) }
            flac.outputStream().use { FlacPcm16Export.encode(wav, it) }
            val bytes = flac.readBytes()
            assertArrayEquals("fLaC".toByteArray(), bytes.copyOfRange(0, 4))
            assertEquals(0x80, bytes[4].toInt() and 255)
            assertEquals(34, bytes[7].toInt() and 255)
        } finally { wav.delete(); flac.delete() }
    }
}
