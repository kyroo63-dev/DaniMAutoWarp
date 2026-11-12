# DaniM Auto Warp 4.0 RC94

## Comparación reproducible del agrupamiento con evidencia real

Se han reutilizado los 22 segmentos y los 1.762 candidatos KICK de la auditoría RC91 del FLAC real «Javi Golo & Andres Gomes - Expanden». El script `compare-cluster-on-real-audit.py` reproduce la agrupación por enlace consecutivo anterior y la agrupación anclada a primer onset de RC93, usando el BPM de referencia y el score espectral registrado.

- Agrupación anterior: 490 candidatos retenidos.
- Agrupación anclada: 998 candidatos retenidos.
- Diferencia: 508 eventos más; 16 de 22 segmentos aumentan.
- No es una medición de precisión, recall ni de bombos verdaderos: no hay anotación manual de referencia.
- La comparación NO reejecuta BeatAnalyzer ni altera los permisos de warp de producción.
- El script requiere los CSV de RC91 y produce `rc94-cluster-comparison.csv`.

Pruebas: `smoke-kick-candidate-cluster.sh`, `smoke-kick-grid-continuity.sh`, `smoke-kick-boundary-continuity.sh`.

Pendiente: contrastar falsos positivos y precisión de fase con anotaciones de referencia; compilación y ejecución Android; escucha A/B real.
