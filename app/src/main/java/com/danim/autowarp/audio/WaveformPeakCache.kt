package com.danim.autowarp.audio

import java.io.File
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs
import kotlin.math.max

/** File-backed waveform envelope. A bounded number of peak pairs is retained. */
data class WaveformPeaks(val framesPerBucket: Int, val peaks: FloatArray)

object WaveformPeakCache {
    fun build(source: PcmFrameSource, maxBuckets: Int = 32_768): WaveformPeaks {
        require(maxBuckets in 256..1_000_000)
        require(source.frameCount >= 0L) { "Número de fotogramas negativo" }
        require(source.channels > 0 && source.sampleRate > 0) { "Formato PCM no válido" }
        // Overflow-safe ceiling division, including very long recordings.
        val bucketSizeLong = source.frameCount / maxBuckets +
            if (source.frameCount % maxBuckets == 0L) 0L else 1L
        require(bucketSizeLong <= Int.MAX_VALUE) { "Audio demasiado largo para el tamaño de caché" }
        val bucketSize = bucketSizeLong.coerceAtLeast(1L).toInt()
        val count = (source.frameCount / bucketSize +
            if (source.frameCount % bucketSize == 0L) 0L else 1L).toInt()
        val peaks = FloatArray(count)
        var first = 0L
        var bucket = 0
        while (first < source.frameCount) {
            val end = minOf(source.frameCount, first + bucketSize)
            var peak = 0f
            var pos = first
            while (pos < end) {
                val n = minOf(4096L, end - pos).toInt()
                val samples = source.readFrames(pos, n)
                for (sample in samples) peak = max(peak, abs(sample))
                pos += n
            }
            peaks[bucket++] = peak
            first = end
        }
        return WaveformPeaks(bucketSize, peaks)
    }

    fun save(file: File, waveform: WaveformPeaks) {
        file.outputStream().buffered().use { stream ->
            DataOutputStream(stream).use { out ->
                out.writeInt(0x444D5750) // DMWP
                out.writeInt(1)
                out.writeInt(waveform.framesPerBucket)
                out.writeInt(waveform.peaks.size)
                waveform.peaks.forEach(out::writeFloat)
            }
        }
    }

    fun load(file: File): WaveformPeaks = DataInputStream(file.inputStream().buffered()).use { input ->
        require(input.readInt() == 0x444D5750 && input.readInt() == 1) { "Caché de forma de onda no válida" }
        val bucketSize = input.readInt()
        val count = input.readInt()
        require(bucketSize > 0 && count in 0..1_000_000)
        WaveformPeaks(bucketSize, FloatArray(count) {
            input.readFloat().also { peak -> require(peak.isFinite() && peak >= 0f) { "Pico de audio no válido" } }
        })
    }
}
