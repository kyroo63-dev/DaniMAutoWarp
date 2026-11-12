package com.danim.autowarp.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.roundToInt

/** Local PCM audition. Playback runs on a background dispatcher. */
object AudioPreview {
    @Volatile private var current: AudioTrack? = null

    fun stop() {
        val old = current
        current = null
        try { old?.pause(); old?.flush(); old?.stop() } catch (_: Exception) { }
        try { old?.release() } catch (_: Exception) { }
    }

    fun play(audio: PcmAudio) = playRange(audio, 0.0, audio.durationSeconds)

    /** Plays the exact same timeline interval for source and rendered audio, enabling fair A/B checks. */
    fun playRange(audio: PcmAudio, startSeconds: Double, endSeconds: Double) {
        require(startSeconds.isFinite() && endSeconds.isFinite() && startSeconds >= 0.0 && endSeconds > startSeconds)
        val start = (startSeconds * audio.sampleRate).toInt().coerceIn(0, audio.frames)
        val end = (endSeconds * audio.sampleRate).toInt().coerceIn(start, audio.frames)
        require(end > start) { "El fragmento seleccionado está vacío" }
        stop()
        val mask = if (audio.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val min = AudioTrack.getMinBufferSize(audio.sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(audio.sampleRate).setChannelMask(mask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(min, 8192))
            .setTransferMode(AudioTrack.MODE_STREAM).build()
        current = track
        track.play()
        val chunkFrames = 4096
        val pcm = ShortArray(chunkFrames * audio.channels)
        var pos = start
        try {
            while (pos < end && current === track) {
                val frames = minOf(chunkFrames, end - pos)
                for (i in 0 until frames * audio.channels) {
                    pcm[i] = (audio.samples[pos * audio.channels + i].coerceIn(-1f, 0.999969f) * 32768f).roundToInt().toShort()
                }
                var written = 0
                while (written < frames * audio.channels && current === track) {
                    val n = track.write(pcm, written, frames * audio.channels - written, AudioTrack.WRITE_BLOCKING)
                    if (n < 0) throw IllegalStateException("AudioTrack write error: $n")
                    written += n
                }
                pos += frames
            }
            if (current === track) track.stop()
        } finally {
            if (current === track) current = null
            try { track.release() } catch (_: Exception) { }
        }
    }
}
