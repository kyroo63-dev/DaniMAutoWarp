package com.danim.autowarp.audio

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.sqrt

/** Inspección de WAV PCM16 sin cargar el tema completo en memoria. */
data class StreamingRenderAuditReport(
    val frames: Long,
    val sampleRate: Int,
    val channels: Int,
    val peak: Double,
    val rms: Double,
    val dcOffset: Double,
    val clippedSamples: Long,
    val discontinuities: Long,
    val durationDifferenceMs: Double?,
    val headerConsistent: Boolean
) {
    val hasWarnings: Boolean get() = !headerConsistent || clippedSamples > 0 || discontinuities > 0 || abs(dcOffset) > 0.02 ||
        (durationDifferenceMs != null && durationDifferenceMs > 2.0)
}

object StreamingRenderAudit {
    fun inspect(file: File, expectedFrames: Long? = null, jumpThreshold: Double = 0.35): StreamingRenderAuditReport {
        require(jumpThreshold > 0 && jumpThreshold.isFinite())
        RandomAccessFile(file, "r").use { raf ->
            require(raf.length() >= 44) { "WAV incompleto" }
            fun ascii(n: Int) = ByteArray(n).also { raf.readFully(it) }.toString(Charsets.US_ASCII)
            fun u16() = raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8)
            fun u32(): Long = u16().toLong() or (u16().toLong() shl 16)
            require(ascii(4) == "RIFF") { "No es RIFF" }
            val riffSize = u32()
            require(ascii(4) == "WAVE") { "No es WAVE" }
            var rate = 0; var channels = 0; var blockAlign = 0
            var dataOffset = -1L; var dataBytes = -1L; var formatFound = false
            while (raf.filePointer + 8 <= raf.length()) {
                val id = ascii(4); val size = u32(); val start = raf.filePointer
                require(size <= raf.length() - start) { "Chunk WAV truncado: $id" }
                if (id == "fmt ") {
                    require(size >= 16)
                    val format = u16(); channels = u16(); rate = u32().toInt()
                    u32(); blockAlign = u16(); val bits = u16()
                    require(format == 1 && bits == 16 && channels in 1..2 && rate > 0 && blockAlign == channels * 2) {
                        "Solo se admite WAV PCM16 mono/estéreo"
                    }
                    formatFound = true
                } else if (id == "data" && dataOffset < 0) {
                    dataOffset = start; dataBytes = size
                }
                val next = start + size + (size and 1L)
                if (next > raf.length()) break
                raf.seek(next)
            }
            require(formatFound && dataOffset >= 0 && dataBytes >= 0 && dataBytes % blockAlign == 0L)
            val frames = dataBytes / blockAlign
            val headerOk = riffSize + 8 == raf.length() && dataOffset + dataBytes <= raf.length()
            raf.seek(dataOffset)
            val buffer = ByteArray(32768 - 32768 % blockAlign)
            val previous = DoubleArray(channels)
            var hasPrevious = false
            var remaining = dataBytes; var sum = 0.0; var squares = 0.0
            var peak = 0.0; var clipped = 0L; var jumps = 0L
            while (remaining > 0) {
                val take = minOf(buffer.size.toLong(), remaining).toInt()
                raf.readFully(buffer, 0, take)
                for (i in 0 until take step blockAlign) {
                    var frameJump = 0.0
                    for (ch in 0 until channels) {
                        val at = i + ch * 2
                        val value = ((buffer[at].toInt() and 255) or (buffer[at + 1].toInt() shl 8)).toShort().toDouble() / 32768.0
                        sum += value; squares += value * value
                        peak = maxOf(peak, abs(value))
                        if (abs(value) >= 0.999) clipped++
                        if (hasPrevious) frameJump = maxOf(frameJump, abs(value - previous[ch]))
                        previous[ch] = value
                    }
                    if (hasPrevious && frameJump > jumpThreshold) jumps++
                    hasPrevious = true
                }
                remaining -= take
            }
            val samples = frames * channels
            return StreamingRenderAuditReport(frames, rate, channels, peak,
                if (samples > 0) sqrt(squares / samples) else 0.0,
                if (samples > 0) sum / samples else 0.0, clipped, jumps,
                expectedFrames?.let { abs(frames - it).toDouble() * 1000.0 / rate }, headerOk)
        }
    }
}
