package com.danim.autowarp.audio

import com.danim.autowarp.analysis.TransientEvent
import com.danim.autowarp.analysis.TransientKind
import com.danim.autowarp.analysis.WarpMarker
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class StreamingWsolaProcessorTest {
    private class SineSource(
        override val sampleRate: Int = 8_000,
        override val channels: Int = 2,
        override val frameCount: Long = 32_000
    ) : PcmFrameSource {
        override fun readFrames(firstFrame: Long, count: Int): FloatArray {
            require(firstFrame >= 0 && firstFrame + count <= frameCount)
            return FloatArray(count * channels) { i ->
                val frame = firstFrame + i / channels
                (sin(2.0 * PI * 440.0 * frame / sampleRate) * 0.4).toFloat()
            }
        }
    }

    @Test fun streamsPitchPreservingWavWithTransientAnchors() {
        val output = ByteArrayOutputStream()
        val report = StreamingWsolaProcessor.renderWav(
            SineSource(),
            listOf(WarpMarker(0.0, 0.0), WarpMarker(4.0, 3.8)),
            output,
            WarpQuality.FAST,
            listOf(
                TransientEvent(1.0, TransientKind.KICK, 0.95),
                TransientEvent(2.0, TransientKind.SNARE, 0.90)
            )
        )
        val bytes = output.toByteArray()
        assertEquals(30_400L, report.outputFrames)
        assertTrue(report.pitchPreserving)
        assertTrue(report.lockedTransientAnchors > 0)
        assertTrue(report.maxSourceFramesBuffered < 1_500)
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals(30_400 * 4, bytes.size - 44)
    }

    @Test fun rendersOnlyRequestedPreviewWindow() {
        val output = ByteArrayOutputStream()
        val report = StreamingWsolaProcessor.renderWav(
            SineSource(), listOf(WarpMarker(0.0, 0.0), WarpMarker(4.0, 3.8)),
            output, WarpQuality.FAST, previewStartSeconds = 1.5, previewDurationSeconds = 0.75
        )
        assertEquals(6_000L, report.outputFrames)
        assertEquals(44 + 6_000 * 4, output.size())
        assertEquals("RIFF", String(output.toByteArray(), 0, 4, Charsets.US_ASCII))
    }

    @Test fun rejectsInvalidPreviewWindow() {
        try {
            StreamingWsolaProcessor.renderWav(
                SineSource(), listOf(WarpMarker(0.0, 0.0), WarpMarker(4.0, 3.8)),
                ByteArrayOutputStream(), previewStartSeconds = Double.NaN, previewDurationSeconds = 1.0
            )
            fail("Expected validation error")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun fileBackedPcm16SourceSupportsSmallRandomWindows() {
        val file = File.createTempFile("danim-stream-test", ".wav")
        try {
            file.outputStream().use { PcmWave.write(PcmAudio(8_000, 1, FloatArray(800) { 0.25f }), it) }
            Pcm16WavFileSource(file).use { source ->
                assertEquals(800L, source.frameCount)
                assertEquals(12, source.readFrames(100, 12).size)
                assertEquals(0.25f, source.readFrames(100, 1)[0], 0.001f)
            }
        } finally { file.delete() }
    }
}
