# DaniM Auto Warp 4.0 RC9

## Cambios
- Extraída la conversión PCM16 y la interpolación estéreo a `Pcm16SampleMath` para que puedan probarse sin Android.
- `ContinuousAbPreview` utiliza la misma conversión de entrada/salida para ambas señales.
- Añadidas pruebas JUnit de límites, muestras no finitas y separación de canales.

## Estado de validación
- Compilación JVM del módulo de muestras: verificada con kotlinc.
- Prueba JVM independiente de límites/interpolación: verificada.
- APK Android completo: no compilado en este entorno (SDK/Gradle Android no disponibles).
- Reproducción real y calidad sonora: no verificadas en teléfono.
- MP3 320 kbps: no implementado.

No publicar como estable hasta superar compilación y pruebas integrales en Android.
