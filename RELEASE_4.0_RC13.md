# DaniM Auto Warp 4.0 RC13

## Cambios
- Token de generación de sesión para impedir que un reproductor A/B antiguo continúe escribiendo después de iniciar una sesión nueva.
- Comprobación del token durante la inicialización, reproducción y escritura.
- Un reproductor anterior no restablece el estado de una sesión nueva en su bloque finally.

## Limitaciones
- No se ha compilado el APK ni probado AudioTrack en un teléfono Android.
- La sincronización completa de la instalación de AudioTrack y el acceso a las fuentes de audio todavía necesita pruebas de estrés en dispositivo.
- Exportación MP3 320 kbps no implementada.
