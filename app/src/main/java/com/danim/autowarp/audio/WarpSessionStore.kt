package com.danim.autowarp.audio

import com.danim.autowarp.analysis.WarpMarker
import com.danim.autowarp.analysis.TransientKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Versioned, atomic sidecar for edits associated with an immutable PCM cache file. */
object WarpSessionStore {
    data class Session(
        val markers: List<WarpMarker>,
        val engine: WarpEngine,
        val quality: WarpQuality,
        val manual: Boolean,
        val transientOverrides: Map<Double, TransientKind> = emptyMap()
    )

    private fun sidecar(pcmFile: File) = File(pcmFile.parentFile, pcmFile.name + ".warp.json")

    fun save(pcmFile: File, session: Session) {
        require(pcmFile.isFile) { "No existe el audio de la sesión" }
        val arr = JSONArray()
        session.markers.forEach { marker ->
            require(marker.sourceSeconds.isFinite() && marker.targetSeconds.isFinite() && marker.strength.isFinite())
            arr.put(JSONObject().put("source", marker.sourceSeconds).put("target", marker.targetSeconds).put("strength", marker.strength.toDouble()))
        }
        val overrides = JSONArray()
        session.transientOverrides.toSortedMap().forEach { (time, kind) ->
            require(time.isFinite() && time >= 0.0)
            overrides.put(JSONObject().put("time", time).put("kind", kind.name))
        }
        val json = JSONObject().put("schema", 2).put("transientOverrides", overrides)
            .put("pcmLength", pcmFile.length()).put("markers", arr)
            .put("engine", session.engine.name).put("quality", session.quality.name)
            .put("manual", session.manual)
        val dest = sidecar(pcmFile)
        val temp = File(dest.parentFile, dest.name + ".tmp")
        try {
            temp.writeText(json.toString(), Charsets.UTF_8)
            if (!temp.renameTo(dest)) {
                // Do not delete a previously valid session if the replacement fails.
                throw java.io.IOException("No se pudo confirmar el guardado de sesión")
            }
        } finally { temp.delete() }
    }

    fun load(pcmFile: File): Session? {
        val file = sidecar(pcmFile)
        if (!pcmFile.isFile || !file.isFile) return null
        return runCatching {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            require(json.getInt("schema") in 1..2 && json.getLong("pcmLength") == pcmFile.length())
            val arr = json.getJSONArray("markers")
            require(arr.length() <= 100_000) { "Demasiados marcadores" }
            val markers = (0 until arr.length()).map { index ->
                val item = arr.getJSONObject(index)
                WarpMarker(item.getDouble("source"), item.getDouble("target"), item.optDouble("strength", 1.0).toFloat())
            }
            require(markers.all { it.sourceSeconds.isFinite() && it.targetSeconds.isFinite() && it.strength.isFinite() && it.strength in 0f..1f })
            require(markers.zipWithNext().all { (a,b) -> a.sourceSeconds < b.sourceSeconds && a.targetSeconds < b.targetSeconds })
            val overridesArray = json.optJSONArray("transientOverrides") ?: JSONArray()
            require(overridesArray.length() <= 100_000)
            val overrides = (0 until overridesArray.length()).associate { i ->
                val item = overridesArray.getJSONObject(i)
                val time = item.getDouble("time")
                require(time.isFinite() && time >= 0.0)
                time to TransientKind.valueOf(item.getString("kind"))
            }
            Session(markers, WarpEngine.valueOf(json.getString("engine")), WarpQuality.valueOf(json.getString("quality")), json.getBoolean("manual"), overrides)
        }.getOrNull()
    }
}
