# DaniM Auto Warp 4.0 RC20

- Corrige el escritor WAV PCM16 en memoria: NaN y valores infinitos se convierten en silencio, y los valores fuera de rango se saturan correctamente.
- Añade prueba JUnit para valores NaN, +Inf, -Inf, saturación positiva/negativa y 0.5.
- No se afirma compilación APK ni ejecución JUnit con Gradle sin Android SDK/Gradle.
- MP3 320 kbps sigue pendiente.
