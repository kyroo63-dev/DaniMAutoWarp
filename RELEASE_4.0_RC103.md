# DaniM Auto Warp 4.0 RC103 — Revisión humana de ataques

- `review-rc103-attacks.py prepare` crea una cola estratificada y determinista de ataques a revisar, sin inventar etiquetas verdaderas.
- Puede generar fragmentos WAV mono de 0,85 s alrededor de cada ataque a partir del FLAC original para escuchar y etiquetar.
- Columnas editables: `human_label` (`kick`, `snare`, `other`, `uncertain`), `reviewer`, `review_notes`.
- `evaluate` calcula matriz de confusión y precisión/sensibilidad **solo** con etiquetas humanas confirmadas; las métricas son N/A cuando no hay suficientes anotaciones.
- El muestreo equilibra candidatos positivos/negativos por unión, por lo que las métricas de la muestra no representan automáticamente la precisión de toda la pista.
- Los fragmentos se generan como materiales de revisión, no son pruebas auditivas A/B del warp.
- No se ha compilado APK; el motor de warp no cambia y permanece bloqueada la autorización global.

## Comandos

```bash
python3 review-rc103-attacks.py prepare rc102-fusion-events.csv rc103-review-queue.csv --audio AUDIO.flac --clips rc103-clips
python3 review-rc103-attacks.py evaluate rc103-review-queue.csv
python3 test-rc103-review.py
```
