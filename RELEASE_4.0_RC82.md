# DaniM Auto Warp 4.0 RC82

## Implementado
- `KickIntervalAudit`: diagnóstico no destructivo de intervalos entre candidatos de bombo.
- Clasificación de intervalos cortos, pulsaciones consecutivas, posibles pulsaciones omitidas e intervalos irregulares, con longitud máxima de racha.
- `smoke-kick-interval-audit.sh`: ocho pruebas unitarias.
- `smoke-real-detector-audit.sh`: nuevas columnas de auditoría por fragmento.

## Auditoría de audio real (cinco segmentos de 8 s de Expanden)
- 0:30: 15 candidatos, 3 intervalos cortos, 8 intervalos de pulso, warp bloqueado.
- 1:15: 19 candidatos, 1 intervalo corto, 15 intervalos de pulso, warp autorizado por controles preexistentes.
- 2:30: 40 candidatos, 34 intervalos cortos, 3 intervalos de pulso, warp bloqueado.
- 4:00: 1 candidato, sin intervalos, warp bloqueado.
- 6:00: 7 candidatos, 3 intervalos cortos, 1 intervalo de pulso, warp bloqueado.

**Limitaciones:** no hay etiquetas de referencia manuales ni validación auditiva A/B; la autorización del warp no se modifica en RC82. No se ha compilado ni ejecutado un APK Android. La grabación original no se distribuye.
