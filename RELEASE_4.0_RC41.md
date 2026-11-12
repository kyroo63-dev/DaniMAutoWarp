# DaniM Auto Warp 4.0 RC41

## Cambio
- La validación de fase asigna cada golpe físico de bombo como máximo a un beat. Impide inflar artificialmente la confianza reutilizando un único ataque en varios pulsos de una cuadrícula demasiado densa.
- Rechaza beats con tiempos NaN/infinito.
- Nuevas pruebas de regresión para emparejamiento único y tiempos no finitos.

## Pruebas locales ejecutadas
- smoke-kick-phase.sh: PASS (6 escenarios).
- smoke-low-confidence.sh: PASS.
- smoke-warp-pipeline.sh: PASS (WSOLA estéreo, WAV PCM16, lectura).
- smoke-beat-tracking.sh: PASS (señal sintética).
- smoke-riff.sh: PASS (11 casos RIFF incorrectos).
- smoke-riff-padding.sh: PASS.

## Limitaciones
- No se ha compilado ni ejecutado APK Android en esta ronda.
- No se ha validado la corrección completa de Expanden ni realizado escucha A/B.
- La corrección automática continúa bloqueada cuando la confianza es insuficiente.
