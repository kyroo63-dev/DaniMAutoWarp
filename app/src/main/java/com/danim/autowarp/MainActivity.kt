package com.danim.autowarp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danim.autowarp.analysis.MarkerTouchSelection
import com.danim.autowarp.analysis.*
import com.danim.autowarp.audio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val BG = Color(0xFF05080B)
private val CARD = Color(0xFF0D1218)
private val TEAL = Color(0xFFFF850B)
private val ORANGE = Color(0xFFFF850B)
private val MUTED = Color(0xFFB5BBC5)
private val PINK = Color(0xFFFF617B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { DaniMApp() } }
    override fun onStop() { AudioPreview.stop(); StreamingPreview.stop(); ContinuousAbPreview.stop(); super.onStop() }
}

@Composable private fun DaniMApp() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var audioInfo by remember { mutableStateOf<AudioFileInfo?>(null) }
    var importedUri by remember { mutableStateOf<Uri?>(null) }
    var pcmCacheFile by remember { mutableStateOf<java.io.File?>(null) }
    var waveformPeaks by remember { mutableStateOf<WaveformPeaks?>(null) }
    var result by remember { mutableStateOf<AnalysisResult?>(null) }
    var markers by remember { mutableStateOf<List<WarpMarker>>(emptyList()) }
    var fileName by remember { mutableStateOf("Ningún audio cargado") }
    var status by remember { mutableStateOf("Importa una pista para comenzar") }
    var manual by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var continuousAb by remember { mutableStateOf(false) }
    var abCorrected by remember { mutableStateOf(false) }
    var warpedCache by remember { mutableStateOf<PcmAudio?>(null) }
    var undoStack by remember { mutableStateOf<List<List<WarpMarker>>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<List<WarpMarker>>>(emptyList()) }
    var waveZoom by remember { mutableFloatStateOf(1f) }
    var waveStart by remember { mutableFloatStateOf(0f) }
    var warpQuality by remember { mutableStateOf(WarpQuality.BALANCED) }
    var warpEngine by remember { mutableStateOf(WarpEngine.WSOLA) }
    var streamingExport by remember { mutableStateOf(true) }
    var outputFormat by remember { mutableStateOf(HighResolutionAudioExport.Format.WAV16) }
    var renderInfo by remember { mutableStateOf("Sin render todavía") }
    var qualityReport by remember { mutableStateOf<RenderQualityReport?>(null) }
    var selectedMarkerIndex by remember { mutableIntStateOf(-1) }
    var abStartSeconds by remember { mutableFloatStateOf(0f) }
    var sessionReady by remember { mutableStateOf(false) }
    var diagnosticsExpanded by remember { mutableStateOf(true) }
    var showTransients by remember { mutableStateOf(true) }
    var showTargetGrid by remember { mutableStateOf(true) }
    var showPhaseZones by remember { mutableStateOf(true) }
    var selectedPhaseWindow by remember { mutableIntStateOf(-1) }
    var nudgeStepMs by remember { mutableIntStateOf(10) }
    var selectedKickInWindow by remember { mutableIntStateOf(0) }
    var manualGridBpm by remember { mutableStateOf("136.0") }
    var manualGridPhaseMs by remember { mutableStateOf("0") }
    var transientOverrides by remember { mutableStateOf<Map<Double, TransientKind>>(emptyMap()) }

    LaunchedEffect(pcmCacheFile, markers, warpQuality, warpEngine, manual, transientOverrides, sessionReady) {
        val file = pcmCacheFile
        if (sessionReady && file?.isFile == true) {
            kotlinx.coroutines.delay(650) // Coalesce edits while dragging markers.
            withContext(Dispatchers.IO) {
                runCatching { WarpSessionStore.save(file, WarpSessionStore.Session(markers, warpEngine, warpQuality, manual, transientOverrides)) }
                    .onFailure { android.util.Log.w("DaniMAutoWarp", "No se pudo guardar la sesión", it) }
            }
        }
    }

    fun commitMarkers(next: List<WarpMarker>) {
        if (next == markers) return
        undoStack = (undoStack + listOf(markers.toList())).takeLast(60)
        redoStack = emptyList()
        markers = next
        warpedCache = null
    }
    fun beginMarkerEdit() {
        undoStack = (undoStack + listOf(markers.toList())).takeLast(60)
        redoStack = emptyList()
    }
    fun undoEdit() {
        if (undoStack.isNotEmpty()) {
            redoStack = (redoStack + listOf(markers.toList())).takeLast(60)
            markers = undoStack.last(); undoStack = undoStack.dropLast(1); warpedCache = null
        }
    }
    fun redoEdit() {
        if (redoStack.isNotEmpty()) {
            undoStack = (undoStack + listOf(markers.toList())).takeLast(60)
            markers = redoStack.last(); redoStack = redoStack.dropLast(1); warpedCache = null
        }
    }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            sessionReady = false
            busy = true; status = "Decodificando audio…"
            try {
                val cacheResult = withContext(Dispatchers.IO) {
                    ProgressivePcmCache.prepare(ctx, uri, ctx.contentResolver.getType(uri))
                }
                val loaded = withContext(Dispatchers.IO) {
                    Pcm16WavFileSource(cacheResult.file).use { AudioFileInfo(it.sampleRate, it.channels, it.frameCount) }
                }
                val peaks = withContext(Dispatchers.IO) {
                    val peakFile = java.io.File(cacheResult.file.parentFile, cacheResult.file.name + ".peaks")
                    val cached = if (peakFile.isFile) runCatching { WaveformPeakCache.load(peakFile) }.getOrNull() else null
                    cached ?: Pcm16WavFileSource(cacheResult.file).use { WaveformPeakCache.build(it) }.also { WaveformPeakCache.save(peakFile, it) }
                }
                val restored = withContext(Dispatchers.IO) { WarpSessionStore.load(cacheResult.file) }
                waveformPeaks = peaks
                audioInfo = loaded; importedUri = uri; result = null; transientOverrides = emptyMap(); markers = emptyList(); warpedCache = null; undoStack = emptyList(); redoStack = emptyList(); waveZoom = 1f; waveStart = 0f; abStartSeconds = 0f
                pcmCacheFile = cacheResult.file
                if (restored != null) {
                    markers = restored.markers; warpEngine = restored.engine
                    warpQuality = restored.quality; manual = restored.manual; transientOverrides = restored.transientOverrides
                }
                sessionReady = true
                fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio importado"
                status = "${loaded.sampleRate} Hz · ${loaded.channels} canales · ${fmt(loaded.durationSeconds, 1)} s · caché PCM ${if (cacheResult.reused) "reutilizada" else "creada"}${if (restored != null) " · sesión restaurada" else ""}"
            } catch (e: Exception) { status = "No se pudo abrir: ${e.message}" }
            finally { busy = false }
        }
    }
    val exportProject = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val audio = pcmCacheFile
        if (uri != null && audio?.isFile == true) scope.launch {
            busy = true
            try {
                val session = WarpSessionStore.Session(markers.toList(), warpEngine, warpQuality, manual, transientOverrides)
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { PortableWarpProject.export(audio, session, it) }
                        ?: error("No se pudo crear el archivo")
                }
                status = "Proyecto .json guardado fuera de la caché"
            } catch (e: Exception) { status = "Error al guardar proyecto: ${e.message}" }
            finally { busy = false }
        }
    }
    val importProject = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val audio = pcmCacheFile
        if (uri != null && audio?.isFile == true) scope.launch {
            busy = true
            try {
                val session = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { PortableWarpProject.import(audio, it) }
                        ?: error("No se pudo leer el proyecto")
                }
                markers = session.markers; warpEngine = session.engine; warpQuality = session.quality
                manual = session.manual; transientOverrides = session.transientOverrides; warpedCache = null; undoStack = emptyList(); redoStack = emptyList()
                status = "Proyecto restaurado · ${session.markers.size} marcadores"
            } catch (e: Exception) { status = "Error al abrir proyecto: ${e.message}" }
            finally { busy = false }
        }
    }
    val saveGrid = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val analysis = result
        if (uri != null && analysis != null) scope.launch {
            try {
                val events = analysis.kickTimes.map { TransientEvent(it, TransientKind.KICK, 0.9) } + analysis.otherTransients
                val csv = BeatgridCsv.encode(analysis, events)
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.bufferedWriter(Charsets.UTF_8).use { it.write(csv) } }
                status = "Beatgrid CSV exportado"
            } catch (e: Exception) { status = "Error al exportar CSV: ${e.message}" }
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val cacheFile = pcmCacheFile; val m = markers
        if (uri != null && cacheFile?.exists() == true && m.size >= 2) scope.launch {
            busy = true; qualityReport = null; status = "Renderizando warp…"
            try {
                val events = result?.kickTimes.orEmpty().map { TransientEvent(it, TransientKind.KICK, 0.9) } + result?.otherTransients.orEmpty()
                val temporaryRender = withContext(Dispatchers.IO) { java.io.File.createTempFile("danim-export-", ".wav", ctx.cacheDir) }
                try {
                    if (streamingExport && warpEngine == WarpEngine.WSOLA) {
                        val report = withContext(Dispatchers.IO) {
                            Pcm16WavFileSource(cacheFile!!).use { source ->
                                temporaryRender.outputStream().buffered().use { stream ->
                                    StreamingWsolaProcessor.renderWav(source, m, stream, warpQuality, events)
                                }
                            }
                        }
                        renderInfo = "WSOLA streaming · ${report.grainsProcessed} granos · ${report.lockedTransientAnchors} anclajes"
                    } else {
                        val report = withContext(Dispatchers.Default) {
                            val src = cacheFile!!.inputStream().buffered().use { PcmWave.read(it) }
                            if (warpEngine == WarpEngine.WSOLA) WsolaWarp.renderDetailed(src, m, events, warpQuality)
                            else WarpRenderReport(PhaseVocoderWarp.render(src, m, if (warpQuality == WarpQuality.HIGH) 4096 else if (warpQuality == WarpQuality.FAST) 1024 else 2048, events), 0, 1.0, 1.0)
                        }
                        withContext(Dispatchers.IO) { temporaryRender.outputStream().buffered().use { PcmWave.write(report.output, it) } }
                        warpedCache = report.output
                        renderInfo = "${report.lockedTransients} anclajes · ratio ${fmt(report.averageStretchRatio, 3)}×"
                    }
                    val reportQuality = withContext(Dispatchers.IO) {
                        Pcm16WavFileSource(cacheFile!!).use { original ->
                            Pcm16WavFileSource(temporaryRender).use { rendered ->
                                StreamingQualityAnalyzer.analyze(original, rendered)
                            }
                        }
                    }
                    qualityReport = reportQuality
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri)!!.buffered().use { stream ->
                            HighResolutionAudioExport.convert(temporaryRender, stream, outputFormat)
                        }
                    }
                    status = if (reportQuality.hasWarnings)
                        "${outputFormat.label} exportado · Revisa los avisos de calidad"
                    else "${outputFormat.label} exportado · Diagnóstico sin alertas"
                } finally { temporaryRender.delete() }
            } catch (e: Exception) { status = "Error al exportar: ${e.message}" }
            finally { busy = false }
        }
    }
    fun auditionContinuous() {
        val cache = pcmCacheFile ?: return
        if (playing || busy || markers.size < 2 || warpEngine != WarpEngine.WSOLA) return
        scope.launch {
            playing = true
            continuousAb = true
            abCorrected = false
            status = "Preparando A/B continuo (fragmento de 8 s)…"
            var source: Pcm16WavFileSource? = null
            var corrected: Pcm16WavFileSource? = null
            var temp: java.io.File? = null
            try {
                val previewRange = com.danim.autowarp.analysis.AbPreviewRanges.select(markers, abStartSeconds.toDouble())
                val start = previewRange.startSeconds
                temp = java.io.File.createTempFile("danim-ab-continuous-", ".wav", ctx.cacheDir)
                source = withContext(Dispatchers.IO) { Pcm16WavFileSource(cache) }
                val events = result?.kickTimes.orEmpty().map { TransientEvent(it, TransientKind.KICK, 0.9) } + result?.otherTransients.orEmpty()
                val renderFile = temp!!
                val sourceFile = source!!
                withContext(Dispatchers.IO) {
                    renderFile.outputStream().buffered().use {
                        StreamingWsolaProcessor.renderWav(sourceFile, markers, it, warpQuality, events,
                            previewStartSeconds = start, previewDurationSeconds = previewRange.durationSeconds)
                    }
                }
                corrected = withContext(Dispatchers.IO) { Pcm16WavFileSource(renderFile) }
                status = "A/B continuo · cambia ORIGINAL / CORREGIDO durante la escucha"
                val correctedFile = corrected!!
                withContext(Dispatchers.IO) { ContinuousAbPreview.play(sourceFile, correctedFile, markers, start) }
                status = "A/B continuo finalizado"
            } catch (e: Exception) { status = "Error A/B continuo: ${e.message}" }
            finally {
                ContinuousAbPreview.stop()
                corrected?.close(); source?.close(); temp?.delete()
                playing = false; continuousAb = false
            }
        }
    }

    fun audition(corrected: Boolean) {
        val cacheFile = pcmCacheFile ?: return
        scope.launch {
            playing = true; status = if (corrected) "Preparando fragmento corregido…" else "Reproduciendo original streaming…"
            var sourceReader: Pcm16WavFileSource? = null
            var renderedReader: Pcm16WavFileSource? = null
            var previewFile: java.io.File? = null
            try {
                val cache = pcmCacheFile
                if (cache != null && cache.exists() && warpEngine == WarpEngine.WSOLA) {
                    sourceReader = withContext(Dispatchers.IO) { Pcm16WavFileSource(cache) }
                    val frameSource = sourceReader!!
                    val clipLength = minOf(8.0, frameSource.frameCount.toDouble() / frameSource.sampleRate)
                    val correctedDuration = markers.lastOrNull()?.targetSeconds ?: (frameSource.frameCount.toDouble() / frameSource.sampleRate)
                    val selectedTarget = abStartSeconds.toDouble().coerceIn(0.0, (correctedDuration - clipLength).coerceAtLeast(0.0))
                    val mappedStart = if (markers.size >= 2) AbTimeline.sourceAt(selectedTarget, markers) else selectedTarget
                    val start = mappedStart.coerceIn(0.0, (frameSource.frameCount.toDouble() / frameSource.sampleRate - clipLength).coerceAtLeast(0.0))
                    if (corrected) {
                        previewFile = withContext(Dispatchers.IO) { java.io.File.createTempFile("danim-ab-", ".wav", ctx.cacheDir) }
                        val events = result?.kickTimes.orEmpty().map { TransientEvent(it, TransientKind.KICK, 0.9) } + result?.otherTransients.orEmpty()
                        withContext(Dispatchers.IO) {
                            val correctedDuration = markers.maxOf { it.targetSeconds }
                            val correctedLength = minOf(8.0, correctedDuration)
                            val correctedStart = abStartSeconds.toDouble().coerceIn(0.0, (correctedDuration - correctedLength).coerceAtLeast(0.0))
                            previewFile!!.outputStream().buffered().use {
                                StreamingWsolaProcessor.renderWav(frameSource, markers, it, warpQuality, events,
                                    previewStartSeconds = correctedStart, previewDurationSeconds = correctedLength)
                            }
                        }
                        renderedReader = withContext(Dispatchers.IO) { Pcm16WavFileSource(previewFile!!) }
                        val renderedDuration = renderedReader!!.frameCount.toDouble() / renderedReader!!.sampleRate
                        val correctedLength = minOf(8.0, renderedDuration)
                        status = "A/B corregido streaming · ${fmt(selectedTarget, 2)}–${fmt(selectedTarget + correctedLength, 2)} s"
                        withContext(Dispatchers.IO) { StreamingPreview.playRange(renderedReader!!, 0.0, renderedDuration) }
                    } else {
                        status = "A/B original streaming · ${fmt(start, 2)}–${fmt(start + clipLength, 2)} s"
                        withContext(Dispatchers.IO) { StreamingPreview.playRange(frameSource, start, start + clipLength) }
                    }
                } else {
                    val src = withContext(Dispatchers.IO) { cacheFile.inputStream().buffered().use { PcmWave.read(it) } }
                    val out = if (corrected) warpedCache ?: withContext(Dispatchers.Default) { AudioWarpEngine.render(src, markers, (result?.kickTimes.orEmpty().map { TransientEvent(it, TransientKind.KICK, 0.9) } + result?.otherTransients.orEmpty()), warpEngine, warpQuality) }.also { warpedCache = it } else src
                    val clipLength = minOf(8.0, out.durationSeconds)
                    val start = abStartSeconds.toDouble().coerceIn(0.0, (out.durationSeconds - clipLength).coerceAtLeast(0.0))
                    status = if (corrected) "A/B corregido · ${fmt(start, 2)}–${fmt(start + clipLength, 2)} s" else "A/B original · ${fmt(start, 2)}–${fmt(start + clipLength, 2)} s"
                    withContext(Dispatchers.IO) { AudioPreview.playRange(out, start, start + clipLength) }
                }
                status = "Fragmento A/B finalizado"
            } catch (e: Exception) { status = "Error de reproducción: ${e.message}" }
            finally {
                renderedReader?.close(); sourceReader?.close(); previewFile?.delete(); playing = false
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = TEAL, secondary = ORANGE, background = BG, surface = CARD)) {
        Column(Modifier.fillMaxSize().background(BG).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(14.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("☰", color = Color.White, fontSize = 23.sp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DaniM", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Text("Auto Warp", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
                Surface(color = ORANGE, shape = RoundedCornerShape(7.dp)) {
                    Text("RC124", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Row(Modifier.fillMaxWidth().background(Color(0xFF0B1118), RoundedCornerShape(13.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("⌂\nInicio", "⇄\nProcesar", "▤\nLote", "⚙\nAjustes").forEachIndexed { i, label ->
                    val active = i == 0
                    Surface(modifier = Modifier.weight(1f), color = if (active) Color(0xFF45210A) else Color.Transparent, shape = RoundedCornerShape(10.dp), border = if (active) androidx.compose.foundation.BorderStroke(1.dp, ORANGE) else null) {
                        Text(label, Modifier.padding(vertical = 9.dp), color = if (active) ORANGE else MUTED, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                    }
                }
            }
            OutlinedButton(onClick = { ctx.startActivity(android.content.Intent(ctx, ReviewActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("REVISAR BOMBOS · ${result?.kickTimes?.size ?: 0} ATAQUES") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                DashboardMetric("BPM", result?.let { fmt(it.bpm, 2) } ?: "—", TEAL, Modifier.weight(1f))
                DashboardMetric("DETECCIÓN", result?.let { "${(it.confidence*100).roundToInt()}%" } ?: "—", Color.White, Modifier.weight(1f))
                DashboardMetric("BOMBOS", result?.kickTimes?.size?.toString() ?: "—", ORANGE, Modifier.weight(1f))
            }
            Card(colors = CardDefaults.cardColors(containerColor = CARD), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263544))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("♫  Importar audio", color = ORANGE, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(fileName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(status, color = MUTED, fontSize = 11.sp)
                    Button(onClick = { open.launch(arrayOf("audio/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = TEAL, contentColor = BG)) {
                        Text("♫  SELECCIONAR MP3 · WAV · AIFF", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.2.sp)
                    }
                    Text("WAV  ·  MP3  ·  AIFF PCM", color = Color(0xFF64788B), fontSize = 9.sp, letterSpacing = 1.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportProject.launch("DaniM_AutoWarp_project.json") }, enabled = pcmCacheFile != null && !busy, modifier = Modifier.weight(1f)) { Text("GUARDAR PROYECTO", fontSize = 10.sp) }
                        OutlinedButton(onClick = { importProject.launch(arrayOf("application/json", "text/plain", "*/*")) }, enabled = pcmCacheFile != null && !busy, modifier = Modifier.weight(1f)) { Text("ABRIR PROYECTO", fontSize = 10.sp) }
                    }
                    Text("Primero importa el audio original; después abre su proyecto .json. El proyecto no incluye audio.", color = MUTED, fontSize = 10.sp)
                }
            }
            // RC112: waveform preview uses decoded real audio peaks, never a simulated correction.
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF080C11)), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27313C))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Original (desfase)", color = Color(0xFFD1D5DC), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    ReferenceWaveform(waveformPeaks, Color(0xFFA9ADB5))
                    Text("Corregido (warp)", color = ORANGE, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (warpedCache != null) {
                        Text("Render corregido disponible para escucha A/B", color = MUTED, fontSize = 11.sp)
                    } else {
                        Box(Modifier.fillMaxWidth().height(49.dp).background(Color(0xFF10151C), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                            Text("Pendiente de renderizar · no se simula una onda corregida", color = MUTED, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { audition(false) }, enabled = audioInfo != null && !playing && !busy, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ORANGE, contentColor = Color.Black), shape = RoundedCornerShape(11.dp)) { Text("▶ Original", fontSize = 12.sp) }
                        OutlinedButton(onClick = { audition(true) }, enabled = audioInfo != null && markers.size >= 2 && !playing && !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp)) { Text("▶ Corregido", fontSize = 12.sp) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(onClick = {
                    val cacheFile = pcmCacheFile ?: return@Button
                    busy = true; status = "Analizando bombos, transitorios y variaciones…"
                    scope.launch {
                        try {
                            val r = withContext(Dispatchers.IO) {
                                Pcm16WavFileSource(cacheFile).use { BeatAnalyzer.analyze(it) }
                            }
                            result = r; transientOverrides = emptyMap(); markers = r.markers; warpedCache = null; undoStack = emptyList(); redoStack = emptyList()
                            status = "Análisis completado · ${r.kickTimes.size} bombos"
                        } catch (e: Exception) { status = "Error de análisis: ${e.message}" }
                        finally { busy = false }
                    }
                }, enabled = audioInfo != null && !busy, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = ORANGE, contentColor = Color.Black)) {
                    Text(if (busy) "PROCESANDO…" else "⚙  PROCESAR (AUTO)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                OutlinedButton(onClick = { manual = !manual }, enabled = !busy, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(13.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (manual) ORANGE else Color(0xFF405365))) {
                    Text(if (manual) "MANUAL ON" else "MANUAL", color = if (manual) ORANGE else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            // RC113: advanced dashboard is populated exclusively by real analysis results.
            result?.let { analysis ->
                var advancedTab by remember { mutableIntStateOf(0) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090D12)),
                    shape = RoundedCornerShape(17.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Text("Procesamiento avanzado", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Análisis", "Warp", "Beatgrid", "Exportar").forEachIndexed { tab, label ->
                                val selected = advancedTab == tab
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    onClick = { advancedTab = tab },
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (selected) ORANGE else Color(0xFF121820),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ORANGE else Color(0xFF27323D))
                                ) {
                                    Text(label, Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center,
                                        color = if (selected) Color.Black else Color.White, fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        when (advancedTab) {
                            0 -> {
                                Text("Seguimiento de velocidad (BPM)", color = Color.White, fontWeight = FontWeight.Bold)
                                AdvancedTempoGraph(if (analysis.barTempoCurve.size >= 2) analysis.barTempoCurve else analysis.tempoCurve)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DashboardMetric("BPM medio", fmt(analysis.bpm, 2), ORANGE, Modifier.weight(1f))
                                    DashboardMetric("Variación", "±${fmt(if (analysis.bpm > 0.0) 100.0 * maxOf(abs(analysis.tempoMaxBpm-analysis.bpm), abs(analysis.bpm-analysis.tempoMinBpm)) / analysis.bpm else 0.0, 2)} %", Color.White, Modifier.weight(1f))
                                }
                                Text("Detección de transitorios", color = Color.White, fontWeight = FontWeight.Bold)
                                AdvancedTransientGraph(waveformPeaks, analysis.kickTimes, analysis.otherTransients, audioInfo?.durationSeconds ?: 0.0)
                                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                                    listOf("Bombo" to Color(0xFFFF674C), "Caja" to Color(0xFF087FFF), "Percusión" to Color(0xFFFFB000)).forEach { (name, tint) ->
                                        Text("● $name", color = tint, fontSize = 10.sp)
                                    }
                                }
                                Text("La onda y los marcadores proceden del audio importado y del análisis.", color = MUTED, fontSize = 10.sp)
                            }
                            1 -> {
                                Text("Calidad de render", color = Color.White, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(Triple("Rápido", WarpQuality.FAST, "Render ágil"),
                                        Triple("Equilibrado", WarpQuality.BALANCED, "Calidad media"),
                                        Triple("Alta calidad", WarpQuality.HIGH, "Render detallado")).forEach { (label, quality, description) ->
                                        OutlinedButton(onClick = { warpQuality = quality; warpedCache = null },
                                            modifier = Modifier.weight(1f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (warpQuality == quality) ORANGE else Color(0xFF35404B)),
                                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 10.dp)) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(label, color = if (warpQuality == quality) ORANGE else Color.White, fontSize = 11.sp)
                                                Text(description, color = MUTED, fontSize = 8.sp, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                                Text("El modo cambia la calidad del motor de render; no modifica los marcadores automáticamente.", color = MUTED, fontSize = 10.sp)
                            }
                            2 -> {
                                Text("Beatgrid detectado: ${analysis.beatTimes.size} pulsos", color = Color.White)
                                Text("Marcadores de warp: ${markers.size}", color = MUTED)
                                Text("Ajusta los marcadores en el editor manual situado más abajo.", color = MUTED, fontSize = 11.sp)
                            }
                            else -> {
                                Text("Exportación de audio", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Utiliza los controles de exportación inferiores para renderizar y guardar el resultado real.", color = MUTED, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            result?.let { r ->
                SectionTitle("ANÁLISIS DE TEMPO", "TRACKING")
                Card(colors = CardDefaults.cardColors(containerColor = CARD), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263544))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("TEMPO MEDIO", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(fmt(r.bpm, 2), color = TEAL, fontSize = 34.sp, fontWeight = FontWeight.Black)
                                    Text(" BPM", color = MUTED, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CONFIANZA", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${(r.confidence * 100).roundToInt()}%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("${r.kickTimes.size} kicks", color = MUTED, fontSize = 10.sp)
                            }
                        }
                        Divider(color = Color(0xFF293847))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Metric("TEMPO MÍN.", "${fmt(r.tempoMinBpm)} BPM")
                            Metric("TEMPO MÁX.", "${fmt(r.tempoMaxBpm)} BPM")
                            Metric("OTROS HITS", r.otherTransients.size.toString())
                        }
                        Text("Desviación máx. frente al grid constante: ${fmt(r.microTimingMs.maxOfOrNull { abs(it) } ?: 0.0)} ms", color = MUTED, fontSize = 10.sp)
                        Text("FASE DE COMPÁS · 4/4", color = MUTED, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(if (r.downbeatConfidence >= 0.12) "Posible tiempo 1: pulso ${r.downbeatBeatIndex + 1}/4 · confianza ${fmt(r.downbeatConfidence * 100, 0)}%" else "Primer tiempo no concluyente · revisa la fase manualmente", color = if (r.downbeatConfidence >= 0.12) TEAL else ORANGE, fontSize = 10.sp)
                        Text("TEMPO POR COMPÁS · 4 BEATS", color = MUTED, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        TempoCurve(if (r.barTempoCurve.isNotEmpty()) r.barTempoCurve else r.tempoCurve)
                    }
                }
            }
            result?.diagnostics?.let { d ->
                SectionTitle("DIAGNÓSTICO AUTO WARP", "CONTROL DE CALIDAD")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showPhaseZones, onCheckedChange = { showPhaseZones = it })
                    Text("Mostrar zonas de fase sobre la onda · violeta: fiable; naranja: revisar", color = MUTED, fontSize = 10.sp)
                }
                Card(colors = CardDefaults.cardColors(containerColor = CARD), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263544))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("INFORME DE DETECCIÓN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (d.warnings.isEmpty()) "Sin alertas del analizador" else "${d.warnings.size} advertencia(s) para revisar", color = if (d.warnings.isEmpty()) TEAL else ORANGE, fontSize = 10.sp)
                            }
                            TextButton(onClick = { diagnosticsExpanded = !diagnosticsExpanded }) {
                                Text(if (diagnosticsExpanded) "OCULTAR" else "DETALLES", color = TEAL, fontSize = 11.sp)
                            }
                        }
                        if (diagnosticsExpanded) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Metric("BOMBOS", d.detectedKicks.toString())
                                Metric("CAJAS", d.detectedSnares.toString())
                                Metric("HI-HATS", d.detectedHats.toString())
                                Metric("OTROS", d.detectedOtherPercussion.toString())
                            }
                            HorizontalDivider(color = Color(0xFF293847))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Metric("WARP ACEPTADOS", d.acceptedWarpMarkers.toString())
                                Metric("DESCARTADOS", d.rejectedWarpMarkers.toString())
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Metric("DESFASE MEDIANO", "${fmt(d.medianAbsoluteTimingMs)} ms")
                                Metric("DESFASE MÁX.", "${fmt(d.maximumAbsoluteTimingMs)} ms")
                                Metric("RANGO BPM", fmt(d.tempoVariationBpm, 2))
                            }
                            result?.kickPhaseSegments?.let { report ->
                                Text("FASE RÍTMICA · DIAGNÓSTICO (NO AUTORIZA WARP)", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${result?.kickPhaseWindows?.count { it.reliable } ?: 0}/${result?.kickPhaseWindows?.size ?: 0} ventanas fiables · ${report.segments.size} segmentos · ${report.discontinuities} saltos · ${report.rejectedWindows} ventanas dudosas", color = ORANGE, fontSize = 10.sp)
                                report.segments.take(5).forEach { segment ->
                                    Text("Segmento ${fmt(segment.startSeconds, 1)}–${fmt(segment.endSeconds, 1)} s · salto máx. ${fmt(segment.maximumPhaseJumpMs, 1)} ms", color = MUTED, fontSize = 10.sp)
                                }
                            }
                            result?.confidenceZones?.takeIf { it.isNotEmpty() }?.let { zones ->
                                Text("FIABILIDAD POR ZONAS · ESTIMACIÓN", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    zones.forEach { zone ->
                                        val tint = if (zone.requiresReview) ORANGE else TEAL
                                        Box(Modifier.weight(1f).height(18.dp).background(tint.copy(alpha = (0.25 + zone.score * 0.75).toFloat()), RoundedCornerShape(2.dp)))
                                    }
                                }
                                val weak = zones.filter { it.requiresReview }
                                Text("${weak.size} de ${zones.size} zonas necesitan revisión. Naranja: revisar; violeta: mejor soporte rítmico.", color = MUTED, fontSize = 10.sp)
                                weak.take(4).forEach { zone ->
                                    Text("Revisar ${fmt(zone.startSeconds, 0)}–${fmt(zone.endSeconds, 0)} s · soporte ${zone.supportedBeats}/${zone.totalBeats} pulsos", color = ORANGE, fontSize = 10.sp)
                                }
                            }
                            d.warnings.forEach { warning ->
                                Text("⚠ $warning", color = ORANGE, fontSize = 11.sp)
                            }
                            Text("Los recuentos y avisos son estimaciones; comprueba los golpes y el resultado mediante A/B.", color = MUTED, fontSize = 10.sp)
                        }
                    }
                }
            }
            SectionTitle("MOTOR DE PROCESAMIENTO", if (warpEngine == WarpEngine.WSOLA) "TRANSIENT-LOCK WSOLA" else "PHASE VOCODER")
            Card(colors = CardDefaults.cardColors(containerColor = CARD), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263544))) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ALGORITMO DE TIME-STRETCH", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(WarpEngine.WSOLA, WarpEngine.PHASE_VOCODER).forEachIndexed { index, engine ->
                            SegmentedButton(selected = warpEngine == engine, onClick = { warpEngine = engine; warpedCache = null }, shape = SegmentedButtonDefaults.itemShape(index, 2)) {
                                Text(if (engine == WarpEngine.WSOLA) "WSOLA · PERCUSIVO" else "PHASE VOCODER · TONAL", fontSize = 8.sp)
                            }
                        }
                    }
                    Text(if (warpEngine == WarpEngine.WSOLA) "Recomendado para techno, bombos, cajas y material con transitorios marcados." else "Alternativa para pads, sintetizadores y colas sostenidas. Puede suavizar ataques; compara siempre con WSOLA.", color = if (warpEngine == WarpEngine.WSOLA) TEAL else ORANGE, fontSize = 10.sp)
                    Text("CALIDAD DE RENDER", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(WarpQuality.FAST, WarpQuality.BALANCED, WarpQuality.HIGH).forEachIndexed { index, quality ->
                            SegmentedButton(selected = warpQuality == quality, onClick = { warpQuality = quality; warpedCache = null }, shape = SegmentedButtonDefaults.itemShape(index, 3)) {
                                Text(when (quality) { WarpQuality.FAST -> "RÁPIDO"; WarpQuality.BALANCED -> "EQUILIBRADO"; WarpQuality.HIGH -> "ALTA" }, fontSize = 9.sp)
                            }
                        }
                    }
                    Text(when (warpQuality) {
                        WarpQuality.FAST -> "Menor tiempo de cálculo; búsqueda de alineación más corta."
                        WarpQuality.BALANCED -> "Equilibrio recomendado entre calidad y velocidad."
                        WarpQuality.HIGH -> "Ventanas y búsqueda más detalladas; render más lento."
                    }, color = MUTED, fontSize = 10.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("EXPORTACIÓN WSOLA STREAMING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("WSOLA por granos, transitorios anclados y WAV progresivo desde caché PCM. El modo Phase Vocoder utiliza carga temporal en RAM.", color = MUTED, fontSize = 9.sp)
                        }
                        Switch(checked = streamingExport, enabled = warpEngine == WarpEngine.WSOLA,
                            onCheckedChange = { streamingExport = it; warpedCache = null })
                    }
                    Text(if (warpEngine == WarpEngine.WSOLA) "Ambos motores WSOLA respetan ataques detectados de alta confianza; el streaming mantiene alineación estéreo y estado de solapamiento entre granos." else "Phase vocoder STFT con phase locking por picos, reinicio ante transitorios, fase estéreo enlazada y mezcla de intensidad por zona.", color = TEAL, fontSize = 10.sp)
                    result?.let { analysis ->
                    val snares = analysis.otherTransients.count { it.kind == TransientKind.SNARE }
                    val hats = analysis.otherTransients.count { it.kind == TransientKind.HI_HAT }
                    val perc = analysis.otherTransients.count { it.kind == TransientKind.PERCUSSION }
                    Text("CAJA $snares   ·   HI-HAT $hats   ·   PERC $perc", color = MUTED, fontSize = 9.sp)
                }
                Text(renderInfo, color = MUTED, fontSize = 9.sp)
                }
            }
            SectionTitle("WAVEFORM & WARP GRID", if (manual) "EDICIÓN MANUAL" else "AUTO ALIGN")
            Card(colors = CardDefaults.cardColors(containerColor = CARD), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263544))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WaveGrid(audioInfo, waveformPeaks, markers, result?.let { r ->
                        val windows = r.kickPhaseWindows
                        if (windows.isEmpty()) null else {
                            val w = windows[selectedPhaseWindow.coerceIn(0, windows.lastIndex)]
                            val kicks = PhaseWindowNavigator.nearbyKicks(r.kickTimes, w)
                            val bpm = manualGridBpm.replace(',', '.').toDoubleOrNull()
                            val phase = manualGridPhaseMs.replace(',', '.').toDoubleOrNull()
                            if (kicks.isEmpty() || bpm == null || phase == null) null
                            else ManualGridPreview.inspect(markers, kicks[selectedKickInWindow.coerceIn(0, kicks.lastIndex)], bpm, phase)
                        }
                    }, result?.kickPhaseWindows.orEmpty(), showPhaseZones, selectedPhaseWindow, onPhaseSelect = { selectedPhaseWindow = it }, result?.let { r -> (r.kickTimes.map { TransientEvent(it, TransientKind.KICK, 0.9) } + r.otherTransients).map { e -> e.copy(kind = transientOverrides[e.seconds] ?: e.kind) } }.orEmpty(), showTransients, showTargetGrid, manual, waveZoom, waveStart, selectedMarkerIndex, onSelect = { selectedMarkerIndex = it }, onEditStart = { beginMarkerEdit() }) { index, newSource ->
                        if (pcmCacheFile == null || index !in 1 until markers.lastIndex) return@WaveGrid
                        val list = markers.toMutableList(); val old = list[index]
                        val lo = if (index == 0) 0.0 else list[index - 1].sourceSeconds + 0.001
                        val hi = if (index == list.lastIndex) (audioInfo?.durationSeconds ?: old.sourceSeconds) else list[index + 1].sourceSeconds - 0.001
                        val bounded = newSource.coerceIn(lo, hi)
                        if (kotlin.math.abs(bounded - old.sourceSeconds) < 0.000001) return@WaveGrid
                        list[index] = old.copy(sourceSeconds = bounded)
                        markers = list; warpedCache = null
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TRANSITORIOS", color = MUTED, fontSize = 10.sp)
                        Switch(checked = showTransients, onCheckedChange = { showTransients = it })
                        Spacer(Modifier.weight(1f))
                        Dot(ORANGE); Text("BOMBO", color = MUTED, fontSize = 8.sp)
                        Dot(PINK); Text("CAJA", color = MUTED, fontSize = 8.sp)
                        Dot(Color(0xFF4EA5FF)); Text("HI-HAT", color = MUTED, fontSize = 8.sp)
                    }
                    result?.let { r ->
                        val windows = r.kickPhaseWindows
                        if (windows.isNotEmpty()) {
                            val chosen = selectedPhaseWindow.coerceIn(0, windows.lastIndex)
                            val w = windows[chosen]
                            val kicks = PhaseWindowNavigator.nearbyKicks(r.kickTimes, w)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { selectedPhaseWindow = (chosen - 1).coerceAtLeast(0) }) { Text("◀", fontSize = 10.sp) }
                                Column(Modifier.weight(1f)) {
                                    Text("VENTANA ${chosen + 1}/${windows.size} · ${if (w.reliable) "FASE FIABLE" else "REVISAR"}", color = if (w.reliable) TEAL else ORANGE, fontSize = 10.sp)
                                    Text("${fmt(w.startSeconds, 1)}–${fmt(w.endSeconds, 1)} s · ${kicks.size} bombos · soporte ${fmt(w.supportRatio * 100, 0)}%", color = MUTED, fontSize = 9.sp)
                                }
                                OutlinedButton(onClick = { selectedPhaseWindow = (chosen + 1).coerceAtMost(windows.lastIndex) }) { Text("▶", fontSize = 10.sp) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    val duration = audioInfo?.durationSeconds ?: 0.0
                                    if (duration > 0.0) {
                                        waveZoom = (duration / (w.endSeconds - w.startSeconds).coerceAtLeast(1.0) * 1.15).toFloat().coerceIn(1f, 64f)
                                        waveStart = PhaseWindowNavigator.viewport(w, duration, waveZoom.toDouble()).toFloat()
                                    }
                                }, modifier = Modifier.weight(1f)) { Text("ENFOCAR ZONA", fontSize = 9.sp) }
                                OutlinedButton(onClick = {
                                    val next = ((chosen + 1)..windows.lastIndex).firstOrNull { !windows[it].reliable }
                                        ?: (0 until chosen).firstOrNull { !windows[it].reliable }
                                    if (next != null) selectedPhaseWindow = next
                                }, enabled = windows.any { !it.reliable }, modifier = Modifier.weight(1f)) { Text("SIGUIENTE DUDA", fontSize = 9.sp) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("AJUSTE FASE", color = MUTED, fontSize = 9.sp)
                                listOf(1, 5, 10).forEach { step ->
                                    OutlinedButton(onClick = { nudgeStepMs = step },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (nudgeStepMs == step) ORANGE else MUTED),
                                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)) {
                                        Text("${step}ms", fontSize = 9.sp)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(-1, 1).forEach { direction ->
                                    OutlinedButton(onClick = {
                                        val adjustment = ManualPhaseNudge.apply(markers, w.startSeconds, w.endSeconds, direction * nudgeStepMs)
                                        if (adjustment.accepted) {
                                            commitMarkers(adjustment.markers)
                                            manual = true
                                            status = "Fase ajustada ${direction * nudgeStepMs} ms en ${adjustment.changed} marcadores. Deshacer disponible."
                                        } else status = "Ajuste rechazado: ${adjustment.reason}"
                                    }, modifier = Modifier.weight(1f)) {
                                        Text(if (direction < 0) "FASE −" else "FASE +", fontSize = 10.sp)
                                    }
                                }
                                OutlinedButton(onClick = { undoEdit() }) { Text("DESHACER", fontSize = 9.sp) }
                            }
                            Text("Solo modifica marcadores existentes dentro de la ventana. Límites de estiramiento: 0,85–1,15×.", color = MUTED, fontSize = 9.sp)
                            if (kicks.isNotEmpty()) {
                                val kickIndex = selectedKickInWindow.coerceIn(0, kicks.lastIndex)
                                val candidate = kicks[kickIndex]
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { selectedKickInWindow = (kickIndex - 1).coerceAtLeast(0) }) { Text("◀", fontSize = 10.sp) }
                                    Text("BOMBO ${kickIndex + 1}/${kicks.size}: ${fmt(candidate, 3)} s", color = ORANGE, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                    OutlinedButton(onClick = { selectedKickInWindow = (kickIndex + 1).coerceAtMost(kicks.lastIndex) }) { Text("▶", fontSize = 10.sp) }
                                }
                                OutlinedButton(onClick = {
                                    // The button is the user's explicit confirmation of the selected candidate.
                                    val inserted = ManualKickAnchor.insert(markers, candidate, confirmedByUser = true)
                                    if (inserted.accepted) {
                                        commitMarkers(inserted.markers)
                                        selectedMarkerIndex = inserted.markers.indexOfFirst { kotlin.math.abs(it.sourceSeconds - candidate) < 0.000001 }
                                        manual = true
                                        status = "Bombo confirmado: marcador neutro añadido en ${fmt(candidate, 3)} s. Ajusta la fase manualmente; puedes deshacer."
                                    } else status = "Marcador rechazado: ${inserted.reason}"
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("CONFIRMAR BOMBO Y AÑADIR MARCADOR", fontSize = 10.sp)
                                }
                                Text("CUADRÍCULA BPM MANUAL", color = TEAL, fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = manualGridBpm, onValueChange = { manualGridBpm = it },
                                        label = { Text("BPM", fontSize = 10.sp) }, singleLine = true,
                                        modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = manualGridPhaseMs, onValueChange = { manualGridPhaseMs = it },
                                        label = { Text("Fase (ms)", fontSize = 10.sp) }, singleLine = true,
                                        modifier = Modifier.weight(1f))
                                }
                                val previewBpm = manualGridBpm.replace(',', '.').toDoubleOrNull()
                                val previewPhase = manualGridPhaseMs.replace(',', '.').toDoubleOrNull()
                                val gridPreview = if (previewBpm != null && previewPhase != null)
                                    ManualGridPreview.inspect(markers, candidate, previewBpm, previewPhase)
                                else null
                                if (gridPreview?.accepted == true) {
                                    Text("VISTA PREVIA · Origen ${fmt(candidate, 3)} s → destino ${fmt(gridPreview.proposedTargetSeconds ?: candidate, 3)} s",
                                        color = TEAL, fontSize = 10.sp)
                                    Text("LÍNEAS EN ONDA: naranja = origen · blanco = destino (amplía el zoom para distinguirlas)", color = MUTED, fontSize = 9.sp)
                                    Text("Desplazamiento ${fmt(gridPreview.shiftMs ?: 0.0, 1)} ms · estiramiento anterior ${fmt(gridPreview.leftStretchPercent ?: 0.0, 2)} % · posterior ${fmt(gridPreview.rightStretchPercent ?: 0.0, 2)} %",
                                        color = MUTED, fontSize = 9.sp)
                                } else {
                                    Text("Vista previa no aplicable: ${gridPreview?.reason ?: "Introduce BPM y fase válidos"}", color = ORANGE, fontSize = 9.sp)
                                }
                                OutlinedButton(onClick = {
                                    val bpm = manualGridBpm.replace(',', '.').toDoubleOrNull()
                                    val phase = manualGridPhaseMs.replace(',', '.').toDoubleOrNull()
                                    if (bpm == null || phase == null) status = "Introduce BPM y fase numéricos"
                                    else {
                                        val aligned = ManualBeatGrid.align(markers, candidate, bpm, phase, confirmedByUser = true)
                                        if (aligned.accepted) {
                                            commitMarkers(aligned.markers)
                                            selectedMarkerIndex = aligned.markers.indexOfFirst { kotlin.math.abs(it.sourceSeconds - candidate) < 0.000001 }
                                            manual = true
                                            status = "Bombo alineado a ${fmt(aligned.targetSeconds ?: 0.0, 3)} s en cuadrícula ${fmt(bpm, 2)} BPM. Deshacer disponible."
                                        } else status = "Cuadrícula rechazada: ${aligned.reason}"
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("CONFIRMAR Y ALINEAR BOMBO A BPM", fontSize = 10.sp) }
                                Text("La cuadrícula utiliza el inicio de la pista más el desfase configurado. No cambia otros marcadores y rechaza estiramientos fuera de 0,85–1,15×.", color = MUTED, fontSize = 9.sp)
                                Text("El marcador nuevo conserva la sincronía actual; no corrige por sí solo el beatgrid. Comprueba el bombo escuchándolo.", color = MUTED, fontSize = 9.sp)
                            }
                            Text("Bombos detectados en esta ventana: " + if (kicks.isEmpty()) "ninguno" else kicks.take(8).joinToString(" · ") { fmt(it, 3) + "s" } + if (kicks.size > 8) " …" else "", color = MUTED, fontSize = 9.sp)
                            Text("Diagnóstico solamente: seleccionar una zona no crea marcadores ni autoriza el Warp.", color = MUTED, fontSize = 9.sp)
                        }
                    }
                    result?.let { r ->
                        val all = (r.kickTimes.map { TransientEvent(it, TransientKind.KICK, 0.9) } + r.otherTransients)
                            .filter { it.seconds >= waveStart && it.seconds <= waveStart + (audioInfo?.durationSeconds ?: 0.0) / waveZoom }
                            .sortedBy { it.seconds }
                        if (all.isNotEmpty()) {
                            var selectedTransient by remember(r) { mutableIntStateOf(0) }
                            val index = selectedTransient.coerceIn(0, all.lastIndex)
                            val event = all[index]
                            val kind = transientOverrides[event.seconds] ?: event.kind
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { selectedTransient = (index - 1).coerceAtLeast(0) }) { Text("◀", fontSize = 10.sp) }
                                Column(Modifier.weight(1f)) {
                                    Text("${fmt(event.seconds, 3)} s · ${kind.name}", color = Color.White, fontSize = 10.sp)
                                    Text("${index + 1}/${all.size} · Etiqueta editable", color = MUTED, fontSize = 9.sp)
                                }
                                OutlinedButton(onClick = { selectedTransient = (index + 1).coerceAtMost(all.lastIndex) }) { Text("▶", fontSize = 10.sp) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                TransientKind.entries.forEach { option ->
                                    OutlinedButton(onClick = {
                                        transientOverrides = transientOverrides + (event.seconds to option)
                                        status = "Clasificación guardada. Pulsa RECALCULAR CON BOMBOS para actualizar el beatgrid"
                                    }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp)) {
                                        Text(when(option) { TransientKind.KICK -> "BOMBO"; TransientKind.SNARE -> "CAJA"; TransientKind.HI_HAT -> "HAT"; TransientKind.PERCUSSION -> "PERC" }, fontSize = 8.sp, color = if (option == kind) ORANGE else MUTED)
                                    }
                                }
                            }
                            OutlinedButton(onClick = {
                                val duration = audioInfo?.durationSeconds ?: 0.0
                                val corrected = (r.kickTimes.map { TransientEvent(it, TransientKind.KICK, 0.9) } + r.otherTransients)
                                    .map { it.copy(kind = transientOverrides[it.seconds] ?: it.kind) }
                                val updated = ConfirmedKickWarp.rebuild(r, corrected, duration)
                                if (updated.matched >= 3) {
                                    commitMarkers(updated.markers)
                                    status = "Beatgrid recalculado: ${updated.matched} bombos vinculados, ${updated.rejected} anclajes descartados"
                                } else status = "Se necesitan al menos 3 bombos confirmados próximos a la cuadrícula"
                            }, enabled = !busy && r.beatTimes.size >= 3, modifier = Modifier.fillMaxWidth()) {
                                Text("RECALCULAR CON BOMBOS", fontSize = 10.sp)
                            }
                            Text("Las etiquetas se guardan con el proyecto. Recalcular utiliza solo los bombos clasificados y conserva los límites de seguridad.", color = MUTED, fontSize = 9.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ZOOM", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Slider(value = waveZoom, onValueChange = { waveZoom = it; val d = audioInfo?.durationSeconds ?: 0.0; waveStart = waveStart.coerceIn(0f, (d - d / it).coerceAtLeast(0.0).toFloat()) }, valueRange = 1f..64f, modifier = Modifier.weight(1f))
                        Text("${fmt(waveZoom.toDouble(), 1)}×", color = TEAL, fontSize = 10.sp)
                    }
                    if (waveZoom > 1.01f && audioInfo != null) {
                        val duration = audioInfo!!.durationSeconds.toFloat()
                        val visible = duration / waveZoom
                        Slider(value = waveStart, onValueChange = { waveStart = it }, valueRange = 0f..(duration - visible).coerceAtLeast(0f), modifier = Modifier.fillMaxWidth())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { undoEdit() }, enabled = undoStack.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("↶ DESHACER", fontSize = 10.sp) }
                        OutlinedButton(onClick = { redoEdit() }, enabled = redoStack.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("↷ REHACER", fontSize = 10.sp) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Dot(TEAL); Text("FUENTE", color = MUTED, fontSize = 9.sp); Dot(PINK); Text("DESTINO", color = MUTED, fontSize = 9.sp)
                        Spacer(Modifier.weight(1f)); Text("${markers.size} PUNTOS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(if (manual) "Arrastra los marcadores turquesa para ajustar el origen temporal." else "Anclajes automáticos de bombo con conservación del inicio y final del audio.", color = MUTED, fontSize = 10.sp)
                    if (markers.size > 2) {
                        val editable = (1 until markers.lastIndex).toList()
                        val activeMarkerIndex = selectedMarkerIndex.coerceIn(editable.first(), editable.last())
                        val selected = markers[activeMarkerIndex]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ZONA ${activeMarkerIndex + 1}", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${(selected.strength * 100).roundToInt()}% WARP", color = TEAL, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("ORIGEN ${fmt(selected.sourceSeconds, 3)} s  ·  DESTINO ${fmt(selected.targetSeconds, 3)} s  ·  DESFASE ${fmt((selected.targetSeconds - selected.sourceSeconds) * 1000, 1)} ms", color = MUTED, fontSize = 10.sp)
                        if (manual) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("PASO", color = MUTED, fontSize = 9.sp)
                                listOf(1, 10, 50).forEach { step ->
                                    FilterChip(selected = nudgeStepMs == step, onClick = { nudgeStepMs = step }, label = { Text("${step} ms", fontSize = 9.sp) })
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    val t = waveStart.toDouble() + (audioInfo!!.durationSeconds / waveZoom) / 2.0
                                    val updated = PrecisionWarpEditor.insert(markers, t)
                                    if (updated != markers) { commitMarkers(updated); selectedMarkerIndex = updated.indexOfFirst { abs(it.sourceSeconds - t) < 0.000001 } }
                                }, modifier = Modifier.weight(1f)) { Text("+ MARCADOR", fontSize = 9.sp) }
                                OutlinedButton(onClick = {
                                    val updated = PrecisionWarpEditor.remove(markers, activeMarkerIndex)
                                    if (updated != markers) { commitMarkers(updated); selectedMarkerIndex = (activeMarkerIndex - 1).coerceAtLeast(1) }
                                }, modifier = Modifier.weight(1f)) { Text("− MARCADOR", fontSize = 9.sp) }
                                OutlinedButton(onClick = {
                                    val candidates = result?.let { r -> r.kickTimes + r.otherTransients.map { it.seconds } }.orEmpty()
                                    commitMarkers(PrecisionWarpEditor.snap(markers, activeMarkerIndex, candidates, 50.0))
                                }, modifier = Modifier.weight(1f)) { Text("IMÁN 50 ms", fontSize = 9.sp) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { commitMarkers(PrecisionWarpEditor.nudgeSource(markers, activeMarkerIndex, -nudgeStepMs.toDouble())) }, modifier = Modifier.weight(1f)) { Text("− ${nudgeStepMs} ms", fontSize = 10.sp) }
                                OutlinedButton(onClick = { commitMarkers(PrecisionWarpEditor.nudgeSource(markers, activeMarkerIndex, nudgeStepMs.toDouble())) }, modifier = Modifier.weight(1f)) { Text("+ ${nudgeStepMs} ms", fontSize = 10.sp) }
                            }
                        }
                        Slider(value = selected.strength, onValueChange = { value ->
                            val changed = WarpZoneTools.setMarkerStrength(markers, activeMarkerIndex, value)
                            if (changed != markers) commitMarkers(changed)
                        }, valueRange = 0f..1f)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { selectedMarkerIndex = (activeMarkerIndex - 1).coerceAtLeast(1) }, modifier = Modifier.weight(1f)) { Text("◀ ZONA", fontSize = 9.sp) }
                            OutlinedButton(onClick = { selectedMarkerIndex = (activeMarkerIndex + 1).coerceAtMost(markers.lastIndex - 1) }, modifier = Modifier.weight(1f)) { Text("ZONA ▶", fontSize = 9.sp) }
                            OutlinedButton(onClick = { commitMarkers(WarpZoneTools.setZoneStrength(markers, 1, markers.lastIndex - 1, 1f)) }, modifier = Modifier.weight(1f)) { Text("100%", fontSize = 9.sp) }
                        }
                    }
                }
            }
            SectionTitle("MONITORIZACIÓN", "A / B · FRAGMENTO SINCRONIZADO")
            Card(colors = CardDefaults.cardColors(containerColor = CARD), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263544))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val duration = audioInfo?.durationSeconds ?: 0.0
                    val clipLength = minOf(8.0, duration)
                    val maxStart = (duration - clipLength).coerceAtLeast(0.0).toFloat()
                    Text("INICIO DEL FRAGMENTO · ${fmt(abStartSeconds.toDouble(), 2)} s", color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Slider(value = abStartSeconds.coerceIn(0f, maxStart), onValueChange = { abStartSeconds = it }, valueRange = 0f..maxStart.coerceAtLeast(0.001f), enabled = audioInfo != null && maxStart > 0.001f)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val bpm = result?.bpm ?: 0.0
                        listOf(-4 to "◀ COMPÁS", -1 to "◀ BEAT", 1 to "BEAT ▶", 4 to "COMPÁS ▶").forEach { (beats, label) ->
                            OutlinedButton(
                                onClick = { abStartSeconds = BeatNavigation.shift(abStartSeconds.toDouble(), bpm, beats, maxStart.toDouble()).toFloat() },
                                enabled = audioInfo != null && bpm > 0.0 && !playing,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp)
                            ) { Text(label, fontSize = 8.sp) }
                        }
                    }
                    Text("A/B: correspondencia temporal por marcadores. En modo continuo se cambia sin reiniciar AudioTrack (no garantiza precisión de muestras).", color = MUTED, fontSize = 10.sp)
                }
            }
            if (warpEngine == WarpEngine.WSOLA) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { auditionContinuous() }, enabled = audioInfo != null && markers.size >= 2 && warpEngine == WarpEngine.WSOLA && !playing && !busy, modifier = Modifier.weight(1f)) { Text("▶ A/B CONTINUO", fontSize = 10.sp) }
                        OutlinedButton(onClick = { abCorrected = !abCorrected; ContinuousAbPreview.selectCorrected(abCorrected) }, enabled = playing && continuousAb, modifier = Modifier.weight(1f)) {
                            Text(if (abCorrected) "CORREGIDO" else "ORIGINAL", fontSize = 10.sp)
                        }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { audition(false) }, enabled = audioInfo != null && !playing && !busy, modifier = Modifier.weight(1f).height(45.dp), shape = RoundedCornerShape(12.dp)) { Text("▶ ORIGINAL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = { audition(true) }, enabled = audioInfo != null && markers.size >= 2 && !playing && !busy, modifier = Modifier.weight(1f).height(45.dp), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TEAL)) { Text("▶ CORREGIDO", color = TEAL, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = { AudioPreview.stop(); StreamingPreview.stop(); ContinuousAbPreview.stop(); playing = false; status = "Reproducción detenida" }, enabled = playing, modifier = Modifier.height(45.dp), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, PINK)) { Text("■", color = PINK) }
            }
            Text("FORMATO DE EXPORTACIÓN", color = MUTED, fontSize = 11.sp)
            HighResolutionAudioExport.Format.entries.chunked(2).forEach { formats ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    formats.forEach { format ->
                        FilterChip(selected = outputFormat == format, onClick = { outputFormat = format },
                            label = { Text(format.label, fontSize = 10.sp) })
                    }
                }
            }
            Text("WAV/AIFF de 24/32 bits parten de PCM16. FLAC conserva exactamente ese PCM16 sin pérdida.", color = MUTED, fontSize = 10.sp)
            qualityReport?.let { report ->
                Card(colors = CardDefaults.cardColors(containerColor = CARD), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("CONTROL DE CALIDAD · ÚLTIMA EXPORTACIÓN", color = TEAL, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Pico: ${fmt(report.peak, 3)} · RMS: ${fmt(report.rms, 3)} · DC: ${fmt(report.dcOffset, 4)}", color = Color.White, fontSize = 11.sp)
                        Text("Muestras al límite: ${report.clippedSamples} · Saltos: ${report.discontinuities}", color = if (report.hasWarnings) ORANGE else MUTED, fontSize = 11.sp)
                        Text("Diferencia de duración: ${fmt(report.durationDifferenceMs, 1)} ms · Muestras finitas: ${if (report.finiteSamples) "sí" else "no"}", color = MUTED, fontSize = 10.sp)
                        if (report.hasWarnings) Text("Aviso: revisa el audio exportado mediante escucha A/B. Los saltos detectados no siempre son artefactos.", color = ORANGE, fontSize = 10.sp)
                        else Text("Sin alertas según los umbrales técnicos actuales. No garantiza ausencia de artefactos audibles.", color = MUTED, fontSize = 10.sp)
                    }
                }
            }
            Button(onClick = { save.launch("DaniM_AutoWarp.${outputFormat.extension}") }, enabled = audioInfo != null && markers.size >= 2 && !busy, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = ORANGE, contentColor = BG)) {
                Text("↓  EXPORTAR AUDIO CORREGIDO", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
            OutlinedButton(onClick = { saveGrid.launch("DaniM_Beatgrid.csv") }, enabled = result != null && result!!.beatTimes.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp)) {
                Text("⇩  EXPORTAR BEATGRID CSV", color = TEAL, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            if (markers.isNotEmpty()) {
                SectionTitle("PUNTOS DE CONTROL", "${markers.size} TOTAL")
                Card(colors = CardDefaults.cardColors(containerColor = CARD), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 6.dp)) {
                        markers.take(10).forEachIndexed { i, m ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}", color = TEAL, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                                Column(Modifier.weight(1f)) { Text("${fmt(m.sourceSeconds, 3)} s", color = Color.White, fontSize = 11.sp); Text("ORIGEN", color = MUTED, fontSize = 8.sp) }
                                Text("→", color = ORANGE, modifier = Modifier.padding(horizontal = 8.dp))
                                Column(Modifier.weight(1f)) { Text("${fmt(m.targetSeconds, 3)} s", color = TEAL, fontSize = 11.sp); Text("DESTINO", color = MUTED, fontSize = 8.sp) }
                                if (manual && i > 0 && i < markers.lastIndex) TextButton(onClick = { commitMarkers(markers.toMutableList().also { it.removeAt(i) }) }) { Text("×", color = PINK) }
                            }
                            if (i < minOf(markers.size, 10) - 1) Divider(color = Color(0xFF253441))
                        }
                        if (markers.size > 10) Text("Mostrando 10 de ${markers.size} puntos", color = MUTED, fontSize = 9.sp, modifier = Modifier.padding(7.dp))
                    }
                }
            }
            Text("DANIM AUTO WARP  ·  PROCESAMIENTO LOCAL  ·  v3.6", color = Color(0xFF526477), fontSize = 8.sp, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun DashboardMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF101925)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263544))) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = MUTED, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Text(value, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable private fun SectionTitle(title: String, tag: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        Text(tag, color = TEAL, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}
@Composable private fun Metric(label: String, value: String) {
    Column { Text(label, color = MUTED, fontSize = 8.sp, fontWeight = FontWeight.Bold); Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}
@Composable private fun Dot(color: Color) { Box(Modifier.size(7.dp).background(color, CircleShape)) }
/** A compact real-peak waveform matching the reference's symmetrical silhouette. */
@Composable private fun ReferenceWaveform(peaks: WaveformPeaks?, tint: Color) {
    Canvas(Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF10151C), RoundedCornerShape(7.dp))) {
        val samples = peaks?.peaks ?: return@Canvas
        if (samples.isEmpty()) return@Canvas
        val width = size.width
        val mid = size.height / 2f
        val columns = (width / 2.5f).toInt().coerceIn(1, 4096)
        for (i in 0 until columns) {
            val from = i.toLong() * samples.size / columns
            val to = ((i + 1).toLong() * samples.size / columns).coerceAtMost(samples.size.toLong())
            var amplitude = 0f
            for (j in from.toInt() until maxOf(from.toInt() + 1, to.toInt()).coerceAtMost(samples.size)) {
                val sample = samples[j]
                if (sample.isFinite()) amplitude = maxOf(amplitude, kotlin.math.abs(sample))
            }
            val h = (amplitude.coerceIn(0f, 1f) * mid * .92f).coerceAtLeast(1f)
            val x = (i + .5f) / columns * width
            drawLine(tint, Offset(x, mid - h), Offset(x, mid + h), strokeWidth = 1.6.dp.toPx())
        }
        drawLine(Color(0xFFFFE1B6), Offset(width / 2f, 0f), Offset(width / 2f, size.height), strokeWidth = 1.dp.toPx())
    }
}

// RC114: plots the actual timeline, not the index of a potentially irregular sample.
@Composable private fun AdvancedTempoGraph(points: List<Pair<Double, Double>>) {
    Canvas(Modifier.fillMaxWidth().height(125.dp).background(Color(0xFF070A0E), RoundedCornerShape(9.dp))) {
        for (i in 0..4) {
            val y = size.height * i / 4f
            drawLine(Color(0xFF26323E), Offset(0f, y), Offset(size.width, y), 0.8.dp.toPx())
        }
        for (i in 0..8) {
            val x = size.width * i / 8f
            drawLine(Color(0xFF26323E), Offset(x, 0f), Offset(x, size.height), 0.8.dp.toPx())
        }
        val valid = points.filter { it.first.isFinite() && it.second.isFinite() && it.second > 0.0 }
            .sortedBy { it.first }
        if (valid.size >= 2) {
            val minTime = valid.first().first
            val duration = (valid.last().first - minTime).coerceAtLeast(0.001)
            val low = valid.minOf { it.second } - 0.3
            val span = (valid.maxOf { it.second } - low + 0.3).coerceAtLeast(0.6)
            for (i in 1 until valid.size) {
                val a = Offset(((valid[i-1].first-minTime)/duration).toFloat()*size.width,
                    size.height*(1f-((valid[i-1].second-low)/span).toFloat()))
                val b = Offset(((valid[i].first-minTime)/duration).toFloat()*size.width,
                    size.height*(1f-((valid[i].second-low)/span).toFloat()))
                drawLine(ORANGE, a, b, 1.7.dp.toPx())
            }
        }
    }
}

// RC115: real cached audio waveform, colored cyan/blue, with analyzed transient markers.
@Composable private fun AdvancedTransientGraph(
    waveform: WaveformPeaks?, kicks: List<Double>, others: List<TransientEvent>, duration: Double
) {
    Canvas(Modifier.fillMaxWidth().height(110.dp).background(Color(0xFF070A0E), RoundedCornerShape(9.dp))) {
        if (duration <= 0.0 || !duration.isFinite()) return@Canvas
        val mid = size.height / 2f
        val samples = waveform?.peaks
        if (samples != null && samples.isNotEmpty()) {
            // Downsample for screen width, taking the maximum amplitude per pixel to preserve attacks.
            val columns = size.width.toInt().coerceIn(1, 4096)
            for (column in 0 until columns) {
                val from = (column.toLong() * samples.size / columns).toInt()
                val endExclusive = (((column.toLong() + 1) * samples.size + columns - 1) / columns)
                    .toInt().coerceAtMost(samples.size)
                var peak = 0f
                for (i in from until endExclusive) {
                    val amplitude = samples[i]
                    if (amplitude.isFinite()) peak = maxOf(peak, abs(amplitude).coerceIn(0f, 1f))
                }
                val height = peak * mid * 0.87f
                val tint = if (column % 4 < 2) Color(0xFF02CDE7) else Color(0xFF1266EE)
                // Map the sampled columns back to the full canvas width.
                val x = (column + 0.5f) * size.width / columns
                drawLine(tint, Offset(x, mid-height), Offset(x, mid+height), (size.width / columns).coerceAtLeast(1f))
            }
        }
        // Events are measured onset times, not fabricated peaks.
        val events = kicks.map { it to Color(0xFFFF674C) } +
            others.map { event ->
                event.seconds to when (event.kind) {
                    TransientKind.SNARE -> Color(0xFF087FFF)
                    TransientKind.PERCUSSION, TransientKind.HI_HAT -> Color(0xFFFFB000)
                    TransientKind.KICK -> Color(0xFFFF674C)
                }
            }
        events.filter { it.first.isFinite() && it.first in 0.0..duration }.forEach { (seconds, tint) ->
            val x = (seconds / duration).toFloat() * size.width
            drawLine(tint, Offset(x, mid-34.dp.toPx()), Offset(x, mid+34.dp.toPx()), 1.dp.toPx())
        }
    }
}

@Composable private fun TempoCurve(points: List<Pair<Double, Double>>) {
    Canvas(Modifier.fillMaxWidth().height(38.dp)) {
        if (points.size < 2) return@Canvas
        val v = points.map { it.second }; val low = (v.minOrNull() ?: 0.0) - 1; val span = ((v.maxOrNull() ?: 1.0) - low + 1).coerceAtLeast(0.1)
        for (i in 1 until v.size) {
            val x1 = (i - 1).toFloat() / (v.size - 1) * size.width; val x2 = i.toFloat() / (v.size - 1) * size.width
            val y1 = size.height - ((v[i - 1] - low) / span).toFloat() * size.height; val y2 = size.height - ((v[i] - low) / span).toFloat() * size.height
            drawLine(TEAL, Offset(x1, y1), Offset(x2, y2), 2.dp.toPx())
        }
    }
}
@Composable private fun WaveGrid(audio: AudioFileInfo?, peaks: WaveformPeaks?, markers: List<WarpMarker>, alignmentPreview: ManualGridPreview.Preview?, phaseWindows: List<KickPhaseDrift.Window>, showPhaseZones: Boolean, selectedPhaseWindow: Int, onPhaseSelect: (Int) -> Unit, transients: List<TransientEvent>, showTransients: Boolean, showTargetGrid: Boolean, manual: Boolean, zoom: Float, viewStart: Float, selectedMarkerIndex: Int, onSelect: (Int) -> Unit, onEditStart: () -> Unit, onMove: (Int, Double) -> Unit) {
    var selected by remember { mutableIntStateOf(-1) }
    var dragAnchorTime by remember { mutableDoubleStateOf(0.0) }
    var dragTouchStartX by remember { mutableFloatStateOf(0f) }
    var dragChanged by remember { mutableStateOf(false) }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentMarkers by rememberUpdatedState(markers)
    val currentOnEditStart by rememberUpdatedState(onEditStart)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentPhaseSelect by rememberUpdatedState(onPhaseSelect)
    Canvas(Modifier.fillMaxWidth().height(170.dp).pointerInput(audio, zoom, viewStart, phaseWindows) {
        detectTapGestures { point ->
            val a = audio ?: return@detectTapGestures
            if (size.width <= 0) return@detectTapGestures
            val visible = a.durationSeconds / zoom
            val time = viewStart + point.x / size.width * visible
            val index = PhaseWindowNavigator.at(phaseWindows, time)
            if (index >= 0) currentPhaseSelect(index)
        }
    }.background(Color(0xFF0B121A), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF253644), RoundedCornerShape(12.dp)).pointerInput(manual, audio, zoom, viewStart) {
        detectDragGestures(onDragStart = { p ->
            selected = -1
            dragChanged = false
            if (manual && currentMarkers.size >= 3 && audio != null && size.width > 0f) {
                val visible = audio.durationSeconds / zoom
                val latest = currentMarkers
                selected = MarkerTouchSelection.nearest(
                    latest.map { it.sourceSeconds }, p.x, size.width.toFloat(),
                    viewStart.toDouble(), visible, 24.dp.toPx()
                )
                if (selected >= 0) {
                    dragAnchorTime = latest[selected].sourceSeconds
                    dragTouchStartX = p.x
                    currentOnSelect(selected)
                }
            }
        }, onDrag = { change, _ ->
            val latest = currentMarkers
            if (manual && selected in 1 until latest.lastIndex && audio != null && size.width > 0f) {
                val candidate = MarkerDragPrecision.proposedSource(
                    dragAnchorTime, dragTouchStartX, change.position.x,
                    size.width.toFloat(), audio.durationSeconds / zoom,
                    latest[selected - 1].sourceSeconds, latest[selected + 1].sourceSeconds
                )
                if (candidate != null && kotlin.math.abs(candidate - latest[selected].sourceSeconds) >= 0.000001) {
                    if (!dragChanged) { currentOnEditStart(); dragChanged = true }
                    currentOnMove(selected, candidate)
                    change.consume()
                }
            }
        }, onDragEnd = {
            selected = -1
            dragChanged = false
        }, onDragCancel = {
            selected = -1
            dragChanged = false
        })
    }) {
        val a = audio ?: return@Canvas
        val visibleDuration = a.durationSeconds / zoom
        val startTime = viewStart.toDouble().coerceIn(0.0, (a.durationSeconds - visibleDuration).coerceAtLeast(0.0))
        val endTime = (startTime + visibleDuration).coerceAtMost(a.durationSeconds)
        if (showPhaseZones) PhaseTimeline.project(phaseWindows, startTime, endTime).forEach { span ->
            drawRect(if (span.reliable) TEAL.copy(alpha = 0.11f) else ORANGE.copy(alpha = 0.18f),
                topLeft = Offset(span.left * size.width, 0f),
                size = androidx.compose.ui.geometry.Size((span.right - span.left) * size.width, size.height))
        }
        if (showPhaseZones && selectedPhaseWindow in phaseWindows.indices) {
            val w = phaseWindows[selectedPhaseWindow]
            val left = ((w.startSeconds - startTime) / visibleDuration).toFloat().coerceIn(0f, 1f)
            val right = ((w.endSeconds - startTime) / visibleDuration).toFloat().coerceIn(0f, 1f)
            if (right > left) drawRect(Color.White.copy(alpha = 0.07f), Offset(left * size.width, 0f), androidx.compose.ui.geometry.Size((right - left) * size.width, size.height))
        }
        for (i in 0..8) { val x = size.width * i / 8f; drawLine(Color(0xFF1D2A36), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()) }
        val mid = size.height / 2
        val firstFrame = (startTime * a.sampleRate).toLong().coerceIn(0L, a.frameCount - 1)
        val lastFrame = (endTime * a.sampleRate).toLong().coerceIn(firstFrame + 1, a.frameCount)
        val step = ((lastFrame - firstFrame) / size.width.toInt().coerceAtLeast(1)).coerceAtLeast(1L)
        var x = 0f
        while (x < size.width) {
            val start = (firstFrame + x.toInt() * step).coerceIn(firstFrame, lastFrame - 1); var peak = 0f
            if (peaks != null && peaks.peaks.isNotEmpty()) {
                val firstBucket = (start / peaks.framesPerBucket).toInt().coerceIn(0, peaks.peaks.lastIndex)
                val lastBucket = ((start + step - 1) / peaks.framesPerBucket).toInt().coerceIn(firstBucket, peaks.peaks.lastIndex)
                for (b in firstBucket..lastBucket) peak = max(peak, peaks.peaks[b])
            }
            drawLine(Color(0xFF20BDA9), Offset(x, mid - peak * mid * 0.88f), Offset(x, mid + peak * mid * 0.88f), 1.1f); x += 1f
        }
        if (showTransients) transients.forEach { event ->
            if (event.seconds in startTime..endTime) {
                val x = ((event.seconds - startTime) / visibleDuration * size.width).toFloat()
                val color = when (event.kind) {
                    TransientKind.KICK -> ORANGE
                    TransientKind.SNARE -> PINK
                    TransientKind.HI_HAT -> Color(0xFF4EA5FF)
                    TransientKind.PERCUSSION -> Color(0xFF9BA4AE)
                }
                drawLine(color.copy(alpha = (0.35 + event.confidence * 0.55).toFloat().coerceIn(0.3f, 0.9f)), Offset(x, size.height * 0.12f), Offset(x, size.height * 0.35f), 2.dp.toPx())
            }
        }
        markers.forEachIndexed { i, m ->
            val sxTime = m.sourceSeconds; val txTime = m.targetSeconds
            if (sxTime in startTime..endTime) {
                val sx = ((sxTime - startTime) / visibleDuration * size.width).toFloat()
                drawLine(if (i == selectedMarkerIndex) ORANGE else TEAL, Offset(sx, 0f), Offset(sx, size.height), if (i == selectedMarkerIndex) 3.dp.toPx() else 1.5.dp.toPx())
            }
            if (showTargetGrid && txTime in startTime..endTime) {
                val tx = ((txTime - startTime) / visibleDuration * size.width).toFloat()
                drawLine(PINK.copy(alpha = 0.72f), Offset(tx, 0f), Offset(tx, size.height), 1.dp.toPx())
            }
        }
        // Preview only: source is orange, proposed target is white; never mutates markers.
        if (alignmentPreview?.accepted == true) {
            val overlay = ManualGridOverlay.project(alignmentPreview, startTime, endTime)
            overlay?.let { positions ->
                val detail = ManualGridOverlay.detail(positions, size.width, 12.dp.toPx())
                // When both strokes overlap, use a magnified horizontal indicator at the top.
                // It is purely graphical: marker data and audio remain untouched.
                if (detail?.needsMagnification == true) {
                    val center = (detail.sourceX!! + detail.targetX!!) / 2f
                    val left = (center - 36.dp.toPx()).coerceAtLeast(0f)
                    val right = (center + 36.dp.toPx()).coerceAtMost(size.width)
                    val y = 10.dp.toPx()
                    drawLine(Color(0xFF34465B), Offset(left, y), Offset(right, y), 5.dp.toPx())
                    drawLine(ORANGE, Offset((center - 8.dp.toPx()).coerceAtLeast(left), y - 6.dp.toPx()), Offset((center - 8.dp.toPx()).coerceAtLeast(left), y + 6.dp.toPx()), 2.dp.toPx())
                    drawLine(Color.White, Offset((center + 8.dp.toPx()).coerceAtMost(right), y - 6.dp.toPx()), Offset((center + 8.dp.toPx()).coerceAtMost(right), y + 6.dp.toPx()), 2.dp.toPx())
                }
                positions.sourceFraction?.let { fraction ->
                    val px = fraction.toFloat() * size.width
                    drawLine(ORANGE, Offset(px, 0f), Offset(px, size.height), 2.5.dp.toPx())
                }
                positions.targetFraction?.let { fraction ->
                    val px = fraction.toFloat() * size.width
                    drawLine(Color.White, Offset(px, 0f), Offset(px, size.height), 2.5.dp.toPx())
                }
            }
        }
        drawLine(Color(0xFF526477), Offset(0f, mid), Offset(size.width, mid), 0.7.dp.toPx())
    }
}

private data class AudioFileInfo(val sampleRate: Int, val channels: Int, val frameCount: Long) {
    val durationSeconds: Double get() = frameCount.toDouble() / sampleRate
}

private fun fmt(value: Double, decimals: Int = 1): String = String.format(Locale.US, "%1$.${decimals}f", value)
