# DaniM Auto Warp 4.0 RC69

## Alcance
Prueba reproducible de WSOLA con cinco anclajes de transitorios candidatos, extraídos de tres segundos de la grabación real «Expanden», 44,1 kHz, dos canales. Se evalúan los modos LEGACY, OFFSET_AWARE y ADAPTIVE para factores de duración 0.99 y 1.01.

## Resultados
| Factor | Modo | Error absoluto medio (ms) | Energía RMS relativa media | Anclajes bloqueados |
|---|---|---:|---:|---:|
| 0.99 | LEGACY | 4.09 | 0.826 | 5 |
| 0.99 | OFFSET_AWARE | 2.92 | 0.867 | 5 |
| 0.99 | ADAPTIVE | 4.09 | 0.826 | 5 |
| 1.01 | LEGACY | 4.35 | 0.795 | 5 |
| 1.01 | OFFSET_AWARE | 3.96 | 0.791 | 5 |
| 1.01 | ADAPTIVE | 4.35 | 0.795 | 5 |

## Reproducción
Ejecutar `./smoke-stereo-transient-modes.sh '/ruta/al/audio.flac'`. Requiere ffmpeg, kotlinc y java. Los CSV por evento se generan en un directorio temporal que se elimina al salir; modificar el script si se necesita conservarlos.

## Limitaciones
Los anclajes son máximos de amplitud alrededor de posiciones de medio segundo; no son etiquetas verificadas de bombo. Los resultados se refieren a un único fragmento de tres segundos y a la calidad FAST. No se ha hecho evaluación perceptual ni compilación de APK. La estrategia ADAPTIVE no supera a LEGACY en este fragmento. El código de producción del motor no se ha modificado en esta versión; se ha ampliado su batería de pruebas.
