# DaniM Auto Warp 4.0 RC72

## Optimización del motor de producción
En `StreamingWsolaProcessor.kt` se sustituyó la búsqueda lineal del anclaje de transitorio más cercano en cada grano (`minByOrNull`) por búsqueda binaria sobre los anclajes ordenados por tiempo de destino. La selección conserva el desempate hacia el anclaje anterior y el umbral de proximidad de 0,55 saltos. Complejidad por grano: de O(N) a O(log N), donde N es el número de transitorios. No se afirma una mejora medida de tiempo real ni de calidad auditiva.

## Pruebas reproducibles
Se ejecutó `smoke-multisegment-transient-modes.sh` en RC71 y RC72 con la grabación original `Javi Golo & Andres Gomes - Expanden.flac`, cinco fragmentos de 3 segundos (30, 75, 150, 240, 360 s), factores 0,99 y 1,01 y tres modos: 30 renderizados por versión. Se compararon las 40 líneas de métricas de modo y diferencias; coinciden todas exactamente. Cada ejecución terminó correctamente.

## Limitaciones
Las mediciones de error temporal y RMS no equivalen a evaluación auditiva. Los cinco eventos de cada fragmento son candidatos de amplitud, no bombos clasificados. No se ha compilado APK Android. La grabación del usuario no está incluida en el paquete.
