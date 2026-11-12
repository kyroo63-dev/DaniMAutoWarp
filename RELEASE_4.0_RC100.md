# DaniM Auto Warp 4.0 RC100

- Nuevo script `audit-rc100-fresh-spectrum.py` que decodifica directamente el FLAC original mediante ffmpeg y vuelve a medir energía por bandas en ventanas de 2048 muestras a 22050 Hz.
- Se han reanalizado 4 uniones preliminarmente coincidentes de RC98 (72, 120, 144 y 456 s), con 4 segundos por lado.
- Los tiempos y las etiquetas KICK/otras proceden del detector histórico RC91; **no son etiquetas verificadas**. La medición espectral sí es nueva, calculada directamente del audio original.
- Los porcentajes de energía grave son una fracción de energía espectral entre 30 y 5000 Hz, no probabilidades de ser bombo ni medidas de precisión.
- Ninguna unión autoriza warp. Sin compilación APK ni escucha A/B.
- Reproducir: `python3 audit-rc100-fresh-spectrum.py AUDIO.flac rc98-phase-joins.csv rc91-full-timeline-audit-events.csv rc100-fresh-spectrum.csv`
