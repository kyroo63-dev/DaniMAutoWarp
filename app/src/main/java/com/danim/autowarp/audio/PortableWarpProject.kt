package com.danim.autowarp.audio

import com.danim.autowarp.analysis.WarpMarker
import com.danim.autowarp.analysis.TransientKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/** Portable edit metadata. Audio remains a separate user-owned file. */
object PortableWarpProject {
    private const val LIMIT = 8 * 1024 * 1024
    private fun fingerprint(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val bytes = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(bytes)
                if (n < 0) break
                digest.update(bytes, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun export(audio: File, session: WarpSessionStore.Session, destination: OutputStream) {
        require(audio.isFile)
        val markers = JSONArray()
        session.markers.forEach {
            require(it.sourceSeconds.isFinite() && it.targetSeconds.isFinite() && it.strength.isFinite())
            markers.put(JSONObject().put("source", it.sourceSeconds).put("target", it.targetSeconds).put("strength", it.strength.toDouble()))
        }
        val overrides = JSONArray()
        session.transientOverrides.toSortedMap().forEach { (time, kind) ->
            require(time.isFinite() && time >= 0.0)
            overrides.put(JSONObject().put("time", time).put("kind", kind.name))
        }
        val project = JSONObject().put("transientOverrides", overrides).put("format", "DaniMAutoWarpProject")
            .put("schema", 2).put("pcmSha256", fingerprint(audio))
            .put("pcmLength", audio.length()).put("engine", session.engine.name)
            .put("quality", session.quality.name).put("manual", session.manual)
            .put("markers", markers)
        destination.bufferedWriter(Charsets.UTF_8).use { it.write(project.toString(2)) }
    }

    fun import(audio: File, input: InputStream): WarpSessionStore.Session {
        require(audio.isFile)
        val payload = input.use { stream ->
            val buffer = ByteArray(8192)
            val output = java.io.ByteArrayOutputStream()
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size() + count <= LIMIT) { "El proyecto supera el límite de 8 MB" }
                output.write(buffer, 0, count)
            }
            output.toString("UTF-8")
        }
        val json = JSONObject(payload)
        require(json.getString("format") == "DaniMAutoWarpProject" && json.getInt("schema") in 1..2) { "Formato de proyecto incompatible" }
        require(json.getLong("pcmLength") == audio.length() && json.getString("pcmSha256") == fingerprint(audio)) {
            "El proyecto pertenece a otro audio; importa primero la pista original"
        }
        val array = json.getJSONArray("markers")
        require(array.length() <= 100_000) { "Demasiados marcadores" }
        val markers = (0 until array.length()).map { index ->
            val m = array.getJSONObject(index)
            WarpMarker(m.getDouble("source"), m.getDouble("target"), m.getDouble("strength").toFloat())
        }
        require(markers.all { it.sourceSeconds.isFinite() && it.targetSeconds.isFinite() && it.strength.isFinite() && it.strength in 0f..1f })
        require(markers.zipWithNext().all { (a, b) -> a.sourceSeconds < b.sourceSeconds && a.targetSeconds < b.targetSeconds })
        val overridesArray = json.optJSONArray("transientOverrides") ?: JSONArray()
        require(overridesArray.length() <= 100_000)
        val overrides = (0 until overridesArray.length()).associate { i ->
            val item = overridesArray.getJSONObject(i)
            val time = item.getDouble("time")
            require(time.isFinite() && time >= 0.0)
            time to TransientKind.valueOf(item.getString("kind"))
        }
        return WarpSessionStore.Session(markers, WarpEngine.valueOf(json.getString("engine")), WarpQuality.valueOf(json.getString("quality")), json.getBoolean("manual"), overrides)
    }
}
