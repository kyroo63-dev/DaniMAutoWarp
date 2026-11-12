# DaniM Auto Warp 4.0 RC50

- Nuevo `ManualBeatGrid.align`: alinea un bombo confirmado al pulso de una cuadrícula de BPM y fase configurables.
- Controles Compose de BPM y desfase en ms, con confirmación explícita.
- La operación añade un marcador si no existe o ajusta el destino del marcador existente, sin modificar los demás.
- Validación de BPM 40–240, fase ±5000 ms, orden estricto, distancia mínima y estiramiento local 0,85–1,15×.
- Deshacer mediante `commitMarkers` existente.
- Prueba JVM: `./smoke-manual-beatgrid.sh` (PASS).
- Otras pruebas locales superadas: `smoke-manual-kick-anchor.sh`, `smoke-manual-phase-nudge.sh`, `smoke-phase-window-navigator.sh`.
- `smoke-low-confidence.sh` produjo dos PASS pero la ejecución global excedió el tiempo disponible.
- No se ha compilado APK ni probado la UI en dispositivo Android.
- El BPM predeterminado del editor (136.0) es solo una sugerencia editable; no representa un tempo validado.
