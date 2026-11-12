# DaniM Auto Warp 4.0 RC52

## Cambios
- Superposición visual no destructiva del bombo origen (naranja) y el destino BPM (blanco) en la forma de onda.
- Recorte al viewport actual: las líneas solo aparecen cuando su posición es visible; el zoom permite distinguir diferencias de milisegundos.
- La vista previa no añade ni altera marcadores. El botón de confirmación sigue siendo la única acción de alineación.
- Proyección independiente `ManualGridOverlay.project` con prueba JVM.

## Validación
- `smoke-manual-grid-overlay.sh` y `smoke-manual-grid-preview.sh`.
- Sin SDK Android en este entorno: no se ha compilado APK ni probado la interfaz en un dispositivo.
- No se ha repetido la prueba auditiva con «Expanden».
