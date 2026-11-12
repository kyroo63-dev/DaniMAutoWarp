# DaniM Auto Warp 4.0 RC96

Auditoría diagnóstica conjunta del agrupamiento y el filtro espectral/rítmico sobre los eventos reales registrados en RC91.

- 1762 eventos KICK; 490 conservados con agrupamiento anterior, 998 con agrupamiento anclado.
- Superan el filtro: 310 (anterior), 499 (anclado); 189 candidatos adicionales.
- Los eventos KICK no tienen confianza individual calibrada y no existe ground truth.
- El filtro no se conecta al motor de warp: no habilita correcciones globales.
- Script reproducible: `audit-rc96-cluster-evidence.py` (entrada: CSV de eventos y resumen RC91).
- La reproducción usa eventos archivados, no vuelve a decodificar el FLAC ni verifica audio A/B.
- Sin Android SDK/Gradle: APK y UI sin compilar ni ejecutar.
