# DaniM Auto Warp 4.0 RC98

- Nueva auditoría diagnóstica de continuidad de fase entre clips: `audit-rc98-phase-joins.py`.
- Reutiliza el filtro de candidatos y el análisis de cadenas RC97.
- Requiere estabilidad de ambos clips, >=4 candidatos cerca de cada borde y salto de BPM <=2.5%.
- Calcula una medida de coincidencia de fase modular entre eventos de los bordes.
- **No valida identidad de bombo ni autoriza warp global.** La coincidencia puede provenir de cajas y otras percusiones regulares.
- 10 comprobaciones sintéticas en `test-rc98-phase-joins.py`.
- Sobre los eventos históricos RC91: 21 uniones; 4 coincidencias diagnósticas, 2 desajustes de fase, 3 insuficiencia de eventos de borde, 12 insuficiencia de estabilidad.
- La auditoría se basa en eventos RC91 y no es un nuevo procesamiento del FLAC original.
- Sin compilación Android ni validación auditiva A/B en dispositivo.
