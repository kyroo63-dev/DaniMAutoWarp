package com.danim.autowarp.audio

import com.danim.autowarp.analysis.WarpMarker
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.math.*

/** Integration regression: render stereo warp, export WAV, import WAV again. */
class WarpPipelineIntegrationTest {
    @Test fun stereoWarpExportImportPreservesDurationAndChannels() {
        val rate = 8000
        val frames = rate * 2
        val samples = FloatArray(frames * 2) { index ->
            val frame = index / 2
            val kick = if (frame % 4000 < 70) (1.0 - frame % 4000 / 70.0) * 0.7 else 0.0
            val tone = sin(2.0 * PI * 220.0 * frame / rate) * 0.12
            (kick + if (index % 2 == 0) tone else -tone).toFloat()
        }
        val input = PcmAudio(rate, 2, samples)
        val anchors = listOf(WarpMarker(0.0, 0.0), WarpMarker(0.9, 1.0), WarpMarker(2.0, 2.2))
        val warped = WsolaWarp.render(input, anchors, quality = WarpQuality.FAST)
        assertEquals(rate, warped.sampleRate)
        assertEquals(2, warped.channels)
        assertEquals(17600, warped.frames)
        assertTrue(warped.samples.all { it.isFinite() && abs(it) <= 1.0f })
        val wav = ByteArrayOutputStream().also { PcmWave.write(warped, it) }
        val imported = PcmWave.read(wav.toByteArray().inputStream())
        assertEquals(warped.frames, imported.frames)
        assertEquals(2, imported.channels)
        assertTrue(imported.samples.any { abs(it) > 0.01f })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonIncreasingWarpTargets() {
        val input = PcmAudio(8000, 1, FloatArray(16000))
        WsolaWarp.render(input, listOf(WarpMarker(0.0, 0.0), WarpMarker(1.0, 0.0)))
    }
}
