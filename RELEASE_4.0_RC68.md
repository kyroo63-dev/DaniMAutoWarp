# DaniM Auto Warp 4.0 RC68

## Alcance

- Nueva prueba reproducible `smoke-small-warp-stereo.sh` sobre un fragmento real de 3 s de «Expanden», decodificado a PCM16 estéreo a 44,1 kHz.
- Seis renderizados: factores de duración 0,99 y 1,01 con los tres modos de anclaje.
- Validación de estructura WAV, muestras finitas, memoria acotada y saltos entre bloques.
- No se suministraron anclajes de transitorios a estas pruebas; por ello los tres modos producen resultados iguales.
- Sin modificación de la ruta de procesamiento WSOLA respecto a RC67.

## Resultados

| Factor | Salto máximo límite de bloque | Salto medio límite | Salto medio otros |
|---|---:|---:|---:|
| 0,99 | 0,26425 | 0,03781 | 0,03977 |
| 1,01 | 0,30881 | 0,03970 | 0,04126 |

Las amplitudes son PCM normalizado. Se aplicaron umbrales de comprobación, no criterios de calidad auditiva. Las pruebas no demuestran ausencia de artefactos ni corrección fiable del beatgrid. APK no compilado.

Para repetir: `./smoke-small-warp-stereo.sh /ruta/al/audio.flac`. El archivo musical no se distribuye con el proyecto.
