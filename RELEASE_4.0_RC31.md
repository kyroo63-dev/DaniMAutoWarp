# DaniM Auto Warp 4.0 RC31

- Prueba de integración reproducible: señal estéreo sintética -> WSOLA con tres anclajes de tiempo -> exportación WAV PCM16 -> importación WAV.
- Verifica duración (2,2 s), frecuencia de muestreo, canales, muestras finitas y audio no silencioso.
- Comprueba que los anclajes Warp con tiempos de destino no crecientes se rechazan.
- Añadidas dos pruebas JUnit para la misma ruta de audio, que se ejecutarán en CI mediante Gradle.
- Versión Android: 4.0.0-rc31, versionCode 70.
- El test local de integración con kotlinc se ha ejecutado. No hay SDK Android ni APK validado en este entorno.
