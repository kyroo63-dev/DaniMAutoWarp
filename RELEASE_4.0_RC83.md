# DaniM Auto Warp 4.0 RC83

- `KickCandidateCluster` agrupa ataques graves consecutivos a menos del 55 % del periodo de pulso estimado y conserva el candidato de mayor puntuación espectral (desempate por tiempo más temprano).
- Se añaden 9 pruebas sintéticas en `smoke-kick-candidate-cluster.sh`.
- `smoke-real-detector-audit.sh` exporta `cluster_retained` y `cluster_suppressed` para cinco cortes reales de ocho segundos.
- El agrupamiento es **solo diagnóstico**: no altera las decisiones de `BeatAnalyzer`, los marcadores, ni la autorización automática. No hay anotaciones manuales que demuestren que los candidatos retenidos sean bombos reales.
- No se ha compilado un APK ni realizado validación auditiva A/B.
