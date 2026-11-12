package com.danim.autowarp.analysis

/** Explicit user-confirmed kick -> neutral anchor. Does not estimate or invent target beats. */
object ManualKickAnchor {
    data class Result(val markers: List<WarpMarker>, val reason: String?) {
        val accepted: Boolean get() = reason == null
    }

    fun insert(markers: List<WarpMarker>, candidateSeconds: Double, confirmedByUser: Boolean,
               minGapSeconds: Double = 0.08, minRatio: Double = 0.85, maxRatio: Double = 1.15): Result {
        fun reject(reason: String) = Result(markers, reason)
        if (!confirmedByUser) return reject("Confirma manualmente el bombo antes de añadir el marcador")
        if (!candidateSeconds.isFinite() || minGapSeconds < 0.005 || !minGapSeconds.isFinite() ||
            !minRatio.isFinite() || !maxRatio.isFinite() || minRatio <= 0 || maxRatio < minRatio)
            return reject("Parámetros inválidos")
        if (markers.size < 2 || markers.any { !it.sourceSeconds.isFinite() || !it.targetSeconds.isFinite() } ||
            markers.zipWithNext().any { (a, b) -> a.sourceSeconds >= b.sourceSeconds || a.targetSeconds >= b.targetSeconds })
            return reject("Marcadores inválidos o insuficientes")
        val right = markers.indexOfFirst { it.sourceSeconds > candidateSeconds }
        if (right <= 0) return reject("El bombo está fuera del tramo editable")
        val leftMarker = markers[right - 1]
        val rightMarker = markers[right]
        val before = candidateSeconds - leftMarker.sourceSeconds
        val after = rightMarker.sourceSeconds - candidateSeconds
        if (before < minGapSeconds || after < minGapSeconds) return reject("Demasiado cerca de otro marcador")
        val fraction = before / (rightMarker.sourceSeconds - leftMarker.sourceSeconds)
        val target = leftMarker.targetSeconds + fraction * (rightMarker.targetSeconds - leftMarker.targetSeconds)
        val ratio = (rightMarker.targetSeconds - leftMarker.targetSeconds) /
            (rightMarker.sourceSeconds - leftMarker.sourceSeconds)
        if (ratio !in minRatio..maxRatio) return reject("El tramo supera los límites de estiramiento")
        val updated = markers.toMutableList()
        updated.add(right, WarpMarker(candidateSeconds, target, 1f))
        return Result(updated, null)
    }
}
