# DaniM Auto Warp 4.0 RC73

## Nuevo modo experimental
`TransientAnchorMode.CONTENT_AWARE` selecciona anclaje con desplazamiento por evento solo cuando la confianza del transitorio es >=0,85, la separación del evento al inicio del salto es >=0,15 saltos y el ratio local es >1. El modo predeterminado `ADAPTIVE` no cambia. El nuevo modo es experimental y no se activa por defecto.

## Validación
`bash smoke-multisegment-transient-modes.sh /ruta/a/grabacion.flac` procesa cinco fragmentos de tres segundos de la grabación de vinilo, dos factores (0,99 y 1,01) y cuatro modos: **40 renderizados** a 44.100 Hz estéreo. El script genera métricas y CSV temporales. Los candidatos a transitorio se derivan de picos de amplitud, no de una clasificación fiable de bombos.

La evaluación no ha demostrado superioridad consistente del modo CONTENT_AWARE: en algunos fragmentos cambia el resultado, en otros coincide con otros modos. No se ha activado automáticamente. No se ha realizado escucha A/B ni compilación APK.

La grabación original no se distribuye con el proyecto.
