# DaniM Auto Warp 4.0 RC70

## Cambio de producción
Selector `TransientAnchorMode.ADAPTIVE` ajustado: el modo OFFSET_AWARE se activa cuando `ratioAt > 1.005` (ratio = segundos de origen / segundos de destino); en el resto de casos usa LEGACY. El criterio es una heurística respaldada únicamente por el ensayo de RC69, no un optimizador perceptual.

## Comparación real, 44,1 kHz estéreo
Se ejecutó `./smoke-stereo-transient-modes.sh` con «Expanden», un extracto de tres segundos y cinco máximos locales de energía sin clasificar. En factor 0.99, ADAPTIVE reduce error medio de 4.09 a 2.92 ms y eleva ratio RMS de 0.826 a 0.867. En factor 1.01 conserva error 4.35 ms y ratio 0.795. Los seis renderizados pasan verificaciones de WAV, buffers, duración y valores finitos. Las medidas no sustituyen una escucha ni representan la canción completa.

## Limitaciones
No se ha compilado un APK Android, ni se ha evaluado perceptualmente la calidad, ni se han etiquetado bombos de forma fiable. Se requiere una batería de varios fragmentos, patrones y calidades antes de considerar óptima la heurística.
