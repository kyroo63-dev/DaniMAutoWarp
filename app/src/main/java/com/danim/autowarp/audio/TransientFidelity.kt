package com.danim.autowarp.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Diagnostic only: compare energy and attack timing near known source/target transients. */
object TransientFidelity {
    data class Measurement(val expectedSeconds: Double, val timingErrorMs: Double,
                           val energyRatio: Double, val sourcePeak: Double, val outputPeak: Double)

    fun measure(source: FloatArray, output: FloatArray, sampleRate: Int,
                sourceSeconds: Double, targetSeconds: Double, radiusMs: Double = 35.0): Measurement {
        require(sampleRate > 0 && source.isNotEmpty() && output.isNotEmpty())
        require(sourceSeconds.isFinite() && targetSeconds.isFinite() && radiusMs.isFinite() && radiusMs > 0.0)
        val radius = max(1, (radiusMs * sampleRate / 1000).toInt())
        fun window(samples: FloatArray, seconds: Double): IntRange {
            val center = (seconds * sampleRate).toInt()
            require(center in samples.indices) { "Transient outside audio" }
            return max(0, center - radius)..minOf(samples.lastIndex, center + radius)
        }
        val srcRange = window(source, sourceSeconds)
        val outRange = window(output, targetSeconds)
        fun peakIndex(samples: FloatArray, range: IntRange): Int = range.maxByOrNull { abs(samples[it]) }!!
        fun energy(samples: FloatArray, range: IntRange): Double =
            sqrt(range.sumOf { val x = samples[it].toDouble(); x * x } / range.count())
        val srcPeakIndex = peakIndex(source, srcRange)
        val outPeakIndex = peakIndex(output, outRange)
        val sourceOffset = srcPeakIndex.toDouble() / sampleRate - sourceSeconds
        val outputOffset = outPeakIndex.toDouble() / sampleRate - targetSeconds
        val srcEnergy = energy(source, srcRange)
        val outEnergy = energy(output, outRange)
        return Measurement(targetSeconds, (outputOffset - sourceOffset) * 1000.0,
            if (srcEnergy > 1e-9) outEnergy / srcEnergy else Double.NaN,
            abs(source[srcPeakIndex].toDouble()), abs(output[outPeakIndex].toDouble()))
    }
}
