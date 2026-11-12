# DaniM Auto Warp 4.0 RC56

- Evita repetir operaciones de arrastre cuando el marcador ya está en la posición calculada (tolerancia de 1 microsegundo).
- Protege los marcadores extremos también en el callback de edición.
- Añade `android-build-check.sh` para verificar SDK 35, disponibilidad de Gradle y ejecutar `assembleDebug` en un entorno Android preparado.
- Añade `smoke-rc56-integration.sh` como comprobación de integración estática; no sustituye la compilación real.

## Limitaciones
No hay SDK Android ni Gradle instalados en el entorno de generación. No se ha compilado ni probado un APK, ni validado la salida sonora en un dispositivo.
