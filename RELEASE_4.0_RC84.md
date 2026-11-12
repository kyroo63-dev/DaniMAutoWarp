# DaniM Auto Warp 4.0 RC84

## Cambios
- Nuevo `KickGridContinuityAudit`: ajusta una fase de cuadrícula a candidatos ya agrupados y calcula pulsaciones distintas, huecos internos, racha máxima y cobertura.
- Nueva prueba `smoke-kick-grid-continuity.sh` con 12 comprobaciones.
- `smoke-real-detector-audit.sh` añade columnas `grid_*` al CSV.
- La auditoría es **solo diagnóstica**: no modifica BeatAnalyzer, marcadores ni la autorización de warp.

## Prueba real (cinco ventanas de 8 segundos de Expanden)
| Offset | Candidatos agrupados | Beats ajustados | Huecos | Racha | Fiable según heurística |
|---|---:|---:|---:|---:|---|
| 30 s | 12 | 11 | 0 | 11 | sí |
| 75 s | 18 | 18 | 0 | 18 | sí |
| 150 s | 6 | 6 | 12 | 2 | no |
| 240 s | 1 | 1 | 0 | 1 | no |
| 360 s | 4 | 4 | 0 | 4 | sí |

**Precaución:** una cuadrícula consistente no demuestra que los candidatos sean bombos reales. En 360 s la heurística marca fiabilidad con solo cuatro eventos; no debe tomarse como validación musical ni permitir warp por sí sola. Las decisiones del detector de producción no cambian.

No se ha compilado un APK ni realizado una evaluación auditiva A/B.
