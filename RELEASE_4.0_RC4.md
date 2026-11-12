# DaniM Auto Warp 4.0 RC4

- Corrige el salto de ganancia al invertir rápidamente ORIGINAL/CORREGIDO durante un fundido.
- La interpolación lineal mantiene la suma de ganancias en 1 para señales correlacionadas.
- El cambio conserva la ganancia actual al invertir dirección y evita reiniciar desde el extremo anterior.

## Verificación
- Compilación Kotlin del módulo y prueba JVM de continuidad de cambios rápidos.
- Pendiente: compilación Gradle Android, APK, dispositivo real, evaluación perceptual con vinilos.
- MP3 320 kbps aún no disponible; no se anuncia como implementado.
