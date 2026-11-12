# DaniM Auto Warp 4.0 RC33

- Corrige el desbordamiento de `sampleRate * channels * 2` al escribir la cabecera WAV PCM16.
- Comprueba el límite unsigned de 32 bits de `byteRate` antes de escribir un archivo.
- Amplía la prueba local con un caso de frecuencia extrema rechazado y otro de frecuencia elevada representable.
- Versión Android `4.0.0-rc33` (`versionCode=72`).

No se ha compilado un APK Android: faltan Android SDK y Gradle en el entorno local.
