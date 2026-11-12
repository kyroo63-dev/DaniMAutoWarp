# DaniM Auto Warp 4.0 RC89

## Cambios
- `smoke-real-long-audit.sh`: auditoría del detector real en tres extractos de 24 segundos (30, 150 y 300 s). Permite configurar `AUDIT_OFFSETS` y `AUDIT_DURATION`.
- El motor de producción no cambia. No se habilita el warp sobre candidatos no confirmados.

## Resultados del FLAC real
| Inicio | Candidatos | Agrupados | Ventanas válidas | Comparaciones | Deriva máxima | Estable | Warp |
|---|---:|---:|---:|---:|---:|---|---|
| 30 s | 48 | 40 | 4 | 2 | 2,22 % | Sí | No |
| 150 s | 121 | 22 | 0 | 0 | n/d | Inconcluso | No |
| 300 s | 129 | 21 | 0 | 0 | n/d | Inconcluso | No |

Los fragmentos se analizan por separado, no equivalen al seguimiento continuo de toda la canción. La confianza del detector es 0 en los tres fragmentos. La auditoría espectral en el CSV se basa en puntuaciones calculadas posteriormente, no en la fuerza interna original del evento. No hay APK compilado ni escucha A/B validada.

## Verificaciones
- `smoke-kick-tempo-window-audit.sh`: 12 pruebas correctas.
- `smoke-beat-tracking.sh`: correcta.
- `smoke-real-long-audit.sh`: procesó tres extractos reales de 24 segundos.
