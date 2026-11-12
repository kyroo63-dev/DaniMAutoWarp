package com.danim.autowarp.analysis

import kotlin.math.abs
import kotlin.math.round

/** Explicit manual alignment of one confirmed kick to a configurable target BPM lattice.
 * All existing anchors remain fixed. The target shift is rejected if local ratios are unsafe.
 */
object ManualBeatGrid {
    data class Result(val markers: List<WarpMarker>, val targetSeconds: Double?, val reason: String?) {
        val accepted get() = reason == null
    }

    fun align(markers: List<WarpMarker>, kickSeconds: Double, bpm: Double,
              phaseMs: Double = 0.0, confirmedByUser: Boolean,
              minGapSeconds: Double = 0.08, minRatio: Double = 0.85,
              maxRatio: Double = 1.15): Result {
        fun reject(message: String) = Result(markers, null, message)
        if (!confirmedByUser) return reject("Confirma manualmente el bombo")
        if (!kickSeconds.isFinite() || !bpm.isFinite() || bpm !in 40.0..240.0 ||
            !phaseMs.isFinite() || abs(phaseMs) > 5000.0 ||
            !minGapSeconds.isFinite() || minGapSeconds <= 0.0 ||
            !minRatio.isFinite() || !maxRatio.isFinite() || minRatio <= 0 || maxRatio < minRatio)
            return reject("Parámetros BPM o fase inválidos")
        if (markers.size < 2 || markers.any { !it.sourceSeconds.isFinite() || !it.targetSeconds.isFinite() } ||
            markers.zipWithNext().any { (a, b) -> a.sourceSeconds >= b.sourceSeconds || a.targetSeconds >= b.targetSeconds })
            return reject("Marcadores inválidos")
        val existing = markers.indexOfFirst { abs(it.sourceSeconds - kickSeconds) < 0.000001 }
        if (existing == 0 || existing == markers.lastIndex) return reject("No se pueden desplazar los extremos")
        val right = if (existing > 0) existing + 1 else markers.indexOfFirst { it.sourceSeconds > kickSeconds }
        val left = if (existing > 0) existing - 1 else right - 1
        if (left < 0 || right !in markers.indices) return reject("Bombo fuera del tramo editable")
        val before = kickSeconds - markers[left].sourceSeconds
        val after = markers[right].sourceSeconds - kickSeconds
        if (before < minGapSeconds || after < minGapSeconds) return reject("Bombo demasiado próximo a otro marcador")
        // Choose the closest beat in target time, not in source time, to respect prior edits.
        val fraction = before / (before + after)
        val currentTarget = if (existing > 0) markers[existing].targetSeconds else
            markers[left].targetSeconds + fraction * (markers[right].targetSeconds - markers[left].targetSeconds)
        val period = 60.0 / bpm
        val origin = markers.first().targetSeconds + phaseMs / 1000.0
        val beat = round((currentTarget - origin) / period)
        val target = origin + beat * period
        if (!target.isFinite() || target <= markers[left].targetSeconds || target >= markers[right].targetSeconds)
            return reject("La cuadrícula cruza otro marcador")
        if ((target - markers[left].targetSeconds) / before !in minRatio..maxRatio ||
            (markers[right].targetSeconds - target) / after !in minRatio..maxRatio)
            return reject("La cuadrícula requiere un estiramiento excesivo")
        val edited = markers.toMutableList()
        if (existing > 0) edited[existing] = edited[existing].copy(targetSeconds = target)
        else edited.add(right, WarpMarker(kickSeconds, target, 1f))
        return Result(edited, target, null)
    }
}
