# DaniM Auto Warp 4.0 RC48

## Implementado
- `ManualPhaseNudge`: ajuste manual de ±1, ±5 o ±10 ms de `targetSeconds` en marcadores interiores existentes de la ventana seleccionada.
- Validación de monotonicidad y ratio de estiramiento por tramo de 0.85 a 1.15; rechaza operaciones inseguras sin modificar el estado.
- Controles Compose de paso, fase −/+, y deshacer. Las operaciones aceptadas se registran con `commitMarkers` y activan modo manual.
- Pruebas sintéticas de ajuste, límites, ventana vacía, datos inválidos y continuidad.

## Pruebas ejecutadas
- `smoke-manual-phase-nudge.sh`: PASS
- `smoke-phase-window-navigator.sh`: PASS
- `smoke-phase-timeline.sh`: PASS
- `smoke-kick-phase-segments.sh`: PASS
- `smoke-low-confidence.sh`: PASS

## Límites
- Los ajustes solo funcionan cuando hay marcadores interiores existentes en la ventana. Con dos marcadores de seguridad no se generan marcadores nuevos automáticamente.
- No se ha compilado ni ejecutado la aplicación Android en un dispositivo: Android SDK/Gradle no están disponibles aquí.
- No se ha demostrado que el análisis automático de «Expanden» sea fiable; permanece la protección por baja confianza.
