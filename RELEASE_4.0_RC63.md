# DaniM Auto Warp 4.0 RC63

## Cambios
- `audio/TransientFidelityReport.kt`: informe por cada transitorio con error temporal, ratio de energía, resumen y CSV.
- `smoke-wsola-fidelity-report.sh`: prueba del WSOLA real con cinco bombos sintéticos y tres duraciones.
- Se mantienen las comprobaciones anteriores de fidelidad y transitorios.

## Resultados de la prueba
| Duración | Error temporal máximo | Ratio medio de energía |
|---|---:|---:|
| 2,85 s | 17,25 ms | 0,612 |
| 3,00 s | 10,63 ms | 0,796 |
| 3,15 s | 6,62 ms | 0,852 |

Los resultados son diagnósticos: no prueban calidad sonora ni preservación perceptual de los transitorios. El módulo de informe aún no está integrado en la interfaz Android.

## Pendientes
- Evaluar y mejorar el WSOLA para preservar mejor los ataques.
- Compilar APK en entorno con SDK Android y validar escucha A/B con audio real.
