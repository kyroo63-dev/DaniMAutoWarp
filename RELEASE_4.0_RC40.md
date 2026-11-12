# DaniM Auto Warp 4.0 RC40

## Cambio
- Nuevo `KickPhaseEvidence`: compara posiciones del beatgrid con ataques de bombo realmente detectados; exige al menos 55 % de soporte y mediana de error no superior a 35 ms. Rechaza pulsos interpolados sin respaldo, ataques a contratiempo y entradas no válidas.
- `BeatAnalyzer` incluye este factor en la confianza antes de autorizar Warp automático.
- Nueva prueba `smoke-kick-phase.sh` (alineado, disperso, contratiempo y fluctuación leve).
- Se actualizaron los scripts de pruebas que compilan el analizador.

## Validación local
- `smoke-kick-phase.sh`: PASS.
- `smoke-low-confidence.sh`: PASS.
- `smoke-beat-tracking.sh`: PASS (120 BPM, 27 beats, 29 markers en señal sintética).
- `smoke-warp-pipeline.sh`: PASS.
- `smoke-real-vinyl.sh`: seis ventanas de Expanden a 136.36 BPM, confianza 0.0, dos marcadores identidad por ventana; el warp sigue bloqueado.

## Pendiente
- Corregir identificación y fase de bombo en vinilo real para obtener confianza positiva.
- Compilación y ejecución Android, validación visual de UI, y comparación auditiva A/B; no se ha generado APK.
