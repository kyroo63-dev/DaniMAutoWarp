# DaniM Auto Warp 4.0 RC11

## Cambios
- Evita liberar AudioTrack desde `stop()` mientras otro hilo está escribiendo; el hilo de reproducción es propietario del cierre.
- Rechaza ventanas de lectura originales cuyo tamaño no cabe en un buffer PCM válido.
- Incrementa versionCode y versionName.

## Pendiente para versión estable
- Compilación completa Gradle/Android SDK y pruebas en dispositivo.
- Pruebas de estrés de inicio/parada de reproducción y escucha real.
- Validación con grabaciones de vinilo.
- MP3 320 kbps no implementado.
