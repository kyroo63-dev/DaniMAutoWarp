# DaniM Auto Warp 4.0 RC80

- Integración conservadora de `KickRhythmEvidence.consecutiveSupport` en `BeatAnalyzer`.
- Con seis o más candidatos, si menos de tres muestran apoyo rítmico >=0.30, la confianza automática pasa a cero. No aumenta la confianza en ningún caso.
- Se corrigió el listado de dependencias del script `smoke-beat-tracking.sh`.
- Pruebas: `smoke-consecutive-support.sh` (seis escenarios), `smoke-beat-tracking.sh` y auditoría real de cinco fragmentos de 8 s del FLAC Expanden.
- Resultados de auditoría: 30s: 15 kicks/0.0; 75s: 19/0.95; 150s: 40/0.0; 240s: 1/0.0; 360s: 7/0.0. Solo 75s autoriza warp automático.
- Limitaciones: sin verdad-terreno manual para bombos, sin prueba auditiva A/B ni compilación APK. El archivo FLAC no se incluye.
