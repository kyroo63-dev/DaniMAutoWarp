# DaniM Auto Warp 4.0 RC102

- Nuevo script reproducible `audit-rc102-fusion.py` que vuelve a extraer rasgos de ataques del FLAC real para las cuatro uniones con coincidencia de fase preliminar de RC98.
- Cruza energía grave, centroide espectral, contraste energético de ataque y vecinos a 1–2 pulsos según el BPM histórico, con tolerancia de 0,12 pulsos.
- **Resultado:** 778 eventos medidos en 8 ventanas de 8 segundos; 503 cumplen la regla heurística combinada, **284** de ellos habían sido clasificados históricamente como `OTHER`. Esto **no es** una medida de errores reales ni demuestra que esos 284 sean bombos.
- El resultado muestra la ambigüedad de la regla, que no es un clasificador entrenado ni validado con etiquetas humanas. El BPM y los tiempos de evento proceden del análisis histórico; por tanto la prueba de ritmo no es independiente de toda la cadena anterior.
- No se habilita warp continuo, no se modifica el renderizador, ni se ha compilado un APK o validado escucha A/B.
- `test-rc102-fusion.py` comprueba siete invariantes del CSV, no la precisión de detección.

Reproducir:

```bash
python3 audit-rc102-fusion.py AUDIO.flac rc98-phase-joins.csv rc91-full-timeline-audit-events.csv rc102-fusion-events.csv rc91-full-timeline-audit.csv
python3 test-rc102-fusion.py rc102-fusion-events.csv
```

## Siguiente fase

Crear un pequeño conjunto de ataques etiquetados manualmente (bombo, caja, otro, incierto) y comparar métricas sobre datos de validación separados. No ajustar umbrales para maximizar coincidencia con las etiquetas del detector anterior.
