package com.danim.autowarp.audio

import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

/** Standards-based FLAC stream, lossless PCM16, using fixed-order Rice coding or verbatim fallback.
 * Compression is lossless and uses bounded memory.
 */
object FlacPcm16Export {
    private const val BLOCK = 4096

    fun encode(wav: File, output: OutputStream) {
        Pcm16WavFileSource(wav).use { source ->
            require(source.channels in 1..2) { "FLAC admite mono o estéreo" }
            require(source.sampleRate in 1..655350) { "Frecuencia FLAC no admitida" }
            require(source.frameCount in 1..0xFFFFFFFFFL) { "Audio demasiado largo para FLAC" }
            val md5 = MessageDigest.getInstance("MD5")
            var offset = 0L
            while (offset < source.frameCount) {
                val count = minOf(BLOCK.toLong(), source.frameCount - offset).toInt()
                val samples = source.readFrames(offset, count)
                for (s in samples) {
                    val v = pcm16(s)
                    md5.update(v.toByte()); md5.update((v ushr 8).toByte())
                }
                offset += count
            }
            output.write("fLaC".toByteArray(Charsets.US_ASCII))
            output.write(byteArrayOf(0x80.toByte(), 0, 0, 34)) // final STREAMINFO block
            val info = ByteArray(34)
            info[0] = (BLOCK ushr 8).toByte(); info[1] = BLOCK.toByte()
            info[2] = info[0]; info[3] = info[1]
            // min/max frame sizes unknown (zero is allowed)
            val packed = (source.sampleRate.toLong() shl 44) or
                ((source.channels - 1).toLong() shl 41) or (15L shl 36) or source.frameCount
            for (i in 0..7) info[10 + i] = (packed ushr (56 - i * 8)).toByte()
            md5.digest().copyInto(info, 18)
            output.write(info)
            offset = 0L
            var frameIndex = 0L
            while (offset < source.frameCount) {
                val count = minOf(BLOCK.toLong(), source.frameCount - offset).toInt()
                val samples = source.readFrames(offset, count)
                val header = java.io.ByteArrayOutputStream()
                header.write(0xFF); header.write(0xF8) // 14-bit sync, fixed-block strategy
                header.write(0x70) // 16-bit explicit blocksize; sample rate from STREAMINFO
                header.write((source.channels - 1) shl 4) // independent channels; sample depth from STREAMINFO
                writeUtf8Number(header, frameIndex)
                header.write(((count - 1) ushr 8) and 255); header.write((count - 1) and 255)
                val head = header.toByteArray()
                val full = java.io.ByteArrayOutputStream()
                full.write(head); full.write(crc8(head))
                val bits = BitWriter(full)
                for (ch in 0 until source.channels) {
                    val pcm = IntArray(count) { i -> pcm16(samples[i * source.channels + ch]) }
                    // FLAC fixed predictor order 1, Rice residuals. Fall back to verbatim
                    // if the residual is too expensive; neither path loses samples.
                    val residual = IntArray(count - 1) { i -> pcm[i + 1] - pcm[i] }
                    var bestK = 0
                    var bestCost = Long.MAX_VALUE
                    for (k in 0..14) {
                        var cost = 6L + 4L + 16L // subframe header + Rice header + warm-up
                        for (r in residual) {
                            val mapped = if (r >= 0) r.toLong() * 2 else -2L * r - 1
                            cost += (mapped ushr k) + 1 + k
                            if (cost >= bestCost) break
                        }
                        if (cost < bestCost) { bestCost = cost; bestK = k }
                    }
                    if (count > 1 && bestCost < 8L + 16L * count) {
                        bits.write(0, 1)
                        bits.write(0b001001, 6) // fixed predictor, order 1
                        bits.write(0, 1)
                        bits.write(pcm[0], 16)
                        bits.write(0, 2) // Rice method 0
                        bits.write(0, 4) // partition order 0
                        bits.write(bestK, 4)
                        for (r in residual) {
                            val mapped = if (r >= 0) r.toLong() * 2 else -2L * r - 1
                            val quotient = (mapped ushr bestK).toInt()
                            repeat(quotient) { bits.write(0, 1) }
                            bits.write(1, 1)
                            if (bestK != 0) bits.write((mapped and ((1L shl bestK) - 1)).toInt(), bestK)
                        }
                    } else {
                        bits.write(0, 1)
                        bits.write(1, 6) // verbatim
                        bits.write(0, 1)
                        for (v in pcm) bits.write(v, 16)
                    }
                }
                bits.flush()
                val payload = full.toByteArray()
                output.write(payload)
                val crc = crc16(payload)
                output.write((crc ushr 8) and 255); output.write(crc and 255)
                offset += count; frameIndex++
            }
        }
    }

    private fun pcm16(value: Float): Int =
        (value.coerceIn(-1f, 0.9999695f) * 32768f).roundToInt().coerceIn(-32768, 32767)

    private fun writeUtf8Number(out: OutputStream, number: Long) {
        require(number in 0..0xFFFFFFFFFL)
        val count = when {
            number < 0x80 -> 1
            number < 0x800 -> 2
            number < 0x10000 -> 3
            number < 0x200000 -> 4
            number < 0x4000000 -> 5
            else -> 6
        }
        if (count == 1) { out.write(number.toInt()); return }
        val firstBits = 7 - count
        out.write((((0xFF shl (8 - count)) and 255) or
            ((number ushr (6 * (count - 1))).toInt() and ((1 shl firstBits) - 1))))
        for (i in count - 2 downTo 0) out.write(0x80 or ((number ushr (i * 6)).toInt() and 0x3F))
    }

    private class BitWriter(private val out: OutputStream) {
        private var buffer = 0
        private var pending = 0
        fun write(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                buffer = (buffer shl 1) or ((value ushr i) and 1)
                pending++
                if (pending == 8) { out.write(buffer); buffer = 0; pending = 0 }
            }
        }
        fun flush() { if (pending != 0) { out.write(buffer shl (8 - pending)); pending = 0; buffer = 0 } }
    }

    private fun crc8(bytes: ByteArray): Int {
        var crc = 0
        for (b in bytes) {
            crc = crc xor (b.toInt() and 255)
            repeat(8) { crc = if ((crc and 0x80) != 0) ((crc shl 1) xor 0x07) and 255 else (crc shl 1) and 255 }
        }
        return crc
    }
    private fun crc16(bytes: ByteArray): Int {
        var crc = 0
        for (b in bytes) {
            crc = crc xor ((b.toInt() and 255) shl 8)
            repeat(8) { crc = if ((crc and 0x8000) != 0) ((crc shl 1) xor 0x8005) and 65535 else (crc shl 1) and 65535 }
        }
        return crc
    }
}
