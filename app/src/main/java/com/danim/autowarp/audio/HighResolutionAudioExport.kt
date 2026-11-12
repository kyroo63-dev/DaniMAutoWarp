package com.danim.autowarp.audio

import java.io.File
import java.io.OutputStream
import kotlin.math.roundToInt

/** Converts the canonical file-backed PCM16 render to higher-container bit depths.
 * 24/32-bit output does NOT recover detail lost by the 16-bit intermediate.
 */
object HighResolutionAudioExport {
    enum class Format(val label: String, val mime: String, val extension: String) {
        WAV16("WAV PCM 16-bit", "audio/wav", "wav"),
        WAV24("WAV PCM 24-bit", "audio/wav", "wav"),
        WAV32_FLOAT("WAV float 32-bit", "audio/wav", "wav"),
        AIFF16("AIFF PCM 16-bit", "audio/aiff", "aiff"),
        AIFF24("AIFF PCM 24-bit", "audio/aiff", "aiff"),
        FLAC16("FLAC lossless PCM16", "audio/flac", "flac")
    }

    fun convert(input: File, output: OutputStream, format: Format) {
        if (format == Format.FLAC16) { FlacPcm16Export.encode(input, output); return }
        Pcm16WavFileSource(input).use { source ->
            val channels = source.channels
            val rate = source.sampleRate
            val frames = source.frameCount
            val bits = when (format) {
                Format.WAV16, Format.AIFF16, Format.FLAC16 -> 16
                Format.WAV24, Format.AIFF24 -> 24
                Format.WAV32_FLOAT -> 32
            }
            // Check the full multiplication before writing a container header.
            // A wrapped length could otherwise produce a valid-looking but truncated file.
            val bytes = try {
                Math.multiplyExact(Math.multiplyExact(frames, channels.toLong()), (bits / 8).toLong())
            } catch (_: ArithmeticException) {
                throw IllegalArgumentException("Audio demasiado largo para exportar")
            }
            // RIFF and IFF chunks must be aligned to even byte boundaries.
            val padBytes = (bytes and 1L).toInt()
            require(bytes <= 0xFFFF_FFFFL - 64L) { "Archivo demasiado grande para WAV/AIFF clásico" }
            val aiff = format == Format.AIFF16 || format == Format.AIFF24
            fun le16(n: Int) { output.write(n and 255); output.write((n ushr 8) and 255) }
            fun le32(n: Long) { repeat(4) { output.write((n ushr (8 * it)).toInt() and 255) } }
            fun be16(n: Int) { output.write((n ushr 8) and 255); output.write(n and 255) }
            fun be32(n: Long) { repeat(4) { output.write((n ushr (24 - 8 * it)).toInt() and 255) } }
            if (aiff) {
                output.write("FORM".toByteArray(Charsets.US_ASCII)); be32(46 + bytes + padBytes)
                output.write("AIFFCOMM".toByteArray(Charsets.US_ASCII)); be32(18)
                be16(channels); be32(frames.toLong()); be16(bits)
                // IEEE 80-bit extended positive integer sample rate.
                val exponent = 63 - java.lang.Long.numberOfLeadingZeros(rate.toLong())
                val mantissa = rate.toLong() shl (63 - exponent)
                be16(16383 + exponent)
                repeat(8) { output.write((mantissa ushr (56 - 8 * it)).toInt() and 255) }
                output.write("SSND".toByteArray(Charsets.US_ASCII)); be32(bytes + 8)
                be32(0); be32(0)
            } else {
                output.write("RIFF".toByteArray(Charsets.US_ASCII)); le32(bytes + 36 + padBytes)
                output.write("WAVEfmt ".toByteArray(Charsets.US_ASCII)); le32(16)
                le16(if (format == Format.WAV32_FLOAT) 3 else 1)
                le16(channels); le32(rate.toLong()); le32(rate.toLong() * channels * bits / 8)
                le16(channels * bits / 8); le16(bits)
                output.write("data".toByteArray(Charsets.US_ASCII)); le32(bytes)
            }
            var pos = 0L
            while (pos < frames) {
                val count = minOf(4096L, frames - pos).toInt()
                val samples = source.readFrames(pos, count)
                check(samples.size == count * channels) {
                    "El archivo intermedio contiene menos muestras de las declaradas"
                }
                for (raw in samples) {
                    // A corrupt intermediate must never produce NaN/Infinity in exported audio.
                    val s = if (raw.isFinite()) raw.coerceIn(-1f, 1f) else 0f
                    val sample16 = (s.coerceIn(-1f, 0.9999695f) * 32768f).roundToInt().coerceIn(-32768,32767)
                    when (format) {
                        Format.WAV16, Format.FLAC16 -> le16(sample16)
                        Format.AIFF16 -> be16(sample16)
                        Format.WAV24 -> { val n = sample16 shl 8; output.write(n and 255); output.write((n ushr 8) and 255); output.write((n ushr 16) and 255) }
                        Format.AIFF24 -> { val n = sample16 shl 8; output.write((n ushr 16) and 255); output.write((n ushr 8) and 255); output.write(n and 255) }
                        Format.WAV32_FLOAT -> le32(java.lang.Float.floatToRawIntBits(s).toLong())
                    }
                }
                pos += count
            }
            if (padBytes != 0) output.write(0)
        }
    }
}
