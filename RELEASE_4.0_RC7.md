# DaniM Auto Warp 4.0 RC7 — estabilización

- Validación de muestras no finitas en el fundido A/B para evitar contaminar el resultado de la mezcla.
- Selección de fuente A/B capturada una sola vez por fotograma estéreo.
- Nuevas pruebas JUnit para conmutación rápida, unidad de ganancia y valores no finitos.

## Estado y límites

El núcleo de audio independiente de Android puede compilarse con `kotlinc`. Las pruebas JUnit se incluyen pero no se han ejecutado con Gradle. **No se ha compilado ni probado un APK Android**: el entorno carece del SDK Android y de Gradle. La exportación MP3 a 320 kbps no está implementada. Esta es una candidata, no una versión final certificada.

## Validación pendiente

Abrir el proyecto en Android Studio con SDK 35, ejecutar `testDebugUnitTest` y `assembleDebug`, instalar en dispositivo Android 8+ y probar importación MP3/WAV/AIFF, auto/manual Warp, A/B, guardado y exportación. Comparar auditivamente grabaciones reales de vinilo.
