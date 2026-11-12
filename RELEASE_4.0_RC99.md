# DaniM Auto Warp 4.0 RC99

- Auditoría independiente `audit-rc99-spectral-joins.py` sobre las 21 uniones RC98.
- Cuatro uniones con coincidencia de fase preliminar; cero con corroboración espectral según los umbrales diagnósticos actuales.
- Requiere al menos cuatro eventos graves por lado, mediana de energía grave >= 0.75 y ausencia de mayoría de ataques graves competidores.
- **Las etiquetas KICK/SNARE del detector NO son ground truth**; estas comprobaciones no validan la identidad real de los bombos.
- La auditoría reutiliza eventos históricos RC91, no vuelve a leer el FLAC. La confianza 1.0 en CSV RC91 no es real y NO se utiliza en RC99.
- El criterio no cambia la lógica de warp de producción ni habilita corrección global.
- Ocho aserciones sintéticas de RC99 pasan. Sin compilación APK ni escucha A/B.
