# DaniM Auto Warp 4.0 RC76

## Cambios
- `KickDensityGate.assessTimeline`: comprueba tanto densidad global como máxima densidad en ventanas deslizantes de 2 s. Evita que ráfagas de detecciones queden ocultas por una media global baja. Usa dos punteros sobre eventos ordenados, coste O(n log n) incluida la ordenación.
- `BeatAnalyzer`: aplica la nueva compuerta antes de autorizar el warp automático; los candidatos siguen disponibles para revisión manual.
- `smoke-kick-window-density.sh`: cinco pruebas unitarias reproducibles.
- `smoke-real-detector-audit.sh`: regresión sobre cinco fragmentos de Expanden, de 8 s cada uno, a 44,1 kHz estéreo.

## Resultado de la regresión real
| Inicio (s) | Candidatos bombo | Confianza | Warp automático |
|---:|---:|---:|---|
| 30 | 15 | 0,00 | No |
| 75 | 19 | 0,95 | Sí |
| 150 | 40 | 0,00 | No |
| 240 | 1 | 0,00 | No |
| 360 | 7 | 0,00 | No |

No cambia la autorización respecto a RC75 para estos cinco fragmentos; la mejora es una salvaguarda para ráfagas no cubiertas por la media global. No se ha validado la exactitud musical de las etiquetas. Pendientes APK, escucha A/B y validación de grabación completa. El archivo de audio del usuario no se distribuye.
