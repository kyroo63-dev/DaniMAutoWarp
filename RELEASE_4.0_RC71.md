# DaniM Auto Warp 4.0 RC71

## Validación multisegmento
Se añadió `smoke-multisegment-transient-modes.sh`, que compila el motor Kotlin de producción y analiza cinco extractos de 3 segundos de «Expanden» (posiciones 30, 75, 150, 240 y 360 s) a 44.1 kHz estéreo, factores 0.99 y 1.01 y modos LEGACY, OFFSET_AWARE y ADAPTIVE: 30 renderizados en total. Genera CSV de mediciones en un directorio temporal y muestra errores temporales y cocientes RMS por candidato. Se han ejecutado los 30 renderizados sin fallos de las comprobaciones técnicas.

## Hallazgo y decisión
La heurística ADAPTIVE de RC70 no mejora todos los fragmentos. En factor 0.99, respecto a LEGACY, el error temporal medio empeora 0.18 ms en 30 s, mejora 1.17 ms en 75 s, empeora 1.47 ms en 150 s, mejora 0.93 ms en 240 s y empeora 1.90 ms en 360 s. El cociente RMS también disminuye en tres fragmentos. No se cambia el algoritmo de producción en RC71 para evitar presentar una mejora no demostrada.

## Limitaciones
Los eventos son máximos de amplitud próximos a una rejilla de medio segundo y no bombos clasificados de forma fiable. Los fragmentos son de 3 s, no pistas completas. El cociente RMS local y la distancia entre máximos no sustituyen la escucha crítica. Sin compilación APK Android ni validación perceptual.

## Reproducción
`./smoke-multisegment-transient-modes.sh '/ruta/a/Expanden.flac'`
