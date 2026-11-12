# DaniM Auto Warp 4.0 RC101

- Nueva auditoría reproducible `audit-rc101-attack-competition.py` sobre FLAC real, usando ventanas de 8 s en cada lado de las 4 uniones de fase preliminares.
- Características independientes medidas: energía grave (30–160 Hz respecto a 30–5000 Hz), centroide espectral y relación energética antes/después del ataque.
- Resultados: 8 ventanas, 260 eventos KICK históricos, 518 otros eventos; todas las ventanas tienen ataques graves competidores.
- Las etiquetas son del detector histórico RC91, **no ground truth**. El cálculo no demuestra clasificación correcta de bombos.
- Solo diagnóstico; no modifica la lógica de autorización del warp ni el audio.
- No se ha compilado APK ni realizado escucha A/B.

Reproducir desde la carpeta del proyecto:

`python3 audit-rc101-attack-competition.py AUDIO.flac rc98-phase-joins.csv rc91-full-timeline-audit-events.csv rc101-attack-competition.csv`
