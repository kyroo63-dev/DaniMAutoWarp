# DaniM Auto Warp — Android 4.0.0-rc1 (candidata; sin APK validado)

Aplicación Android nativa (Kotlin + Jetpack Compose) para analizar ripeos de vinilo, corregir desviaciones del beatgrid y editar marcadores Warp.

## Novedades 1.2

- **Anclajes de transitorios en WSOLA streaming:** los ataques detectados de alta confianza (kick, snare y percusión) se incorporan como referencias fuente/destino. Cerca del anclaje, el motor limita la búsqueda para no desplazar el ataque detectado.
- **Alineación estéreo compartida:** la correlación se calcula con ambos canales y cada grano usa una única decisión temporal.
- **Lectura file-backed en exportación:** para WAV PCM16, la app copia el original a caché y el render WSOLA lee ventanas aleatorias desde ese fichero. La copia se elimina al terminar. Si el WAV no es PCM16 o el formato no es compatible, utiliza el PCM ya decodificado.
- **Métricas ampliadas:** la exportación muestra granos procesados, transitorios anclados y tamaño máximo de ventana fuente.
- **Pruebas:** render estéreo de duración conocida, conservación de tono de una senoide, anclajes de transitorios, límite de ventana y lectura file-backed PCM16.

## Alcance y limitaciones de 1.2

- El motor streaming es una implementación WSOLA nativa experimental; **no es una réplica de Elastique Pro**. Con ratios extremos, cambios de tempo muy bruscos o material polifónico complejo pueden aparecer artefactos.
- La detección de transitorios sigue dependiendo del análisis previo. Los anclajes de confianza baja se descartan y un falso positivo de alta confianza puede fijar un ataque incorrecto; revisa la detección antes de exportar.
- **La importación/análisis aún decodifica la pista completa a `PcmAudio` en memoria.** La lectura desde archivo en esta versión se usa durante la exportación streaming WAV PCM16; no elimina todavía la memoria utilizada por el análisis.
- La escucha A/B continúa usando el motor en memoria. El interruptor streaming afecta a la exportación WAV.
- La fuente file-backed admite únicamente WAV RIFF PCM16 mono/estéreo. WAV PCM de otros bits, IEEE float, AIFF y MP3 usan el PCM decodificado en memoria.
- El streaming WSOLA escribe PCM16 WAV. No genera todavía el análisis post-render completo de discontinuidades que utiliza el flujo en memoria.
- El modo streaming conserva el tono mediante WSOLA, pero no garantiza resultados idénticos a Elastique Pro. Escucha siempre el resultado, especialmente en percusiones complejas y mezclas densas.

## Motores y formatos

- WSOLA experimental en memoria con anclajes de transitorios.
- WSOLA streaming con overlap-add continuo, anclajes de transitorios y lectura file-backed PCM16 opcional.
- Phase Vocoder experimental en memoria.
- Importación WAV PCM, AIFF PCM y formatos comprimidos según los decodificadores Android disponibles.
- Exportación WAV PCM16 y beatgrid CSV genérico; CSV no es formato nativo garantizado de Rekordbox, Traktor o Serato.

## Pruebas y compilación

El núcleo DSP puede compilarse de forma aislada con Kotlin/JVM. La suite Gradle y la aplicación Android deben validarse en Android Studio con JDK 17 y Android SDK 35 (`:app:testDebugUnitTest`, `:app:assembleDebug`). No se declara APK generado ni validación en dispositivo Android.

Abrir el proyecto en Android Studio con JDK 17 y Android SDK 35. El ZIP no incluye Gradle Wrapper ejecutable completo; usar Gradle instalado o generar wrapper desde Android Studio.

## Version 1.3.0 — file-backed A/B preview

After decoding an imported track, the app writes a normalized PCM16 WAV cache in the app cache directory. WSOLA export uses this random-access file source. Original A/B playback reads the selected interval from disk in 2048-frame windows. Corrected A/B playback renders WSOLA incrementally to a temporary WAV and then streams the selected interval from that file through `AudioTrack.MODE_STREAM`; it does not allocate an in-memory rendered song. Stop/lifecycle cleanup closes readers and removes temporary preview renders.

**Memory boundary:** the current UI still retains the decoded `PcmAudio` for waveform display, transient analysis and the existing analysis algorithms. Therefore this release makes export and audition file-backed, but does not yet claim zero-copy or fully out-of-core import/analysis. MP3/AIFF decoding still uses the existing decoder path before cache creation. The temporary normalized PCM cache requires additional disk space approximately equal to 16-bit PCM size (sample rate × channels × 2 bytes × duration).

## Versión 1.5.0 — caché PCM progresiva para formatos comprimidos

- MP3 y otros formatos comprimidos admitidos por `MediaExtractor`/`MediaCodec`: decodificación por buffers directamente a WAV PCM16 en el directorio de caché, sin acumular una lista de fragmentos en memoria.
- Conversión de PCM8/16/24/32 y float a PCM16, con comprobación de alineación y tamaño RIFF.
- Reutilización del WAV de caché por identificador URI y metadatos de tamaño/fecha (cuando estén disponibles); el archivo se valida al reabrir.
- WAV y AIFF conservan el importador anterior y todavía se cargan completos durante la normalización.
- **Limitación importante:** tras preparar el WAV se carga `PcmAudio` completo para el analizador, forma de onda y motores heredados; por ello la memoria total de la aplicación todavía escala con la duración. No se ha implementado análisis verdaderamente por bloques ni caché de forma de onda en esta versión.
- Los proveedores que no exponen una fecha de modificación fiable podrían ofrecer cachés obsoletas si cambian el contenido manteniendo la URI y tamaño.

### v1.7 — Pista sin PCM residente en la interfaz
Tras importar, `MainActivity` retiene solo `AudioFileInfo`, `WaveformPeaks` y el archivo WAV PCM16 cacheado. La forma de onda usa los picos en vez de acceder a muestras residentes. El analizador y el motor WSOLA streaming leen del archivo. Los formatos WAV/AIFF todavía se decodifican inicialmente con el parser heredado y Phase Vocoder / modo WSOLA no streaming realizan una carga temporal completa de PCM: estas rutas **no** son out-of-core. El formato WAV RIFF clásico y el almacenamiento libre también limitan la duración admitida.


### Versión 1.8: sesiones recuperables
Al abrir una pista, la aplicación busca un archivo `<nombre-cache>.warp.json` y restaura los marcadores Warp y los ajustes del motor. Los cambios se guardan automáticamente tras 650 ms de inactividad. La restauración depende de la conservación y reutilización de la caché PCM: Android puede limpiar `cacheDir`, y la aplicación conserva como máximo tres cachés PCM recientes. No equivale a un proyecto portable ni a una garantía de conservación permanente. Para conservar sesiones a largo plazo será necesario moverlas a almacenamiento persistente o exportarlas.

### 1.9: Proyectos portátiles
Con una pista abierta, **GUARDAR PROYECTO** crea un documento JSON fuera de la caché de la app. Para recuperar una edición, primero importa el mismo audio y después usa **ABRIR PROYECTO**. El proyecto guarda marcadores Warp, motor, calidad y modo manual; no incluye el audio. Se comprueba el SHA-256 del PCM normalizado para evitar aplicar marcadores a otra pista. El guardado automático en caché de la versión anterior permanece disponible.

**Pendiente:** importación WAV/AIFF totalmente progresiva, recuperación de audio automática y compilación/pruebas instrumentadas Android.

### Versión 2.0: importación WAV/AIFF progresiva

La importación WAV PCM y AIFF PCM sin compresión pasa por `StreamingPcmImporter`, que copia la fuente a un temporal de disco, interpreta los encabezados RIFF/FORM y convierte las muestras por ventanas de 4096 fotogramas. Esto limita el uso de RAM durante la conversión. La copia temporal permite leer proveedores Android cuyo `InputStream` no admite búsqueda aleatoria. El temporal se elimina al terminar o fallar.

Formatos PCM admitidos: mono/estéreo, 8/16/24/32 bits enteros, entre 8 kHz y 384 kHz. WAV IEEE float, RF64, AIFC y WAV extensible no están implementados en este importador. WAV PCM de 8 bits se interpreta sin signo; AIFF PCM de 8 bits se interpreta con signo.

El análisis y la visualización siguen el diseño basado en archivos y cachés de versiones previas. El motor Phase Vocoder y otras rutas heredadas pueden conservar límites de memoria. Esta entrega ha pasado pruebas JVM del nuevo importador, pero no una compilación ni pruebas de dispositivo Android.

### Versión 2.1: protección de Auto Warp
El análisis automático aplica `WarpSafetyGate` a los marcadores detectados. Se descartan anclajes con pendientes locales fuera de 0,85–1,15 y se conserva la duración de la pista. Esto es una medida de seguridad, no una validación perceptual ni una mejora demostrada de precisión sobre vinilos reales.

### Versión 2.2 — diagnósticos de Auto Warp
`AnalysisResult.diagnostics` permite inspeccionar los transitorios clasificados, el número de anclajes automáticos descartados por `WarpSafetyGate`, la mediana/máximo de desfase y el rango de BPM observado. Los avisos no sustituyen la revisión auditiva. El algoritmo de detección de golpes no se ha reentrenado en esta versión: la mejora es de observabilidad, no una precisión demostrada superior.


## 2.3.0 — Panel de diagnóstico
- Panel desplegable de calidad del análisis Auto Warp: recuentos de bombos, cajas, hi-hats y percusiones, marcadores aceptados/descartados, desviaciones de tiempo, rango de BPM y advertencias.
- Se utilizan los datos de `AnalysisResult.diagnostics` generados por el motor 2.2; no se afirma que haya mejorado la precisión del clasificador.
- Las métricas se calculan durante Auto Warp y no se persisten como parte del archivo de proyecto; al restaurar una sesión hay que volver a analizar para verlas.
- No se ha validado la interfaz con Android SDK ni con dispositivo físico.

### Versión 2.4: confianza por zonas
El diagnóstico muestra ventanas de 16 segundos con una puntuación heurística basada en la coincidencia entre pulsos y bombos detectados, la fuerza de los golpes y percusión competidora. Las zonas naranjas requieren revisión manual. **No se ha validado aún una mejora de la precisión de clasificación de bombos/cajas con grabaciones reales**; la puntuación no es una probabilidad calibrada.

### Version 2.5 — spectral support for kick classification
The automatic beat analyzer now uses a small, file-backed spectral check around kick candidates. It measures bass versus midrange spectral energy and uses the result conservatively to adjust confidence. This is **not** a machine-learning classifier, and accuracy on real vinyl rips remains to be validated. The beat tracker, safety gate, manual editing, A/B audition and export remain available.

### 2.6 Transient overlay
Use the TRANSITORIOS switch to show colored onset markers over the waveform. Browse visible transients and relabel their types. These manual labels are session-local and visualization-only; they do not change automatic beat tracking or persist to project JSON.

### 2.7 Confirmed kick re-warp and project persistence
Manual transient classifications are saved in cache sidecars and portable JSON projects (schema 2; schema 1 remains importable). The new RECALCULAR CON BOMBOS action rebuilds conservative warp anchors using confirmed kicks near the previously analyzed beat positions, and runs WarpSafetyGate before applying. At least three matching kicks are required. This is a targeted recalculation, not a complete rerun of transient detection; an initial Auto Warp analysis remains necessary.

### Versión 2.8 — edición precisa de beatgrid
En modo manual, selecciona un anclaje interior desde la forma de onda (iniciando un gesto sobre su línea) o con los botones de zona. Elige pasos de 1, 10 o 50 ms para mover su tiempo fuente sin cambiar el tiempo destino. Los anclajes de inicio y fin están protegidos y no se permite cruzar marcadores vecinos. La cuadrícula de destino puede ocultarse para comparar visualmente con la fuente. Los cambios se integran con el historial deshacer/rehacer y el guardado de sesiones existente.


## v2.9 – Editor avanzado
- Zoom de forma de onda hasta 64×.
- Inserción de anclaje en el centro de la ventana, con destino interpolado.
- Eliminación de anclajes interiores; extremos protegidos.
- Imán a transitorios detectados en un radio de 50 ms.
- Operaciones integradas en el historial deshacer/rehacer y autoguardado.
- La selección de transitorios para el imán utiliza el análisis actual; no sustituye la escucha crítica.

## Versión 3.0.0 — formatos de exportación
- Selector en la interfaz: WAV PCM 16/24-bit, WAV IEEE float 32-bit, AIFF PCM 16/24-bit.
- El render WSOLA progresivo se guarda temporalmente como WAV PCM16; después se convierte por bloques (4096 frames) al formato solicitado. La exportación de Phase Vocoder sigue utilizando RAM proporcional a la pista.
- **Limitación de fidelidad:** la caché de procesamiento sigue siendo PCM16. Un archivo final de 24 o 32 bits NO recupera resolución original de 24 bits ni aporta calidad adicional. Se conserva la frecuencia de muestreo.
- MP3 320 kbps y FLAC siguen pendientes: no se presentan como formatos disponibles. La codificación MP3 requiere integrar y validar un codificador compatible/licenciado, y FLAC necesita su propio codificador probado.
- La escucha A/B sigue siendo secuencial, no una conmutación instantánea sample-accurate. La navegación por compases y los ensayos con grabaciones reales permanecen pendientes.
- Pruebas: compilación Kotlin del núcleo y conversión sintética de los cinco formatos, incluyendo lectura de retorno de WAV y AIFF. No se ha compilado un APK Android.


## Versión 3.1 — exportación FLAC PCM16

Se añade FLAC a la lista de exportación. `FlacPcm16Export` genera FLAC estándar con
STREAMINFO, MD5 del PCM, cabeceras y CRC de tramas, subtramas verbatim y lectura
por bloques de 4096 muestras. Se ha validado con `flac -t` y `ffprobe` sobre audio
sintético estéreo de 44,1 kHz. La exportación es **sin pérdida respecto al PCM16
intermedio**, no respecto a fuentes originales de 24 bits. Las subtramas verbatim
no aplican compresión predictiva: el archivo puede ser grande. MP3 320 kbps,
procesamiento interno de 24 bits y APK Android siguen pendientes.

### Versión 3.2: FLAC optimizado
El exportador FLAC16 utiliza predicción fija de primer orden y residuos Rice con parámetro adaptable, con alternativa verbatim por bloque/canal. Mantiene la fidelidad exacta del PCM16 de trabajo, no recupera precisión de fuentes de 24 bits. Pruebas locales con estéreo de 44,1 kHz: verificación FLAC y comparación de muestras tras decodificar. MP3 320 kbps aún no está disponible. APK no compilado en este entorno.

### Versión 3.3.0
La monitorización A/B incorpora navegación por un beat y un compás de cuatro tiempos basada en el BPM estimado. Los botones se habilitan después de analizar la pista. Esta función no cambia el motor de audio ni proporciona aún conmutación A/B instantánea o sincronizada a nivel de muestra. MP3 320 kbps sigue pendiente de integración y validación en Android.

### 3.4.0: A/B timeline alignment
The original preview start is mapped from the corrected timeline using marker interpolation. This aligns musical events better when the recording drifts. This is still two separate playback operations, not seamless real-time switching or sample-accurate synchronized dual playback. MP3 320 kbps export remains pending.

### Versión 3.5
`StreamingQualityAnalyzer.analyze` analiza fuentes PCM respaldadas por archivo sin cargar el render completo en RAM. Los resultados son diagnósticos objetivos, no una medida de calidad auditiva. MP3 320 kbps y A/B continuo permanecen pendientes.

### 3.6.0
Tras renderizar, se analiza el WAV PCM16 intermedio desde disco por bloques y se presenta un diagnóstico en la pantalla de exportación. Los avisos son heurísticos, no sustituyen una prueba auditiva. La exportación sigue siendo WAV/AIFF/FLAC; MP3 y conmutación A/B continua están pendientes.

### 3.7.0 — A/B continuo experimental
Con motor WSOLA, pulsa **A/B CONTINUO** y alterna **ORIGINAL/CORREGIDO** mientras se reproduce el fragmento de 8 segundos. El motor utiliza un solo `AudioTrack` y consulta la posición equivalente del original mediante los marcadores Warp. La selección se aplica dentro de bloques de 256 fotogramas, con un fundido aproximado de 10 ms. No implica conmutación exacta a nivel de muestra ni reproducción instantánea: primero se renderiza el fragmento corregido. Requiere validación en dispositivo Android; exportación MP3 pendiente.

### Cambios 3.8
El reproductor A/B continuo aplica un fundido cruzado de 12 ms calculado por frame
(igual para todos los canales). Se necesita comprobar el comportamiento en un
Android físico. La exportación MP3 320 kbps no está implementada; los formatos
WAV/AIFF/FLAC disponibles no implican codificación MP3.

### Versión 3.9.0
La preescucha A/B continua usa `PreparedAbTimeline` para consultar posiciones de origen por muestra sin ordenar marcadores en el bucle de audio. Se han probado 2001 posiciones contra la implementación anterior. La optimización aún requiere validación y medición en un dispositivo Android; MP3 320 kbps sigue pendiente.


## Prueba de integración sin Android SDK (RC31)

Ejecutar `bash smoke-warp-pipeline.sh` con Kotlin CLI instalado para comprobar la ruta estéreo WSOLA → WAV → lectura. Esto no sustituye a la compilación Android ni a pruebas en dispositivo.
