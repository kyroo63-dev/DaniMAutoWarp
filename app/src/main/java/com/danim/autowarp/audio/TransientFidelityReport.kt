package com.danim.autowarp.audio

import kotlin.math.abs

/** Diagnostic summary against an explicit source-to-target transient map. */
object TransientFidelityReport {
    data class Row(val sourceSeconds: Double, val targetSeconds: Double,
                   val timingErrorMs: Double, val energyRatio: Double)
    data class Summary(val rows: List<Row>, val maxAbsoluteTimingErrorMs: Double,
                       val meanAbsoluteTimingErrorMs: Double, val meanEnergyRatio: Double,
                       val invalidEnergyRows: Int)

    fun analyze(source: FloatArray, output: FloatArray, sampleRate: Int,
                positions: List<Pair<Double, Double>>, radiusMs: Double = 35.0): Summary {
        require(positions.isNotEmpty())
        val rows = positions.map { (src, dst) ->
            val m = TransientFidelity.measure(source, output, sampleRate, src, dst, radiusMs)
            Row(src, dst, m.timingErrorMs, m.energyRatio)
        }
        val valid = rows.filter { it.energyRatio.isFinite() }
        return Summary(rows, rows.maxOf { abs(it.timingErrorMs) },
            rows.map { abs(it.timingErrorMs) }.average(),
            if (valid.isEmpty()) Double.NaN else valid.map { it.energyRatio }.average(),
            rows.size - valid.size)
    }

    fun csv(summary: Summary): String = buildString {
        appendLine("source_seconds,target_seconds,timing_error_ms,energy_ratio")
        for (row in summary.rows) {
            append(row.sourceSeconds).append(',').append(row.targetSeconds).append(',')
                .append(row.timingErrorMs).append(',').append(row.energyRatio).append('\n')
        }
    }
}
