package com.danim.autowarp.analysis

/** Helpers for non-destructive, marker-based zone amount editing. */
object WarpZoneTools {
    fun setMarkerStrength(markers: List<WarpMarker>, index: Int, strength: Float): List<WarpMarker> {
        require(index in markers.indices) { "Índice de marcador inválido" }
        require(markers.size >= 2) { "Se necesitan al menos dos marcadores" }
        // Endpoints remain full-strength anchors so the file boundaries stay deterministic.
        if (index == 0 || index == markers.lastIndex) return markers
        return markers.toMutableList().also {
            it[index] = it[index].copy(strength = strength.coerceIn(0f, 1f))
        }
    }

    /** Applies a smooth strength profile to an inclusive marker interval. */
    fun setZoneStrength(markers: List<WarpMarker>, firstIndex: Int, lastIndex: Int, strength: Float): List<WarpMarker> {
        require(firstIndex in markers.indices && lastIndex in markers.indices && firstIndex < lastIndex)
        val s = strength.coerceIn(0f, 1f)
        return markers.mapIndexed { i, marker ->
            if (i in firstIndex..lastIndex && i != 0 && i != markers.lastIndex) marker.copy(strength = s) else marker
        }
    }
}
