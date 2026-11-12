# DaniM Auto Warp 4.0 RC88

## Cambio principal
`KickTempoWindowAudit.kt` implementa una auditoría **solo diagnóstica** del BPM en ventanas cronológicas fijas. Requiere al menos tres intervalos compatibles por ventana y dos ventanas consecutivas con evidencia antes de informar estabilidad. No interpola huecos ni autoriza el warp.

## Pruebas
- `smoke-kick-tempo-window-audit.sh`: 12 casos superados.
- `smoke-beat-tracking.sh`, `smoke-kick-tempo-holdout.sh`, `smoke-kick-sequence-gate.sh` y `smoke-kick-interval-audit.sh`: superados.
- `smoke-real-detector-audit.sh`: cinco extractos de 8 segundos de la grabación FLAC aportada. La columna `window_stable` es una evaluación diagnóstica, no autorización de procesamiento.

| Offset | Ventanas con evidencia | Comparaciones adyacentes | Deriva máxima | Estable |
|---|---:|---:|---:|---|
| 30 s | 1 | 0 | n/d | No concluyente |
| 75 s | 2 | 1 | 0 % | Sí |
| 150 s | 0 | 0 | n/d | No concluyente |
| 240 s | 0 | 0 | n/d | No concluyente |
| 360 s | 1 | 0 | n/d | No concluyente |

## Limitaciones
El análisis usa ventanas cortas y no demuestra estabilidad en los ocho minutos completos. No se ha compilado un APK, ni realizado pruebas auditivas A/B en Android. La grabación FLAC no está incluida en el ZIP.
