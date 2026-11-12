package com.danim.autowarp.audio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class HighResolutionAudioExportAlignmentTest {
    @Test fun oddSizedMonoPcm24UsesEvenChunkPadding() {
        val input = File.createTempFile("danim-source-", ".wav")
        try {
            input.outputStream().use { PcmWave.write(PcmAudio(44100, 1, floatArrayOf(0.5f)), it) }
            for (format in listOf(HighResolutionAudioExport.Format.WAV24, HighResolutionAudioExport.Format.AIFF24)) {
                val output = java.io.ByteArrayOutputStream()
                HighResolutionAudioExport.convert(input, output, format)
                val b = output.toByteArray()
                assertEquals(0, b.size % 2)
                val declared = if (format == HighResolutionAudioExport.Format.WAV24)
                    (b[4].toInt() and 255) or ((b[5].toInt() and 255) shl 8) or
                    ((b[6].toInt() and 255) shl 16) or ((b[7].toInt() and 255) shl 24)
                else ((b[4].toInt() and 255) shl 24) or ((b[5].toInt() and 255) shl 16) or
                    ((b[6].toInt() and 255) shl 8) or (b[7].toInt() and 255)
                assertEquals(b.size - 8, declared)
                assertEquals(0, b.last().toInt())
            }
        } finally { input.delete() }
    }
}
