# DaniM Auto Warp 4.0 RC32

- Añadido `smoke-beat-tracking.sh`: prueba reproducible con bombo sintético a ~120 BPM, pequeñas variaciones de tempo y caja alterna.
- Comprueba BPM plausible, seguimiento de beats, marcadores Warp estrictamente crecientes y valores numéricos válidos.
- Añadida prueba JUnit `VaryingTempoBeatAnalyzerTest` para ejecución futura en Android CI.
- Versión `4.0.0-rc32`, `versionCode 71`.
- Pruebas locales superadas: smoke-core, smoke-riff (11 archivos malformados), smoke-warp-pipeline y smoke-beat-tracking.
- Sin SDK Android local: no se ha compilado ni probado un APK; la prueba JUnit no se ha ejecutado con Gradle aquí.
