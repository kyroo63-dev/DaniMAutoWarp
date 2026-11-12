# DaniM Auto Warp 4.0 RC38 — validación con vinilo real

- Se actualiza `smoke-real-vinyl.sh` para evaluar seis ventanas de 60 s a 0, 80, 160, 240, 320 y 420 segundos del FLAC proporcionado.
- Se corrige la lista de dependencias de compilación del smoke test para incluir `TempoContinuityGate`.
- Se incorpora una aserción de seguridad: cualquier análisis con confianza <0,20 debe mantener todos los marcadores sobre la diagonal identidad.
- Resultados de la grabación real: las seis ventanas reportan 136,36 BPM, confianza 0,00 y 2 marcadores identidad. No hay corrección automática fiable en estas ventanas.
- La prueba abarca 360 segundos de audio de los 507,48 segundos totales; **no** es un seguimiento continuo de la canción completa.
- Falta validar la detección de los bombos reales y el seguimiento continuo, realizar escucha A/B, compilar Android y probar la interfaz.
