# DaniM Auto Warp 4.0 RC108

## Cambios reales
- Se extrae el cálculo de picos de la pantalla Android a `ReviewWav.kt`, sin dependencias Android y probado mediante `kotlinc`.
- El parser valida la cabecera RIFF/WAVE, longitud declarada, chunks `fmt ` y `data`, padding, formato PCM16 y mono/estéreo. Ya no presupone que los datos comienzan en el byte 44.
- Se liberan los recursos del reproductor en `onStop`, `onDestroy`, al cambiar de ataque, al terminar el clip y cuando ocurre un error al abrirlo.
- Se conservan las clasificaciones y la importación/exportación CSV de RC106.

## Pruebas
- 9 comprobaciones JVM/Kotlin ejecutadas, incluido un clip WAV real empaquetado, encabezados truncados, formato no compatible y un chunk extra antes de `fmt `.
- No se ha compilado APK: no hay SDK Android/Gradle disponible en este entorno.
- No se ha probado reproducción en dispositivo ni escucha A/B, ni se ha habilitado warp continuo.
