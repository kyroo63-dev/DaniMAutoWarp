package com.danim.autowarp.audio

import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import kotlin.math.pow

/** Disk-backed RIFF/WAVE and uncompressed AIFF PCM converter; bounded input/output buffers. */
object StreamingPcmImporter {
    fun convert(input: InputStream, output: File) {
        val source = File.createTempFile("danim-pcm-source-", ".bin", output.parentFile)
        try {
            input.use { stream -> source.outputStream().buffered().use { stream.copyTo(it, 65536) } }
            RandomAccessFile(source, "r").use { src ->
                require(src.length() >= 12) { "Archivo PCM demasiado corto" }
                val header = ByteArray(12); src.readFully(header)
                val tag = String(header, 0, 4, Charsets.US_ASCII)
                val format = String(header, 8, 4, Charsets.US_ASCII)
                val wav = tag == "RIFF" && format == "WAVE"
                require(wav || (tag == "FORM" && format == "AIFF")) { "Solo WAV PCM o AIFF sin compresión" }
                fun u16(b: ByteArray, p: Int, le: Boolean): Int = if (le) (b[p].toInt() and 255) or ((b[p+1].toInt() and 255) shl 8) else ((b[p].toInt() and 255) shl 8) or (b[p+1].toInt() and 255)
                fun u32(b: ByteArray, p: Int, le: Boolean): Long {
                    var n = 0L
                    if (le) for (i in 3 downTo 0) n = (n shl 8) or (b[p+i].toLong() and 255)
                    else for (i in 0..3) n = (n shl 8) or (b[p+i].toLong() and 255)
                    return n
                }
                var channels = 0; var bits = 0; var rate = 0; var audioFormat = 1
                var dataOffset = -1L; var dataLength = 0L; var declaredFrames = Long.MAX_VALUE
                var pos = 12L
                while (pos + 8 <= src.length()) {
                    src.seek(pos); val chunkHeader = ByteArray(8); src.readFully(chunkHeader)
                    val id = String(chunkHeader, 0, 4, Charsets.US_ASCII)
                    val length = u32(chunkHeader, 4, wav)
                    val start = pos + 8
                    require(length <= src.length() - start) { "Chunk PCM truncado: $id" }
                    if (wav && id == "fmt " && length >= 16) {
                        val fmt = ByteArray(16); src.readFully(fmt)
                        audioFormat = u16(fmt, 0, true); channels = u16(fmt, 2, true)
                        rate = u32(fmt, 4, true).toInt(); bits = u16(fmt, 14, true)
                    } else if (!wav && id == "COMM" && length >= 18) {
                        val fmt = ByteArray(18); src.readFully(fmt)
                        channels = u16(fmt, 0, false); declaredFrames = u32(fmt, 2, false)
                        bits = u16(fmt, 6, false)
                        val exp = u16(fmt, 8, false)
                        var mant = 0.0
                        for (i in 10..17) mant = mant * 256.0 + (fmt[i].toInt() and 255)
                        rate = if (exp in 1..0x7ffe) (mant * 2.0.pow((exp - 16383 - 63).toDouble())).toInt() else 0
                    } else if (wav && id == "data" && dataOffset < 0) {
                        dataOffset = start; dataLength = length
                    } else if (!wav && id == "SSND" && length >= 8 && dataOffset < 0) {
                        val ssnd = ByteArray(8); src.readFully(ssnd)
                        val offset = u32(ssnd, 0, false)
                        require(offset <= length - 8) { "SSND offset inválido" }
                        dataOffset = start + 8 + offset; dataLength = length - 8 - offset
                    }
                    pos = start + length + (length and 1L)
                }
                require(audioFormat == 1 && channels in 1..2 && rate in 8000..384000 && bits in setOf(8,16,24,32) && dataOffset >= 0) { "Formato PCM no admitido" }
                val bytesPer = bits / 8
                val frameSize = bytesPer * channels
                val frames = minOf(dataLength / frameSize, declaredFrames)
                require(frames > 0 && frames <= (0xFFFF_FFFFL - 36) / (channels * 2)) { "Audio vacío o WAV de salida mayor de 4 GB" }
                val totalBytes = frames * channels * 2
                RandomAccessFile(output, "rw").use { dest ->
                    dest.setLength(0)
                    fun ascii(s: String) = dest.write(s.toByteArray(Charsets.US_ASCII))
                    fun w16(n: Int) { dest.write(n and 255); dest.write((n ushr 8) and 255) }
                    fun w32(n: Long) { w16(n.toInt()); w16((n ushr 16).toInt()) }
                    ascii("RIFF"); w32(totalBytes + 36); ascii("WAVEfmt "); w32(16)
                    w16(1); w16(channels); w32(rate.toLong()); w32(rate.toLong() * channels * 2)
                    w16(channels * 2); w16(16); ascii("data"); w32(totalBytes)
                    src.seek(dataOffset)
                    val blockFrames = 4096
                    val inputBytes = ByteArray(blockFrames * frameSize)
                    val outputBytes = ByteArray(blockFrames * channels * 2)
                    var remaining = frames
                    while (remaining > 0) {
                        val take = minOf(remaining, blockFrames.toLong()).toInt()
                        src.readFully(inputBytes, 0, take * frameSize)
                        for (i in 0 until take * channels) {
                            val offset = i * bytesPer
                            var value = 0L
                            if (wav) for (j in bytesPer - 1 downTo 0) value = (value shl 8) or (inputBytes[offset+j].toLong() and 255)
                            else for (j in 0 until bytesPer) value = (value shl 8) or (inputBytes[offset+j].toLong() and 255)
                            val signed = (value shl (64 - bits)) shr (64 - bits)
                            val pcm = if (wav && bits == 8) ((value.toInt() - 128) shl 8) else (if (bits >= 16) signed shr (bits - 16) else signed shl (16 - bits)).toInt().coerceIn(-32768, 32767)
                            outputBytes[i*2] = pcm.toByte(); outputBytes[i*2+1] = (pcm shr 8).toByte()
                        }
                        dest.write(outputBytes, 0, take * channels * 2)
                        remaining -= take
                    }
                }
            }
        } catch (e: Exception) { output.delete(); throw e }
        finally { source.delete() }
    }
}
