# DaniM Auto Warp 4.0 RC74

## Cambio de código
- Nuevo `KickOverlapEvidence`: ajuste conservador de confianza cuando el ataque está dominado por medios/agudos y la evidencia de graves es débil.
- Integrado en `BeatAnalyzer` para los candidatos a bombo. No descarta golpes automáticamente ni inventa marcadores.
- `versionCode=113`, `versionName=4.0.0-rc74`.

## Validación ejecutada
- `./smoke-kick-overlap.sh`: PASS, cuatro casos sintéticos deterministas.
- `./smoke-multisegment-transient-modes.sh`: PASS, 30 renderizados WSOLA sobre cinco fragmentos reales de Expanden, a 44.1 kHz estéreo, ±1 %, con candidatos de alta energía.

## Limitaciones
- La prueba de 30 renderizados utiliza candidatos de energía y **no** ejecuta el nuevo detector BeatAnalyzer: no valida una mejora en clasificación real de bombos.
- Sin etiquetas de referencia de instrumentos, no se puede calcular precisión/recall.
- Sin compilación Android ni APK; no hay escucha A/B validada.
- El audio del usuario no se distribuye en el ZIP.
