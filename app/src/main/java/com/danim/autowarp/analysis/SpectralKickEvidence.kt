package com.danim.autowarp.analysis

import com.danim.autowarp.audio.PcmFrameSource
import kotlin.math.*

/** Conservative, file-backed spectral evidence for kick candidates.
 * This is a supporting cue, not a trained instrument classifier.
 */
object SpectralKickEvidence {
    data class Evidence(val bassShare: Double, val midShare: Double, val score: Double)

    fun measure(source: PcmFrameSource, onsetSeconds: Double): Evidence {
        val sampleRate = source.sampleRate
        val n = 2048
        // Include the initial body of the drum hit, not just its transient attack.
        val start = ((onsetSeconds + 0.012) * sampleRate).toLong().coerceIn(0L, (source.frameCount - 1).coerceAtLeast(0L))
        val available = (source.frameCount - start).coerceAtMost(n.toLong()).toInt()
        if (available < n / 2) return Evidence(0.0, 0.0, 0.5)
        val pcm = source.readFrames(start, available)
        val mono = DoubleArray(available) { i ->
            var v = 0.0
            for (ch in 0 until source.channels) v += pcm[i * source.channels + ch]
            (v / source.channels) * (0.5 - 0.5 * cos(2.0 * PI * i / (available - 1)))
        }
        fun power(hz: Double): Double {
            val omega = 2.0 * PI * hz / sampleRate
            val coeff = 2.0 * cos(omega)
            var s1 = 0.0; var s2 = 0.0
            for (x in mono) { val s = x + coeff * s1 - s2; s2 = s1; s1 = s }
            return (s1 * s1 + s2 * s2 - coeff * s1 * s2).coerceAtLeast(0.0)
        }
        val bass = listOf(48.0, 65.0, 85.0, 110.0, 135.0).sumOf(::power)
        val mid = listOf(260.0, 420.0, 700.0, 1100.0).sumOf(::power)
        val total = bass + mid
        if (total < 1e-8) return Evidence(0.0, 0.0, 0.5)
        val bassShare = bass / total
        // A clear midrange-dominant hit should reduce confidence, never veto on its own.
        return Evidence(bassShare, mid / total, ((bassShare - 0.2) / 0.6).coerceIn(0.0, 1.0))
    }
}
