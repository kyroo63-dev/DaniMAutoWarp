# DaniM Auto Warp 4.0 RC85

## Cambios
- Nuevo `KickSpectralGridAudit`: cruza la coincidencia con la fase de beatgrid ajustada con el score espectral de los candidatos agrupados.
- Para marcar una secuencia como fiable exige al menos 6 pulsaciones distintas, 5 consecutivas, cobertura >=70% y >=60% de candidatos con apoyo conjunto. Los umbrales son heurísticos, no calibrados contra anotaciones humanas.
- Nueva prueba `smoke-kick-spectral-grid.sh`: 12 casos superados.
- La auditoría real añade siete columnas `spectral_*` y `joint_*` al CSV.
- Diagnóstico solamente: no altera `BeatAnalyzer`, el audio procesado ni la autorización automática.

## Auditoría real (5 ventanas de 8 segundos de Expanden)
| Offset | Agrupados | Apoyo conjunto | Racha conjunta | Fiable |
|---|---:|---:|---:|---|
| 30s | 12 | 11 | 11 | sí |
| 75s | 18 | 18 | 18 | sí |
| 150s | 6 | 6 | 2 | no |
| 240s | 1 | 0 | 0 | no |
| 360s | 4 | 4 | 4 | no |

**Limitación:** un score espectral y una cuadrícula consistentes no prueban que sean bombos reales. Se necesita una referencia anotada manualmente y evaluación auditiva A/B. No se ha compilado un APK Android.
