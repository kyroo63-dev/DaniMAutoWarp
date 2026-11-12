# DaniM Auto Warp 4.0 RC28

- Corrige un fallo de estado de la preescucha A/B: si AudioTrack falla durante la configuración o construcción, se restablece `running` para evitar que la interfaz quede bloqueada en reproducción.
- La limpieza de sesiones anteriores conserva la protección por `sessionCounter` para no detener una sesión más reciente.
- Versión Android: `4.0.0-rc28`, código 67.
- Verificación: pruebas Kotlin del núcleo y comprobación estática; **APK Android no compilado ni probado**.
- MP3 a 320 kbps sigue sin implementarse.
