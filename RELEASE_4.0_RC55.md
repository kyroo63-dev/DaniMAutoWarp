# DaniM Auto Warp 4.0 RC55

## Cambios
- Edición táctil con desplazamiento relativo al punto inicial del gesto, sin saltar el marcador a la posición del dedo.
- Ajustes finos limitados por los marcadores adyacentes con separación mínima de 1 ms.
- Registro de deshacer solo cuando hay un movimiento efectivo durante el gesto.
- Limpieza de selección temporal al finalizar o cancelar el arrastre.
- Nuevo `MarkerDragPrecision.kt` y prueba JVM `smoke-marker-drag-precision.sh`.

## Validación
- Pruebas JVM de arrastre y selección táctil: PASS.
- Sin SDK Android disponible en este entorno: no se ha compilado un APK ni probado el audio en un dispositivo.
