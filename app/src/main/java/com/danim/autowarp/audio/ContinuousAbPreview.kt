package com.danim.autowarp.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.danim.autowarp.analysis.PreparedAbTimeline
import com.danim.autowarp.analysis.WarpMarker
import kotlin.math.floor

/** Single AudioTrack A/B preview: switches at block boundaries without restarting the output clock.
 * The original is sampled at the source positions corresponding to corrected target time.
 * Both streams use the same sample rate and channel count. A short fade limits switching clicks.
 */
object ContinuousAbPreview {
    @Volatile private var track: AudioTrack? = null
    @Volatile private var correctedSelected = false
    @Volatile private var running = false
    private val sessionCounter = java.util.concurrent.atomic.AtomicLong(0L)
    fun selectCorrected(value: Boolean) { correctedSelected = value }
    fun stop() {
        sessionCounter.incrementAndGet()
        running = false
        // Cancellation only: AudioTrack is owned exclusively by the playback thread.
        // Do not pause/flush/release from the UI thread while write() is in progress.
        // The writer checks running at each block boundary and releases in finally.
    }

    fun play(original: PcmFrameSource, corrected: PcmFrameSource, markers: List<WarpMarker>,
             targetStartSeconds: Double, initiallyCorrected: Boolean = false) {
        require(original.sampleRate == corrected.sampleRate && original.channels == corrected.channels)
        require(original.sampleRate in 8000..192000) { "Frecuencia de muestreo fuera de rango" }
        require(original.channels in 1..2 && markers.size >= 2 && original.frameCount > 1 && corrected.frameCount > 0)
        require(targetStartSeconds.isFinite() && targetStartSeconds >= 0.0)
        val timeline = PreparedAbTimeline(markers)
        val sortedMarkers = markers.sortedBy { it.targetSeconds }
        val lastTarget = sortedMarkers.last().targetSeconds
        require(targetStartSeconds >= sortedMarkers.first().targetSeconds && targetStartSeconds < lastTarget)
        // Validate the playable interval before cancelling any current playback.
        val playableFrames = AbPreviewBounds.frames(corrected.sampleRate, corrected.frameCount, targetStartSeconds, lastTarget)
        // corrected is a preview rendered from targetStartSeconds, so its local timeline starts at frame zero.
        stop()
        val session = sessionCounter.incrementAndGet()
        correctedSelected = initiallyCorrected
        running = true
        val rate = corrected.sampleRate
        val channels = corrected.channels
        val mask = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        // Initialization can fail before the playback loop's finally block. Never leave
        // the shared running flag stuck true after an unsupported device configuration.
        val audioTrack = try {
            val minBufferResult = AudioTrack.getMinBufferSize(rate, mask, AudioFormat.ENCODING_PCM_16BIT)
            require(minBufferResult > 0) { "Formato de audio no admitido por el dispositivo" }
            val bytesPerFrame = channels * 2
            val minBuffer = ((minBufferResult.coerceAtLeast(4096) + bytesPerFrame - 1) / bytesPerFrame) * bytesPerFrame
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(mask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(maxOf(minBuffer, 8192)).setTransferMode(AudioTrack.MODE_STREAM).build()
        } catch (failure: Exception) {
            if (sessionCounter.get() == session) running = false
            throw failure
        }
        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release()
            if (sessionCounter.get() == session) running = false
            throw IllegalStateException("AudioTrack failed to initialize")
        }
        // A newer playback may have been started while this track initialized.
        if (!running || sessionCounter.get() != session) {
            audioTrack.release()
            return
        }
        track = audioTrack
        val block = 256
        val out = ShortArray(block * channels)
        val crossfade = AbCrossfade(rate, 12).apply { reset(initiallyCorrected) }
        var frame = 0L
        try {
            audioTrack.play()
            while (running && sessionCounter.get() == session && track === audioTrack && frame < playableFrames) {
                val n = minOf(block.toLong(), playableFrames - frame).toInt()
                val correctedBlock = corrected.readFrames(frame, n)
                check(correctedBlock.size >= n * channels) { "Bloque corregido incompleto" }
                // Read only the original source window needed for this output block.
                val startT = targetStartSeconds + frame.toDouble() / rate
                // Strength interpolation can make the source mapping non-monotonic.
                // Inspect every frame instead of assuming block endpoints bound the source window.
                val sourcePositions = DoubleArray(n) { i ->
                    (timeline.sourceAt(startT + i.toDouble() / rate) * rate)
                        .coerceIn(0.0, (original.frameCount - 1).toDouble())
                }
                val minPosition = sourcePositions.minOrNull() ?: 0.0
                val maxPosition = sourcePositions.maxOrNull() ?: minPosition
                val low = floor(minPosition).toLong().coerceIn(0L, original.frameCount - 2)
                val high = (kotlin.math.ceil(maxPosition).toLong() + 1)
                    .coerceIn(low + 2, original.frameCount)
                require(high - low <= Int.MAX_VALUE.toLong() / channels) {
                    "Intervalo de lectura original demasiado grande"
                }
                val sourceFrames = (high - low).toInt()
                val sourceBlock = original.readFrames(low, sourceFrames)
                check(sourceBlock.size >= sourceFrames * channels) { "Bloque original incompleto" }
                for (i in 0 until n) {
                    // Latch the user selection once per frame for coherent stereo gain.
                    val selected = correctedSelected
                    val sourcePosition = sourcePositions[i] - low
                    val firstFrame = sourcePosition.toInt()
                    val nextFrame = (firstFrame + 1).coerceAtMost(sourceFrames - 1)
                    val fraction = (sourcePosition - firstFrame).toFloat()
                    for (c in 0 until channels) {
                        val first = sourceBlock[firstFrame * channels + c]
                        val next = sourceBlock[nextFrame * channels + c]
                        val originalValue = first * (1f - fraction) + next * fraction
                        val correctedValue = correctedBlock[i * channels + c]
                        val sample = crossfade.mix(originalValue, correctedValue, selected)
                        out[i * channels + c] = Pcm16SampleMath.encoded(sample)
                    }
                    crossfade.advanceFrame()
                }
                var offset = 0
                while (offset < n * channels && running && sessionCounter.get() == session && track === audioTrack) {
                    val written = audioTrack.write(out, offset, n * channels - offset, AudioTrack.WRITE_BLOCKING)
                    if (written <= 0) throw IllegalStateException("AudioTrack write: $written")
                    offset += written
                }
                frame += n
            }
            if (running && sessionCounter.get() == session && track === audioTrack) audioTrack.stop()
        } finally {
            if (track === audioTrack && sessionCounter.get() == session) { running = false; track = null }
            try { audioTrack.release() } catch (_: Exception) { }
        }
    }
}
