# DaniM Auto Warp 4.0 RC66

## Prueba real WSOLA con «Expanden»

Nuevo `smoke-real-wsola-comparison.sh`: extrae 8 s desde 75 s de la grabación FLAC (mono 8 kHz para reducir coste del test), selecciona 13 picos locales de energía como **candidatos**, y ejecuta los tres modos de anclaje (`LEGACY`, `OFFSET_AWARE`, `ADAPTIVE`) para tres factores de duración (0.95, 1.00, 1.05). Verifica estructura de los nueve WAV PCM16, memoria de lectura acotada y métricas finitas, y genera CSV individuales en un directorio temporal (eliminados al finalizar). No incorpora ni redistribuye el audio original.

## Resultados reales observados

| Factor | Modo | Error medio ms | Energía RMS relativa |
|---|---|---:|---:|
| 0.95 | LEGACY | 18.52 | 0.875 |
| 0.95 | OFFSET_AWARE / ADAPTIVE | 16.26 | 0.845 |
| 1.00 | LEGACY / ADAPTIVE | 4.80 | 0.903 |
| 1.00 | OFFSET_AWARE | 0.00 | 1.000 |
| 1.05 | LEGACY / ADAPTIVE | 14.13 | 0.920 |
| 1.05 | OFFSET_AWARE | 15.57 | 0.900 |

Los resultados demuestran una limitación del selector adaptativo actual, especialmente en factor 1.0, donde OFFSET_AWARE ofrece una reconstrucción idéntica en los eventos analizados. Los candidatos por energía no son golpes de bombo confirmados. El análisis es mono a 8 kHz y **no** valida calidad de producción a 44.1 kHz estéreo. No se afirma que WSOLA haya corregido el beatgrid de «Expanden» ni que la calidad auditiva sea aceptable.

## Ejecución

`./smoke-real-wsola-comparison.sh '/ruta/al/archivo.flac'`

Requiere ffmpeg, kotlinc y JRE. El archivo de entrada no se empaqueta. No se ha compilado APK Android.
