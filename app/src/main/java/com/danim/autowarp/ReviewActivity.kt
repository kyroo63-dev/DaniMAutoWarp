package com.danim.autowarp

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class ReviewActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private fun stopPlayback() {
        val old = player
        player = null
        old?.runCatching { if (isPlaying) stop() }
        old?.runCatching { reset() }
        old?.runCatching { release() }
    }
    override fun onStop() { stopPlayback(); super.onStop() }
    override fun onDestroy() { stopPlayback(); super.onDestroy() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val items = ReviewLabels.parseQueue(assets.open("review/queue.csv").bufferedReader().use { it.readText() })
        require(items.isNotEmpty()) { "La cola de revisión está vacía" }
        setContent {
            val prefs = remember { getSharedPreferences("review_rc105", MODE_PRIVATE) }
            val labels = remember { mutableStateMapOf<String,String>().apply {
                items.forEach { item -> prefs.getString(item.id, null)?.takeIf { it in ReviewLabels.allowed }?.let { put(item.id,it) } }
            } }
            var index by remember { mutableIntStateOf(ReviewProgress.restoreIndex(prefs.getString("last_review_id", null), items)) }
            var status by remember { mutableStateOf("Sin etiquetas humanas") }
            val current = items[index]
            LaunchedEffect(current.id) { prefs.edit().putString("last_review_id", current.id).apply() }
            DisposableEffect(index) {
                onDispose { stopPlayback() }
            }
            val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
                if (uri != null) runCatching {
                    contentResolver.openOutputStream(uri)?.use { it.write(ReviewLabels.export(items,labels).toByteArray(Charsets.UTF_8)) }
                        ?: error("No se pudo abrir el destino")
                }.onSuccess { status = "CSV guardado" }.onFailure { status = "Error al exportar: ${it.message}" }
            }
            val importCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) runCatching {
                    val csv = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("No se pudo leer el archivo")
                    ReviewCsvImport.parse(csv, items)
                }.onSuccess { imported ->
                    // Validation is complete before any saved label is modified.
                    val merged = ReviewProgress.merge(labels.toMap(), imported.labels, items)
                    // Commit the merged snapshot, not only the imported subset.
                    val editor = prefs.edit()
                    merged.forEach { (id, value) -> editor.putString(id, value) }
                    if (!editor.commit()) {
                        status = "Error al guardar etiquetas importadas"
                    } else {
                        labels.clear()
                        labels.putAll(merged)
                        status = "Importadas ${imported.reviewed} etiquetas; se conservaron las existentes"
                    }
                }.onFailure { status = "CSV rechazado: ${it.message}" }
            }
            val samples = remember(index) { runCatching { ReviewWav.peaks(assets.open("review/clips/${current.clip}").use { it.readBytes() }) }.getOrDefault(emptyList()) }
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB56BFF))) {
                Column(Modifier.fillMaxSize().background(Color(0xFF080D13)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("DaniM · REVISIÓN DE ATAQUES", color = Color.White, fontSize = 20.sp)
                    Text("${index+1} / ${items.size} · ${labels.size} etiquetados", color = Color.LightGray)
                    Text("Desliza para acceder a todas las acciones", color = Color.LightGray, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { labels.size.toFloat()/items.size }, modifier = Modifier.fillMaxWidth())
                    Text("${current.id} · predicción anterior: ${current.prediction}", color = Color.LightGray)
                    Canvas(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF15212D),RoundedCornerShape(12.dp))) {
                        val mid = size.height/2
                        if(samples.isNotEmpty()) samples.forEachIndexed { i, v ->
                            val x = i.toFloat() / maxOf(1, samples.size - 1) * size.width
                            drawLine(Color(0xFFB56BFF),Offset(x,mid-v*mid*0.9f),Offset(x,mid+v*mid*0.9f),1.5f)
                        }
                    }
                    Button(onClick = {
                        runCatching {
                            stopPlayback()
                            val next = MediaPlayer()
                            player = next
                            next.apply {
                                assets.openFd("review/clips/${current.clip}").use { fd -> setDataSource(fd.fileDescriptor,fd.startOffset,fd.length) }
                                setOnCompletionListener { completed -> if (player === completed) stopPlayback() }
                                setOnErrorListener { failed, _, _ ->
                                    if (player === failed) { stopPlayback(); status = "Error al reproducir el fragmento" }
                                    true
                                }
                                prepare(); start()
                            }
                        }.onFailure { stopPlayback(); status = "Error de audio: ${it.message}" }
                    }, modifier=Modifier.fillMaxWidth()) { Text("▶ ESCUCHAR ATAQUE") }
                    listOf("KICK" to "BOMBO", "SNARE" to "CAJA", "PERCUSSION" to "OTRA PERCUSIÓN", "UNCERTAIN" to "INCIERTO").forEach { (value,title) ->
                        OutlinedButton(onClick = {
                            if (prefs.edit().putString(current.id, value).commit()) {
                                labels[current.id] = value
                                index = ReviewProgress.nextIndex(index, items.size)
                                status = "Etiqueta guardada"
                            } else status = "No se pudo guardar la etiqueta"
                        }, modifier=Modifier.fillMaxWidth()) {
                            Text("${if(labels[current.id]==value) "✓ " else ""}$title")
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={ index = ReviewProgress.previousIndex(index, items.size) }, enabled=index>0,modifier=Modifier.weight(1f)){Text("ANTERIOR")}
                        OutlinedButton(onClick={ index = ReviewProgress.nextIndex(index, items.size) },enabled=index<items.lastIndex,modifier=Modifier.weight(1f)){Text("SIGUIENTE")}
                    }
                    OutlinedButton(onClick={ importCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) },modifier=Modifier.fillMaxWidth()){Text("IMPORTAR ETIQUETAS CSV")}
                    Button(onClick={ export.launch("danim-review-rc110.csv") },modifier=Modifier.fillMaxWidth()){Text("EXPORTAR ETIQUETAS CSV")}
                    val metrics = ReviewLabels.evaluate(items,labels)
                    Text(if(metrics==null) "Precisión: pendiente de etiquetas" else "Precisión bombo: ${metrics.precision?.let { String.format(Locale.US,"%.1f%%",it*100) } ?: "N/D"} · Sensibilidad: ${metrics.recall?.let { String.format(Locale.US,"%.1f%%",it*100) } ?: "N/D"}",color=Color.LightGray)
                    Text(status,color=Color.LightGray,fontSize=12.sp)
                }
            }
        }
    }
}
