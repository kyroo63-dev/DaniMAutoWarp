# DaniM Auto Warp 4.0 RC47

## Cambios
- `PhaseWindowNavigator`: selección por tiempo, encuadre limitado y consulta de bombos por ventana.
- `MainActivity`: toque sobre waveform para seleccionar ventana, ventana resaltada, navegación anterior/siguiente, botón para enfocar y salto a siguiente ventana dudosa.
- Panel de diagnóstico muestra el número de bombos, soporte y los primeros tiempos detectados.
- `smoke-phase-window-navigator.sh`: selección en bordes, NaN, límites de viewport y filtrado.

## Pruebas locales ejecutadas
- `smoke-phase-window-navigator.sh`: PASS.
- `smoke-phase-timeline.sh`: PASS.
- `smoke-kick-phase-segments.sh`: PASS.
- `smoke-low-confidence.sh`: PASS.

## Limitaciones
- No se dispone de SDK Android para compilar o ejecutar la interfaz Compose: los cambios de UI requieren verificación en Android Studio/dispositivo.
- No se ha realizado una nueva validación del algoritmo sobre la grabación completa de «Expanden» en esta versión.
- La selección de ventana es diagnóstico y navegación; no realiza por sí misma ajuste automático ni edición local de fase.
- No hay APK instalable verificado.
