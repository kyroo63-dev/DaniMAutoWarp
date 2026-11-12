package com.danim.autowarp.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.security.MessageDigest

/** Decodes compressed audio directly into a PCM16 WAV cache without accumulating FloatArray chunks. */
object ProgressivePcmCache {
    data class CacheResult(val file: File, val reused: Boolean)

    fun prepare(context: Context, uri: Uri, mimeHint: String?): CacheResult {
        val key = stableKey(context, uri) ?: java.util.UUID.randomUUID().toString()
        val target = File(context.cacheDir, "danim-pcm-$key.wav")
        if (target.isFile) {
            try { Pcm16WavFileSource(target).use { if (it.frameCount > 0L) return CacheResult(target, true) } }
            catch (_: Exception) { target.delete() }
        }
        val tmp = File.createTempFile("danim-decode-", ".wav", context.cacheDir)
        try {
            val name = uri.lastPathSegment.orEmpty().lowercase()
            val mime = mimeHint.orEmpty().lowercase()
            if (name.endsWith(".wav") || name.endsWith(".aif") || name.endsWith(".aiff") || mime.contains("wav") || mime.contains("aiff")) {
                context.contentResolver.openInputStream(uri)?.use { StreamingPcmImporter.convert(it, tmp) }
                    ?: error("No se pudo abrir el audio PCM")
            } else decodeCompressed(context, uri, tmp)
            Pcm16WavFileSource(tmp).use { require(it.frameCount > 0L) { "El archivo decodificado está vacío" } }
            if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete() }
            pruneCache(context.cacheDir, target)
            return CacheResult(target, false)
        } catch (e: Exception) { tmp.delete(); throw e }
    }

    private fun pruneCache(dir: File, current: File) {
        val entries = dir.listFiles { f -> f.name.startsWith("danim-pcm-") && f.name.endsWith(".wav") }
            ?.sortedByDescending { it.lastModified() } ?: return
        entries.filter { it != current }.drop(2).forEach { old ->
            if (old.delete()) {
                File(old.parentFile, old.name + ".peaks").delete()
                File(old.parentFile, old.name + ".warp.json").delete()
            }
        }
    }

    private fun stableKey(context: Context, uri: Uri): String? {
        var size = -1L; var modified = -1L
        try { context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE, "last_modified"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val s = cursor.getColumnIndex(OpenableColumns.SIZE)
                val m = cursor.getColumnIndex("last_modified")
                if (s >= 0 && !cursor.isNull(s)) size = cursor.getLong(s)
                if (m >= 0 && !cursor.isNull(m)) modified = cursor.getLong(m)
            }
        }
        } catch (_: Exception) { return null }
        // Without both fields, use a unique key rather than reuse potentially stale data.
        if (size < 0L || modified <= 0L) return null
        val raw = "${uri}|$size|$modified|v1"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return hash.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun decodeCompressed(context: Context, uri: Uri, output: File) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No hay pista de audio compatible")
            val inputFormat = extractor.getTrackFormat(track)
            extractor.selectTrack(track)
            val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()
            var rate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            var dataBytes = 0L
            var inputDone = false
            var outputDone = false
            val info = MediaCodec.BufferInfo()
            RandomAccessFile(output, "rw").use { wav ->
                wav.setLength(0); repeat(44) { wav.write(0) }
                while (!outputDone) {
                    if (!inputDone) {
                        val idx = decoder.dequeueInputBuffer(10_000)
                        if (idx >= 0) {
                            val buffer = decoder.getInputBuffer(idx)!!; buffer.clear()
                            val count = extractor.readSampleData(buffer, 0)
                            if (count < 0) {
                                decoder.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(idx, 0, count, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    when (val idx = decoder.dequeueOutputBuffer(info, 10_000)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val fmt = decoder.outputFormat
                            require(dataBytes == 0L) { "El formato PCM cambió durante la decodificación" }
                            rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            encoding = if (fmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) fmt.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (idx >= 0) {
                            try {
                                if (info.size > 0) {
                                    require(channels in 1..2 && rate > 0) { "Solo se admite PCM mono/estéreo" }
                                    val bb = decoder.getOutputBuffer(idx)!!
                                    bb.position(info.offset); bb.limit(info.offset + info.size)
                                    val input = bb.slice().order(ByteOrder.LITTLE_ENDIAN)
                                    val bytesPerSample = when (encoding) {
                                        AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
                                        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
                                        AudioFormat.ENCODING_PCM_8BIT -> 1
                                        AudioFormat.ENCODING_PCM_16BIT -> 2
                                        else -> error("Codificación PCM no admitida: $encoding")
                                    }
                                    require(info.size % (bytesPerSample * channels) == 0) { "Bloque PCM desalineado" }
                                    val count = info.size / bytesPerSample
                                    val bytes = ByteArray(count * 2)
                                    for (i in 0 until count) {
                                        val sample = when (encoding) {
                                            AudioFormat.ENCODING_PCM_FLOAT -> input.float.coerceIn(-1f, 0.999969f)
                                            AudioFormat.ENCODING_PCM_32BIT -> (input.int.toDouble() / 2147483648.0).toFloat()
                                            AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                                                val lo = input.get().toInt() and 255
                                                val mid = input.get().toInt() and 255
                                                val hi = input.get().toInt()
                                                ((lo or (mid shl 8) or (hi shl 16)) shl 8).toFloat() / 2147483648f
                                            }
                                            AudioFormat.ENCODING_PCM_8BIT -> ((input.get().toInt() and 255) - 128) / 128f
                                            else -> input.short / 32768f
                                        }
                                        val pcm = (sample.coerceIn(-1f, 0.999969f) * 32768f).toInt().coerceIn(-32768, 32767)
                                        bytes[i * 2] = pcm.toByte()
                                        bytes[i * 2 + 1] = (pcm shr 8).toByte()
                                    }
                                    dataBytes += bytes.size
                                    require(dataBytes <= 0xFFFF_FFFFL - 36L) { "WAV supera el límite RIFF de 4 GB" }
                                    wav.write(bytes)
                                }
                                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            } finally { decoder.releaseOutputBuffer(idx, false) }
                        }
                    }
                }
                require(dataBytes > 0L && channels in 1..2) { "Decodificación vacía" }
                wav.seek(0)
                fun ascii(s: String) = wav.write(s.toByteArray(Charsets.US_ASCII))
                fun u16(n: Int) { wav.write(n and 255); wav.write((n ushr 8) and 255) }
                fun u32(n: Long) { u16(n.toInt()); u16((n ushr 16).toInt()) }
                ascii("RIFF"); u32(dataBytes + 36); ascii("WAVEfmt "); u32(16)
                u16(1); u16(channels); u32(rate.toLong()); u32(rate.toLong() * channels * 2)
                u16(channels * 2); u16(16); ascii("data"); u32(dataBytes)
            }
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            codec?.release(); extractor.release()
        }
    }
}
