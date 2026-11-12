# DaniM Auto Warp 4.0 RC93

- `KickCandidateCluster`: agrupación de proximidad anclada al primer ataque de cada grupo; evita la fusión transitiva de ataques de percusión.
- `smoke-kick-candidate-cluster.sh`: 12 casos superados.
- `smoke-kick-grid-continuity.sh`: 12 casos superados.
- `smoke-kick-boundary-continuity.sh`: 13 casos superados.
- Auditoría de eventos: `reported_event_confidence` queda vacío para KICK, pues `AnalysisResult.kickTimes` no expone la confianza individual; no se utiliza el sustituto engañoso de 1.0.
- Cambios exclusivamente diagnósticos; no alteran decisiones de warp de producción.
- No hay compilación de APK ni validación auditiva A/B.
