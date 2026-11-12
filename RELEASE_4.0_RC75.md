# DaniM Auto Warp 4.0 RC75

## Implementado
- `KickDensityGate.kt`: compuerta conservadora que impide autorizar warp automático si el detector produce más de 3,5 candidatos de bombo por segundo. No elimina los eventos para revisión manual. Este límite es heurístico, no un clasificador entrenado.
- `BeatAnalyzer.kt`: aplica la compuerta de densidad al cálculo de confianza antes de autorizar marcadores de corrección automática.
- `smoke-kick-density.sh`: pruebas unitarias del límite.
- `smoke-real-detector-audit.sh`: ejecuta el `BeatAnalyzer` real sobre cinco fragmentos de 8 segundos (30, 75, 150, 240 y 360 s), decodificados a PCM16 WAV estéreo 44,1 kHz con ffmpeg, y genera CSV. Requiere un archivo de audio externo.

## Resultados reales: Expanden
| Inicio (s) | BPM indicado | Confianza | Candidatos bombo | Otros transitorios | Warp automático |
|---:|---:|---:|---:|---:|---|
| 30 | 133,33 | 0,00 | 15 | 61 | No |
| 75 | 136,36 | 0,95 | 19 | 66 | Sí |
| 150 | 136,36 | 0,00 | 40 | 66 | No |
| 240 | 136,36 | 0,00 | 1 | 69 | No |
| 360 | 136,36 | 0,00 | 7 | 71 | No |

**Limitaciones:** estas son etiquetas heurísticas del detector, no ground truth auditado de bombos/cajas. El fragmento de 75 s necesita revisión manual antes de afirmar que el warp es musicalmente correcto. No hay escucha A/B validada ni APK compilado. El audio del usuario no se distribuye en el ZIP.
