# DaniM Auto Warp 4.0 RC79

## Cambio
`KickRhythmEvidence.consecutiveSupport` calcula el respaldo de pulsaciones consecutivas en slots temporales únicos, ponderando las puntuaciones de fase existentes. Un golpe aislado no recibe respaldo; dos golpes reciben respaldo parcial y tres o más consecutivos reciben respaldo completo. La métrica es diagnóstica, no autoriza warp automático.

## Pruebas
- `smoke-consecutive-support.sh`: seis pruebas sintéticas superadas.
- `smoke-real-detector-audit.sh`: cinco extractos de ocho segundos del FLAC de referencia analizados; CSV de eventos incluye `consecutive_support`.
- La decisión de warp automático permanece sin cambios respecto a RC78: solo 75 segundos se autoriza en estos extractos.

## Limitaciones
No hay etiquetas manuales de verdad-terreno para medir precisión/recall. No se ha compilado APK ni realizado escucha A/B en Android. No se distribuye el FLAC original.
