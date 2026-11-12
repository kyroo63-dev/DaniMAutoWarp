## 4.0 RC109
- Persistencia de la posición de revisión, guardado atómico de importaciones CSV y pruebas JVM de navegación.

## 4.0.0-rc105
- Pantalla nativa Compose para revisión auditiva de 64 ataques, guardado local de etiquetas, métricas y exportación CSV.
- 7 pruebas Kotlin sintéticas superadas; sin compilación Android.

## 4.0.0-rc104
- Interfaz local de revisión humana con audio, waveform, teclado y CSV de salida.
- Ninguna modificación del motor de warp ni autorización automática.

## 4.0.0-rc103
- Cola estratificada para revisión humana de ataques, fragmentos WAV y métricas solo con etiquetas verificadas. Sin habilitar warp.

## 4.0.0-rc102
- Auditoría diagnóstica de fusión espectral-rítmica sobre FLAC real; 284 candidatos ambiguos previamente etiquetados OTHER; warp continuo bloqueado.

## 4.0.0-rc96
- Comparación reproducible de agrupamiento antiguo/anclado seguido del filtro espectral y rítmico.
- Exportación de métricas por fragmento, sin habilitar warp automático.

## 4.0 RC95
- Filtro diagnóstico de candidatos por espectro y periodicidad; pruebas y auditoría exploratoria.

## 4.0.0-rc94
- Comparación reproducible entre agrupamiento legado y anclado sobre 22 fragmentos reales; script y reporte de resultados. No altera el warp de producción.

## 4.0.0-rc93
- Agrupamiento de candidatos anclado al primer ataque para impedir cadenas de percusión. Diagnóstico solamente.
- Auditoría CSV: confianza individual de KICK desconocida se deja vacía, sin valor ficticio 1.0.
- Nuevos casos de regresión para cadenas de ataques y orden de entrada.

## 4.0.0-rc92
- Auditoría diagnóstica de continuidad de BPM y fase entre clips; 13 pruebas; 21 uniones del vinilo sin autorización de warp global.

## 4.0.0-rc88
- Auditoría diagnóstica de BPM en ventanas fijas con rechazo de huecos y datos insuficientes.
- 12 nuevas pruebas y auditoría de cinco extractos de vinilo real.

## 4.0 RC87
- Auditoría independiente de estabilidad de tempo entre mitades, sin habilitar nuevas correcciones automáticas.

## 4.0 RC86
- Añadida validación de fase con conjunto de comprobación separado (holdout).
- Nueva auditoría CSV y pruebas de regresión.

## 4.0.0-rc85
- Auditoría conjunta espectral y continuidad de beatgrid; 12 pruebas unitarias y regresión real. No cambia autorización de warp.

## 4.0.0-rc84
- Auditoría diagnóstica de continuidad del beatgrid después del agrupamiento, con 12 pruebas y CSV ampliado. Sin cambios de autorización de warp.

## 4.0 RC82
- Diagnóstico de intervalos entre candidatos de bombo, ocho pruebas y auditoría con cinco clips reales. Sin cambios en autorización automática.

## 4.0.0-rc81
- Extracted KickSequenceGate; nine unit cases passed and real FLAC regression unchanged.

## 4.0.0-rc80
- La evidencia de golpes consecutivos limita conservadoramente la confianza del warp automático.
- Reparado el script de prueba de beat tracking.
- Auditoría real de cinco fragmentos y seis pruebas de apoyo rítmico.

## 4.0.0-rc79
- Diagnóstico de respaldo de pulsaciones consecutivas, sin cambiar el warp automático.
- Seis pruebas sintéticas y auditoría de cinco extractos de audio real.

## 4.0.0-rc77
- Diagnóstico espectral por evento en `smoke-real-detector-audit.sh`, exportando CSV de eventos junto al resumen.
- Regresión del detector real sobre cinco segmentos de Expanden. Sin cambios en el clasificador ni en la autorización del warp.

## 4.0.0-rc61
- Prueba JVM reproducible de conservación de ataques de bombo sintéticos durante WSOLA streaming.
- Verificación de anclajes, duración, amplitud y memoria acotada en tres velocidades.

## 4.0 RC59
- Auditoría diferencial fuente/exportación y prueba de regresiones con WAV real.

## 4.0 RC56
- Evitar arrastres redundantes y proteger extremos en callback.
- Diagnóstico reproducible de compilación Android y prueba de integración.

## 4.0 RC52
- Superposición origen/destino en la forma de onda, limitada al viewport y sin modificación de marcadores.
- Pruebas de proyección y validación de vista previa.

## 4.0.0-rc1 — estabilización A/B
- Reparación de la integración PreparedAbTimeline en reproducción continua.
- Límite de duración del fragmento y corrección de estado de parada concurrente.
- Fundido cruzado de ganancia unitaria y pruebas JVM.
- No se considera final estable hasta validar compilación y ejecución Android.

## 3.1.0
- Añadido exportador FLAC mono/estéreo PCM16 por bloques, con STREAMINFO, MD5 y CRC.
- Selector de exportación actualizado para incluir FLAC.
- Verificación externa con `flac -t` y `ffprobe` sobre señal sintética estéreo.
- Pendiente MP3 320 kbps y validación APK.

## 2.9.0
- Editor: insertar, eliminar y ajustar anclajes a transitorios.
- Zoom máximo 64×.

## 2.3.0
- Panel desplegable de diagnósticos Auto Warp con métricas y advertencias integradas en Compose.
- Etiqueta de interfaz actualizada a 2.3.

# Changelog

## 1.2.0
- Added high-confidence kick/snare/percussion anchors to streaming WSOLA, mapped through the current warp marker timeline.
- Constrained grain search near transient anchors and exposed the number of locked anchors in render metrics.
- Connected the PCM16 WAV file-backed frame source to streaming export via a temporary cache copy, with in-memory fallback for unsupported formats.
- Updated the Compose UI status and processing description for the 1.2 streaming path.
- Added regression coverage for transient anchoring and retained bounded-window/pitch checks.
- Documented that full-file import/analysis and A/B preview still use in-memory PCM.

## 1.1.0
- Added stateful bounded-memory streaming WSOLA with persistent overlap-add state and progressive PCM16 WAV output.
- Linked stereo channels to a single correlation-based grain alignment decision.
- Added a UI switch for streaming WSOLA export and reported grain count, stretch ratio, and maximum source window.
- Added an in-memory PCM frame-source adapter and streaming WSOLA tests.
- Documented current limitations: no explicit transient anchors in streaming mode, source import remains whole-buffer, and A/B preview remains in-memory.

## 1.0.0
- Added a bounded-memory, block-based WAV rendering path using random-access PCM frame sources.
- Added monotonic marker validation, per-zone strength blending, cubic interpolation, stereo-linked processing, and incremental PCM16 WAV output.
- Added streaming renderer tests for a 12-second stereo source, block count, output container, memory-window bound, and invalid markers.
- Important: this first streaming path is pitch-coupled cubic resampling. It is a low-memory fallback and does not yet replace pitch-preserving WSOLA/Phase Vocoder. The existing high-quality engines remain whole-buffer.

## 0.9.0
- A/B fragment preview, block-based WAV writing, render quality report, and Beatgrid CSV v2.
- Added file-backed `Pcm16WavFileSource` for random-access PCM16 WAV windows.
- Added an explicit test for file-backed window reads.

## 1.3.0
- Added a normalized PCM16 WAV cache after import for random-access file-backed WSOLA export and preview.
- Added `StreamingPreview`, which streams source and rendered A/B fragments to `AudioTrack.MODE_STREAM` in 2048-frame windows.
- Corrected A/B preview now renders to a temporary WAV progressively, then plays the selected output interval directly from disk.
- Added reader/temp-file cleanup and stop handling for both streaming and legacy preview paths.
- Added regression tests for streaming WSOLA pitch-preserving output, transient anchors, and file-backed random windows.
- Analysis and waveform data remain in memory; this version does not claim fully out-of-core decoding or beat analysis.

## 1.5.0
- Nuevo decodificador comprimido a caché WAV PCM16 mediante buffers MediaCodec, con escritura directa a disco.
- Validación del WAV generado, límite RIFF, limpieza de temporales y reutilización de caché.
- Integración del flujo de importación con la caché progresiva.
- Pendiente: análisis y forma de onda completamente fuera de memoria.

## 1.7.0 — Modelo de pista basado en archivo
- La pantalla principal ya no conserva `PcmAudio` completo después de importar: utiliza metadatos (`AudioFileInfo`) y la caché PCM16 en disco.
- Forma de onda y edición de marcadores trabajan con duración/frames de metadatos y picos precalculados.
- Reutilización de caché de picos cuando existe; regeneración si falla su lectura.
- Auto Warp, exportación WSOLA y A/B WSOLA trabajan desde la fuente de archivo.
- El motor Phase Vocoder y la exportación WSOLA sin streaming cargan PCM temporalmente; no se consideran de memoria constante.
- La importación WAV/AIFF sigue teniendo una ruta heredada en memoria durante la conversión inicial.


## 1.8.0 — Sesiones recuperables
- Guardado automático con debounce de 650 ms de marcadores Warp, motor, calidad y modo manual.
- Restauración de ajustes al volver a importar el mismo audio cuando se reutiliza su caché PCM.
- Archivo JSON versionado, con longitud PCM comprobada y validación de marcadores.
- Limpieza de archivos auxiliares de sesión y picos cuando se elimina una caché PCM antigua.
- No incluye todavía importación WAV/AIFF completamente progresiva ni compilación APK.

## 1.9.0
- Exportación de proyectos de edición en JSON a documentos elegidos por el usuario mediante Storage Access Framework.
- Importación de proyectos para el audio actualmente abierto, con comprobación SHA-256 y tamaño del PCM normalizado.
- Recuperación de marcadores Warp, motor, calidad y modo manual desde un archivo independiente de la caché.
- Límite de 8 MB al importar proyectos y validación de orden, valores finitos y fuerza de marcadores.
- El proyecto JSON no contiene el audio y no restaura automáticamente una pista eliminada; se debe importar primero el audio correspondiente.
- La importación progresiva WAV/AIFF continúa pendiente.

## 2.0.0 — Importación PCM progresiva
- Nuevo `StreamingPcmImporter`: conversión WAV PCM y AIFF PCM sin compresión mediante bloques de 4096 fotogramas.
- Conversión PCM mono/estéreo de 8/16/24/32 bits a caché WAV PCM16, con comprobaciones de truncamiento y límites RIFF.
- El flujo `ProgressivePcmCache.prepare` utiliza ahora el importador progresivo para WAV/AIFF en lugar de `readBytes()` y `FloatArray` de canción completa.
- Invalidación de caché de importación v2 para impedir reutilizar archivos decodificados con el formato anterior.
- Pruebas de conversión WAV/AIFF realizadas sobre el núcleo JVM. No se ha compilado APK Android.

## 2.1.0
- Filtro conservador de seguridad para marcadores Auto Warp: evita saltos no monótonos, cambios locales de velocidad superiores a ±15 % y desplazamientos mayores de 500 ms.
- Preserva el inicio y final originales; no restringe los ajustes manuales.
- Pruebas unitarias para deriva gradual y falsos anclajes.

## 2.2.0
- Nuevo `WarpDiagnosticsAnalyzer`: recuentos de bombos, cajas, hi-hats y percusión, marcadores aceptados/rechazados, desviaciones de timing y variación de BPM.
- `AnalysisResult.diagnostics` ofrece el informe al resto de la aplicación sin romper los constructores anteriores.
- Avisos conservadores ante baja confianza, marcadores rechazados, variaciones elevadas de BPM y desfases importantes.
- El informe está disponible en el núcleo de análisis; todavía no hay un panel visual ni exportación JSON del diagnóstico.

## 2.4.0
- Añade ZoneConfidenceAnalyzer para puntuar el soporte de golpes de bombo por ventanas de 16 s.
- Señala zonas con pocos bombos alineados o percusión competidora para revisión manual.
- Visualiza las zonas en el panel de diagnóstico; puntuaciones heurísticas, no probabilidades calibradas.
- Añade pruebas unitarias de zonas y límites temporales.

## 2.5.0 — Spectral kick evidence
- Added `SpectralKickEvidence`, a conservative file-backed Goertzel spectral cue around candidate kick onsets (48–135 Hz versus 260–1100 Hz).
- Kick confidence now combines the existing multiband transient score with spectral evidence. Weak, clearly midrange-dominant candidates can be rejected; high-confidence candidates remain eligible.
- Updated application version and UI label to 2.5.0.
- Pure Kotlin DSP compilation and synthetic 80 Hz / 700 Hz discrimination smoke test passed.
- Limitations: not calibrated on real vinyl recordings; no full Android SDK build or listening test; spectral bins are approximate and can be affected by overlapping instruments.

## 2.6.0
- Transient overlay on zoomable waveform (kick/snare/hi-hat/percussion), toggle and legend.
- Manual transient classification labels for current session (visual only; not persisted or fed back into beat tracker).
- Fixed undefined references in WaveGrid gesture handler and marker edit bounds found in 2.5 source.
- Android build still requires SDK/Gradle; UI not device-validated.

## 2.7.0
- Manual transient labels persisted in automatic sessions and portable JSON (schema 2, backward-compatible schema 1 import).
- Recalculate warp from confirmed kicks with conservative beat matching and WarpSafetyGate validation.
- Undo support for recalculated marker set.

## 2.8.0
- Editor de precisión para desplazar marcadores fuente en pasos de 1, 10 o 50 ms, sin modificar los destinos.
- Selección de anclajes desde la forma de onda y resaltado de la selección actual.
- Interruptor para mostrar u ocultar la cuadrícula de destino, conservando la fuente visible.
- Lectura de origen, destino y desfase en milisegundos para el marcador activo.
- Pruebas unitarias de límites y desplazamiento no destructivo.

## 3.0.0
- Added export selector for WAV PCM16/24, WAV float32, AIFF PCM16/24.
- Added bounded-memory converter from canonical PCM16 render and temporary-render cleanup.
- Explicitly documented intermediate-precision limitations and pending FLAC/MP3.
- Verified Kotlin DSP compilation and synthetic export round-trips; Android APK/UI not verified.

## 3.2.0
- FLAC PCM16: codificación sin pérdida con predictor fijo de orden 1 y residuos Rice, seleccionados adaptativamente por canal y bloque.
- Se conserva la subtrama verbatim como alternativa cuando la predicción no reduce el tamaño.
- Codificación progresiva sin cargar la pista completa en memoria.
- Validación sintética: decodificación con FFmpeg, `flac -t` y comparación PCM exacta.
- MP3 320 kbps y A/B sincronizado siguen pendientes; no se incluyen codificadores de terceros.

## 3.3.0
- Añadida navegación de preescucha por beat y compás (4 beats) según BPM detectado.
- Límites de desplazamiento para evitar superar la duración de la pista.
- Prueba unitaria y smoke test del módulo BeatNavigation.
- MP3 320 kbps sigue pendiente: no se incluye un codificador Android no validado.
- A/B sigue siendo secuencial; no se declara conmutación instantánea de precisión de muestra.

## 3.4.0
- A/B preview aligns the original listening start to the corrected timeline using interpolated warp markers and their strength.
- Added pure Kotlin timeline mapping and unit tests.
- Corrected preview status shows the selected target-timeline range instead of the temporary clip-local time.
- MP3 export and continuous sample-accurate A/B switching remain unimplemented.

## 3.5.0
- Diagnóstico de calidad PCM por bloques con memoria acotada para renders largos: pico, RMS, DC, clipping, saltos entre muestras y diferencia de duración.
- Conserva el estado de la muestra anterior entre bloques para detectar discontinuidades en sus límites.
- No implica una puntuación perceptual ni conmutación A/B instantánea. MP3 320 kbps continúa pendiente.

## 3.6.0
- Informe de calidad por bloques tras el render: picos, RMS, DC, clipping, discontinuidades y duración.
- Panel de diagnóstico de la última exportación y avisos técnicos no perceptuales.
- No se afirma conmutación A/B continua ni MP3 320 kbps.

## 3.7.0
- Preescucha A/B continua para WSOLA: se renderiza un fragmento de hasta 8 segundos y se reproduce con un único AudioTrack.
- Botón para alternar ORIGINAL/CORREGIDO durante la reproducción sin reiniciar AudioTrack.
- Correspondencia temporal del original mediante marcadores Warp y fundido breve (~10 ms) para suavizar cambios.
- La preparación del fragmento aún requiere renderizado previo; no es conmutación garantizada a precisión de muestras. MP3 320 kbps sigue pendiente.

## 3.8.0 — Fundido cruzado A/B por muestra
- Nuevo `AbCrossfade`: mezcla original/corregido con curva de potencia igual durante 12 ms.
- `ContinuousAbPreview` conmuta entre las dos señales alineadas en la misma posición temporal, con avance del fundido por frame multicanal.
- Prueba JVM independiente a 48 kHz de inicio, transición y destino del fundido.
- Pendiente: validación en Android, mediciones de latencia real y codificador MP3 320 kbps.

## 3.9.0 — Optimización de correspondencia A/B
- Nuevo `PreparedAbTimeline`: ordena y valida los marcadores una vez y reutiliza la tabla en cada muestra de la preescucha continua.
- Elimina ordenaciones y validaciones repetidas por muestra en `ContinuousAbPreview`.
- Ajusta los límites de interpolación para evitar indexación fuera del bloque original.
- Comparación JVM de 2001 posiciones contra `AbTimeline.sourceAt` superada; núcleo Kotlin compilado.
- Pendiente: mediciones de latencia y pruebas en Android, MP3 320 kbps y APK completo.

## 4.0.0-rc37
- Añadido TempoContinuityGate para detectar saltos de tempo por compás y bloquear Warp automático no fiable.
- Nueva prueba CLI smoke-tempo-continuity.sh con tempo estable, deriva gradual, salto brusco y datos inválidos.

## 4.0.0-rc39
- Fallback seguro sin beatgrid sintético cuando no hay bombos suficientes; confianza 0 y marcadores identidad.
- Prueba de regresión de baja confianza y reparación de script de pruebas de beat tracking.
- Repetida la prueba con seis ventanas reales de Expanden: sin Warp automático por confianza insuficiente.

## 4.0 RC41
- Emparejamiento uno-a-uno de bombos y beats para evitar confianza inflada por ataques reutilizados.
- Rechazo de tiempos de beat no finitos y pruebas de regresión.

## 4.0.0-rc42
- Independent kick pulse phase audit and conservative confidence gate.
- Added CLI regression test for kick periodicity and malformed inputs.

## 4.0 RC43
- Diagnostic 12-second kick phase windows and phase-drift smoke tests.
- Real-vinyl smoke report includes per-window phase support; automatic warp remains blocked on low confidence.

## 4.0.0-rc44
- Added conservative contiguous kick-phase segment diagnostic; rejects gaps and discontinuities, without authorizing warp.
- Added phase-segment smoke test.

- RC45: phase-window and continuous-segment diagnostics integrated into `AnalysisResult`; safety gating unchanged.

## 4.0 RC46
- Superposición de zonas de fase sobre forma de onda, opción de visibilidad y resumen de segmentos.
- Proyección temporal pura testeada y scripts smoke actualizados.

## 4.0 RC47
- Selector de ventana de fase sobre la forma de onda mediante toque.
- Navegación entre ventanas, acceso a la siguiente zona dudosa y zoom centrado.
- Panel con soporte de fase y tiempos de los primeros bombos detectados.
- Navegación puramente diagnóstica: no habilita el warp ni crea marcadores.
- Prueba smoke del navegador de ventanas y casos límite.

## 4.0.0-rc48
- Ajuste manual de fase localizado ±1/5/10 ms, controles en la ventana seleccionada, deshacer y comprobación de continuidad.
- Nuevo smoke test `smoke-manual-phase-nudge.sh`.

## 4.0 RC49
- Inserción manual conservadora de marcador neutro sobre candidato de bombo confirmado; navegación y pruebas sintéticas.

## 4.0.0-rc50 — cuadrícula manual configurable
- Alineación explícita de bombo a BPM/fase configurables con validación de continuidad y límites de estiramiento.
- Integración en la pantalla de ventanas de fase y deshacer.
- Pruebas JVM para alineación y validación. APK aún no validado.

## 4.0.0-rc51
- Previsualización de destino BPM, desfase y estiramientos locales antes de confirmar alineación manual.
- Pruebas JVM de previsualización y cuadrícula manual superadas.
- Compilación Android y validación auditiva pendientes.

## 4.0 RC53
- Indicador visual ampliado para líneas de alineación manual solapadas, con pruebas de geometría y zoom.
- VersionCode 92; versionName 4.0.0-rc53.

## 4.0.0-rc54
- Selección táctil de marcadores con tolerancia en dp y pruebas de hit-testing.

## 4.0 RC55
- Arrastre táctil relativo sin saltos, límites entre marcadores y pruebas JVM de precisión.

## 4.0.0-rc57
- Auditoría WAV PCM16 de exportaciones mediante lectura por bloques, sin cargar el tema completo.
- Nueva prueba JVM de render, integridad RIFF, duración, niveles y detección de truncamiento.

## RC58
- Prueba de render y auditoría de fragmento real de vinilo: `smoke-real-vinyl-render-audit.sh`. Véase `RELEASE_4.0_RC58.md`.

## 4.0 RC60
- Añadida prueba automatizada de conservación de tono del motor WSOLA streaming en tres ratios, con validación de duración y memoria.

## 4.0.0-rc62
- Medición independiente de fidelidad de transitorios (error de ataque y ratio RMS) y prueba JVM reproducible.
- Pruebas WSOLA sintéticas RC61 repetidas satisfactoriamente.

## 4.0.0-rc63
- Informe de fidelidad WSOLA por transitorio con CSV y resumen de desplazamiento/energía.
- Prueba reproducible con cinco bombos sintéticos en tres duraciones.

## 4.0 RC64
- Ajuste del anclaje WSOLA a la posición sub-hop del ataque.
- Pruebas de fidelidad: mejora en 2,85 s y regresiones leves en 3,00 y 3,15 s.

## RC65
- Modos LEGACY, OFFSET_AWARE y ADAPTIVE para anclaje de transitorios WSOLA; regresión comparativa de los tres modos y prueba de tono.

## 4.0 RC66
- Añadida prueba de regresión WSOLA con nueve renderizados sobre fragmento real de Expanden, comparación de modos y CSV por candidato.
- Documentada regresión del selector adaptativo a velocidad nominal; no se modificó el selector sin más validación.

## 4.0.0-rc67
- Renderizado PCM16 directo para mapas temporales identidad; evita modificaciones innecesarias de transitorios.
- Pruebas de igualdad PCM16 en estéreo 44,1 kHz con audio real, preview y render no identidad.

## 4.0.0-rc68
- Añadida prueba de correcciones pequeñas (±1 %) con audio real estéreo 44,1 kHz y métricas de continuidad por salto de bloque.
- Motor WSOLA sin cambios respecto a RC67; versión de pruebas y diagnóstico.

## 4.0.0-rc69
- Prueba nueva `smoke-stereo-transient-modes.sh` con cinco anclajes de transitorios candidatos y audio real a 44,1 kHz estéreo.
- Comparación reproducible de tres modos de anclaje con factores 0,99 y 1,01; CSV por evento y comprobaciones de memoria, formato y duración.
- Informe de regresión `RELEASE_4.0_RC69.md`. El motor WSOLA no cambia en esta entrega.

## 4.0 RC70
- Selector adaptativo WSOLA corregido para escoger OFFSET_AWARE cuando la relación fuente/destino es superior a 1.005, y LEGACY en otro caso.
- Regresión real estéreo 44,1 kHz con cinco candidatos de transitorios y factores 0.99 y 1.01, seis renderizados.
- Documentación de las mejoras medidas y de sus limitaciones.

## 4.0.0-rc71
- Prueba multisegmento reproducible con 30 renderizados de audio real, 44.1 kHz estéreo, tres modos de anclaje y cinco posiciones de la grabación.
- Identificadas regresiones del selector adaptativo en diferentes secciones; motor sin cambios para evitar regresiones no justificadas.
- Informe de resultados y limitaciones en RELEASE_4.0_RC71.md.

## 4.0.0-rc72
- Búsqueda binaria del transitorio más cercano en WSOLA, en lugar de exploración lineal por cada grano.
- Regresión comparada RC71/RC72: 30 renderizados reales por versión, 40 líneas de métricas idénticas.
- No se modifican los criterios de selección adaptativa; no se afirma una mejora de calidad perceptual.

## 4.0.0-rc73
- Modo de anclaje `CONTENT_AWARE` experimental por confianza de evento y fase dentro del salto WSOLA; `ADAPTIVE` permanece por defecto.
- Matriz de regresión de 40 renderizados con cinco secciones reales y cuatro modos; resultados documentados sin atribuir mejora auditiva no verificada.

## 4.0.0-rc74
- Añadido `KickOverlapEvidence` para penalizar moderadamente posibles bombos dominados por ataques medios/agudos.
- Integrado el ajuste en `BeatAnalyzer`; añadido `smoke-kick-overlap.sh`.
- Ejecutada regresión WSOLA de 30 renderizados con fragmentos reales, sin afirmar mejora audible ni clasificación validada.

## 4.0 RC75
- Auditoría del detector de producción con cinco fragmentos reales de vinilo y CSV reproducible.
- Compuerta conservadora de densidad de candidatos de bombo para bloquear correcciones automáticas dudosas.
- Pruebas unitarias de densidad y de percusión superpuesta.

## 4.0 RC76
- Compuerta de densidad de bombos por ventanas móviles de 2 segundos, conservando detecciones para revisión manual.
- Cinco pruebas unitarias y regresión real sobre cinco fragmentos de Expanden.

## 4.0.0-rc78
- Diagnóstico de fase rítmica por candidato (`KickRhythmEvidence`), sin alterar la autorización del warp.
- Columna `rhythm_score` en el CSV de auditoría real.

## RC83
- Agrupamiento diagnóstico de candidatos graves cercanos (`KickCandidateCluster`), 9 pruebas y auditoría FLAC real. Sin cambios en el warp automático.

## 4.0 RC89
- Auditoría diagnóstica con tres extractos reales de 24 segundos; script `smoke-real-long-audit.sh` configurable. Sin cambios en autorización de warp.

## 4.0.0-rc90
- Sample 11 spaced 24-second windows over the real vinyl FLAC, retain absolute timestamps, export a compact timeline summary; do not join independent warp decisions.

## 4.0.0-rc91
- Auditoría con cobertura temporal contigua y resumen por unión de intervalos, sin habilitar warp global.

## 4.0.0-rc97
- Add conservative offline rhythmic-run diagnostic and 12 synthetic checks.
- Analyze 499 filtered candidates in 22 clips: 13 diagnostic-stable clips, zero additional automatic warp permissions.

## 4.0.0-rc98
- Auditoría diagnóstica de fase entre fragmentos; 10 pruebas sintéticas; ninguna nueva autorización de warp.

## 4.0 RC99
- Auditoría de corroboración espectral en uniones RC98; no habilita warp global.

## RC100
- Nueva medición espectral desde FLAC original en cuatro uniones candidatas; diagnóstico sin autorización de warp.

## 4.0.0-rc101
- Auditoría de ataques graves competidores con ventanas ampliadas de 8 s, mediciones frescas de espectro y envolvente sobre FLAC real.
- Sin cambios en la autorización conservadora de warp.

## 4.0.0-rc106
- Importación CSV validada para la pantalla nativa de revisión; conservación de etiquetas locales no importadas.
- Once pruebas Kotlin independientes del SDK superadas; APK y reproducción Android sin validar.

## 4.0 RC107
- Revisión Android desplazable en pantallas pequeñas y liberación del reproductor al cambiar de ataque.
- Nueve comprobaciones estáticas del contrato de interfaz.

## 4.0.0-rc108
- Parser WAV PCM16 con chunks RIFF y comprobaciones de integridad para visualizar ataques.
- Limpieza del MediaPlayer en finalización, errores y ciclo de vida.
- 9 pruebas JVM sobre audio real y casos adversos.

## RC110
# DaniM Auto Warp 4.0 RC110

- Corrección de persistencia: cada etiqueta se confirma en disco con `commit()` **antes** de avanzar o mostrarla en la interfaz; un fallo mantiene la selección actual.
- La importación CSV confirma el estado fusionado completo y solo después actualiza la interfaz.
- Reproductor: libera recursos incluso si fallan stop/reset; el callback de finalización comprueba identidad y se añade callback de error.
- Rechaza explícitamente una cola de revisión vacía para evitar acceso al índice 0.
- Los cambios en callbacks y persistencia requieren pruebas instrumentadas Android; las pruebas JVM no sustituyen compilación APK.
- El warp continuo permanece bloqueado hasta disponer de validación humana suficiente.

