# DaniM Auto Warp 4.0 RC39

## Cambios
- `BeatAnalyzer.fallbackGrid`: cuando no se detectan al menos cuatro bombos fiables, devuelve BPM informativo, confianza 0, beatTimes vacío y solo dos marcadores identidad. Antes generaba un beatgrid sintético con confianza 0.25 y marcadores adicionales que podían interpretarse como una detección real.
- Nuevo `smoke-low-confidence.sh`: regresión para silencio y transitorios escasos.
- Reparada la dependencia `TempoContinuityGate.kt` en `smoke-beat-tracking.sh`.

## Pruebas locales ejecutadas
- `smoke-low-confidence.sh`: PASS (dos señales).
- `smoke-beat-tracking.sh`: PASS (120 BPM, 27 pulsos, 29 marcadores).
- `smoke-real-vinyl.sh` con `Javi Golo & Andres Gomes - Expanden.flac`: seis ventanas de 60 s, BPM 136.36 en todas, confianza 0 y dos marcadores de identidad por ventana. NO se ha validado la corrección automática.
- `smoke-core.sh`: PASS.
- `smoke-riff.sh`: PASS.
- `smoke-warp-pipeline.sh`: PASS (advertencias no fatales del compilador Kotlin).

## Pendiente
- Mejorar seguimiento de fase y confianza con el audio real; los seis fragmentos aún se bloquean por baja confianza.
- Escucha A/B de un render musical corregido.
- Compilar APK y probar interfaz en Android SDK/emulador o dispositivo; no disponible en este entorno.
