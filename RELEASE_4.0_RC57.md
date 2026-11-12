# DaniM Auto Warp 4.0 RC57

- Añadido `StreamingRenderAudit.kt`: análisis de WAV PCM16 en memoria acotada, con comprobación de RIFF/chunks, número de fotogramas, diferencia de duración, pico, RMS, DC, saturación y saltos entre fotogramas.
- Detecta datos truncados y rechaza formatos distintos de PCM16 mono/estéreo.
- `smoke-streaming-render-audit.sh` crea un audio estéreo sintético, lo procesa con `StreamingWarpProcessor`, audita el WAV generado y verifica que se detecta un archivo truncado.
- La detección de saltos es una alarma heurística, no una prueba de ausencia de artefactos audibles. El render streaming sigue utilizando interpolación cúbica sin preservar el tono.
- No hay SDK Android 35 ni Gradle configurados aquí: APK y pruebas auditivas en dispositivo pendientes.
