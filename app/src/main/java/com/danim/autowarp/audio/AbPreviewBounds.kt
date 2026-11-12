package com.danim.autowarp.audio

/** Pure validation of the playable target interval, testable without Android APIs. */
object AbPreviewBounds {
    fun frames(sampleRate: Int, renderedFrames: Long, targetStart: Double, targetEnd: Double): Long {
        require(sampleRate > 0 && renderedFrames > 0)
        require(targetStart.isFinite() && targetEnd.isFinite() && targetStart >= 0 && targetEnd > targetStart)
        val durationFrames = (targetEnd - targetStart) * sampleRate
        require(durationFrames.isFinite() && durationFrames >= 1.0) { "Fragmento demasiado corto" }
        return minOf(renderedFrames, durationFrames.toLong()).also { require(it > 0) }
    }
}
