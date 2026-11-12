# DaniM Auto Warp 4.0 RC18

- WAV PCM24 y AIFF PCM24 añaden el byte de relleno cuando el audio ocupa un número impar de bytes.
- Los tamaños RIFF/FORM incluyen el relleno y `data`/`SSND` conservan su longitud real.
- Añadida prueba JUnit de un archivo mono con una sola muestra.
- Núcleo Kotlin compilado; prueba independiente de contenedores y lectura `ffprobe` superadas.
- APK y reproducción en Android no verificados; MP3 320 kbps pendiente.
