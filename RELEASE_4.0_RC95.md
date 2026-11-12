# DaniM Auto Warp 4.0 RC95

- Añadido `KickCandidateEvidenceGate.kt`: filtro **exclusivamente diagnóstico** de proporción de graves, puntuación espectral y vecinos a 1-2 pulsos de distancia, con tolerancia de 16% del periodo. No se conecta al render ni habilita warp.
- 12 pruebas sintéticas nuevas del filtro superadas.
- Añadido `audit-rc95-evidence.py`, réplica exploratoria del filtro para los eventos ya medidos en RC91. Se aplica a eventos KICK anteriores al nuevo agrupamiento, por lo que **no valida los 508 candidatos adicionales de RC94**.
- En 22 segmentos se conservan 1521 de 1762 eventos KICK. Esta cifra **no mide precisión**, porque no hay anotaciones humanas de bombos verdaderos, y la evidencia espectral puede saturarse.
- Pendiente: reevaluar audio original con el nuevo agrupamiento, revisar falsos positivos manualmente, compilar APK y validar A/B.
