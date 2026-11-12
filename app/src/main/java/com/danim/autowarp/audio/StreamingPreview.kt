package com.danim.autowarp.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.roundToInt

/** File/window based A/B playback. Reads PCM in small chunks and never builds a clip-sized array. */
object StreamingPreview {
    @Volatile private var current: AudioTrack? = null

    fun stop() {
        val old = current
        current = null
        try { old?.pause(); old?.flush(); old?.stop() } catch (_: Exception) { }
        try { old?.release() } catch (_: Exception) { }
    }

    fun playRange(source: PcmFrameSource, startSeconds: Double, endSeconds: Double) {
        require(source.sampleRate > 0 && source.channels in 1..2)
        require(startSeconds.isFinite() && endSeconds.isFinite() && startSeconds >= 0.0 && endSeconds > startSeconds)
        val start = (startSeconds * source.sampleRate).toLong().coerceIn(0L, source.frameCount)
        val end = (endSeconds * source.sampleRate).toLong().coerceIn(start, source.frameCount)
        require(end > start) { "El fragmento seleccionado está vacío" }
        stop()
        val mask = if (source.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minBuffer = AudioTrack.getMinBufferSize(source.sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(source.sampleRate).setChannelMask(mask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(minBuffer, 16_384))
            .setTransferMode(AudioTrack.MODE_STREAM).build()
        current = track
        val chunkFrames = 2048
        val pcm = ShortArray(chunkFrames * source.channels)
        track.play()
        var position = start
        try {
            while (position < end && current === track) {
                val count = minOf(chunkFrames.toLong(), end - position).toInt()
                val samples = source.readFrames(position, count)
                require(samples.size == count * source.channels) { "Ventana de audio incompleta" }
                for (i in samples.indices) pcm[i] = (samples[i].coerceIn(-1f, 0.999969f) * 32768f).roundToInt().toShort()
                var written = 0
                while (written < count * source.channels && current === track) {
                    val n = track.write(pcm, written, count * source.channels - written, AudioTrack.WRITE_BLOCKING)
                    if (n < 0) throw IllegalStateException("AudioTrack write error: $n")
                    written += n
                }
                position += count
            }
            if (current === track) track.stop()
        } finally {
            if (current === track) current = null
            try { track.release() } catch (_: Exception) { }
        }
    }
}
