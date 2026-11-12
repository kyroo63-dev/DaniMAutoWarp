# DaniM Auto Warp 4.0 RC110

- Corrección de persistencia: cada etiqueta se confirma en disco con `commit()` **antes** de avanzar o mostrarla en la interfaz; un fallo mantiene la selección actual.
- La importación CSV confirma el estado fusionado completo y solo después actualiza la interfaz.
- Reproductor: libera recursos incluso si fallan stop/reset; el callback de finalización comprueba identidad y se añade callback de error.
- Rechaza explícitamente una cola de revisión vacía para evitar acceso al índice 0.
- Los cambios en callbacks y persistencia requieren pruebas instrumentadas Android; las pruebas JVM no sustituyen compilación APK.
- El warp continuo permanece bloqueado hasta disponer de validación humana suficiente.
