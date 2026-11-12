# DaniM Auto Warp 4.0 RC92

- Nuevo `KickBoundaryContinuityAudit.kt` (diagnóstico exclusivamente): verifica adyacencia temporal, elegibilidad conservadora, salto relativo de BPM y diferencia de fase en segundos absolutos.
- `smoke-kick-boundary-continuity.sh`: 13 casos superados; incluye un caso de regresión en el que una pequeña diferencia de BPM acumula un salto de fase tras 24 s.
- `audit-boundaries.py`: genera auditoría de 21 uniones a partir de la auditoría RC91. La fase se ajusta con candidatos KICK **sin agrupar** de clips independientes; no es una verdad de referencia ni valida la identificación real del bombo.
- En «Expanden»: 21 uniones contiguas, 0 elegibles para warp conjunto, 0 autorizadas. Los dos clips autorizados por el detector de producción no son contiguos.
- La auditoría no altera decisiones de producción ni aplica warp al archivo original.
- Limitaciones: no hay SDK Android/Gradle disponible para compilar APK ni prueba auditiva A/B. El campo `detector_confidence` del CSV de eventos heredado se construye con un valor sustituto de 1.0 y **no** representa la confianza real por evento.
