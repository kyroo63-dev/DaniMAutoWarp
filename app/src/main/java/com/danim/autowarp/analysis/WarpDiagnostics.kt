package com.danim.autowarp.analysis

import kotlin.math.abs

/** Lightweight, deterministic report suitable for a UI panel or JSON export. */
data class WarpDiagnostics(
    val detectedKicks: Int,
    val detectedSnares: Int,
    val detectedHats: Int,
    val detectedOtherPercussion: Int,
    val acceptedWarpMarkers: Int,
    val rejectedWarpMarkers: Int,
    val medianAbsoluteTimingMs: Double,
    val maximumAbsoluteTimingMs: Double,
    val tempoVariationBpm: Double,
    val warnings: List<String>
)

object WarpDiagnosticsAnalyzer {
    fun build(events: List<TransientEvent>, timingMs: List<Double>, tempoCurve: List<Pair<Double, Double>>,
              safety: WarpSafetyGate.Result, confidence: Double): WarpDiagnostics {
        val deviations = timingMs.filter { it.isFinite() }.map { abs(it) }.sorted()
        val median = if (deviations.isEmpty()) 0.0 else if (deviations.size % 2 == 0)
            (deviations[deviations.size / 2 - 1] + deviations[deviations.size / 2]) / 2
            else deviations[deviations.size / 2]
        val tempos = tempoCurve.map { it.second }.filter { it.isFinite() && it > 0 }
        val variation = if (tempos.isEmpty()) 0.0 else tempos.maxOrNull()!! - tempos.minOrNull()!!
        val warnings = buildList {
            if (confidence < 0.4) add("Detección de ritmo con confianza baja: revisar manualmente")
            if (safety.rejected > 0) add("Se descartaron ${safety.rejected} marcadores automáticos anómalos")
            if (variation > 8.0) add("Variación de BPM elevada: comprobar golpes falsos y cambios de tempo")
            if (deviations.lastOrNull()?.let { it > 250.0 } == true) add("Desfase acumulado superior a 250 ms")
        }
        return WarpDiagnostics(
            events.count { it.kind == TransientKind.KICK },
            events.count { it.kind == TransientKind.SNARE },
            events.count { it.kind == TransientKind.HI_HAT },
            events.count { it.kind == TransientKind.PERCUSSION },
            (safety.markers.size - 2).coerceAtLeast(0), safety.rejected, median,
            deviations.lastOrNull() ?: 0.0, variation, warnings
        )
    }
}
