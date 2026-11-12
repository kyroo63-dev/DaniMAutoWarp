# DaniM Auto Warp 4.0 RC5 — robustez de reproducción A/B

- Rechaza una posición inicial fuera de la duración del audio corregido antes de detener la reproducción anterior.
- Verifica la longitud de los bloques PCM recibidos para evitar errores de índices silenciosos.
- Convierte muestras no finitas (NaN/infinito) a silencio antes de convertir a PCM16.
- Añade tres pruebas unitarias JUnit de fundido A/B: señal idéntica, inversiones rápidas y canales estéreo.

## Validación pendiente
- Compilación Gradle completa y pruebas instrumentadas en Android físico.
- Pruebas perceptuales con material real de vinilo.
- Exportación MP3 320 kbps todavía no implementada.
