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
        val bucketSize = ((source.frameCount + maxBuckets - 1) / maxBuckets).coerceAtLeast(1).toInt()
        val count = ((source.frameCount + bucketSize - 1) / bucketSize).toInt()
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
        WaveformPeaks(bucketSize, FloatArray(count) { input.readFloat() })
    }
}
