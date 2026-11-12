package com.danim.autowarp.analysis

/** Conservative manual adjustment: moves target time only for existing interior markers.
 * No synthetic markers, no edits across the selected window, and no unsafe stretch ratios.
 */
object ManualPhaseNudge {
    data class Result(val markers: List<WarpMarker>, val changed: Int, val reason: String?) {
        val accepted get() = reason == null
    }

    fun apply(markers: List<WarpMarker>, start: Double, end: Double, deltaMs: Int,
              minRatio: Double = 0.85, maxRatio: Double = 1.15): Result {
        fun reject(message: String) = Result(markers, 0, message)
        if (!start.isFinite() || !end.isFinite() || start < 0.0 || end <= start ||
            deltaMs !in -10..10 || deltaMs == 0 || minRatio <= 0.0 || maxRatio < minRatio) return reject("Parámetros inválidos")
        if (markers.size < 3 || markers.any { !it.sourceSeconds.isFinite() || !it.targetSeconds.isFinite() } ||
            markers.zipWithNext().any { (a, b) -> a.sourceSeconds >= b.sourceSeconds || a.targetSeconds >= b.targetSeconds })
            return reject("Marcadores insuficientes o desordenados")
        val indices = (1 until markers.lastIndex).filter { markers[it].sourceSeconds >= start && markers[it].sourceSeconds < end }
        if (indices.isEmpty()) return reject("No hay marcadores interiores en esta ventana")
        val shift = deltaMs / 1000.0
        val edited = markers.mapIndexed { i, marker -> if (i in indices) marker.copy(targetSeconds = marker.targetSeconds + shift) else marker }
        if (edited.zipWithNext().any { (a, b) ->
                val sourceDelta = b.sourceSeconds - a.sourceSeconds
                val targetDelta = b.targetSeconds - a.targetSeconds
                targetDelta <= 0.0 || targetDelta / sourceDelta !in minRatio..maxRatio
            }) return reject("El ajuste supera los límites de continuidad o estiramiento")
        return Result(edited, indices.size, null)
    }
}
