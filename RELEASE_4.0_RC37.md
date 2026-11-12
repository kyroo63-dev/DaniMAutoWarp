# DaniM Auto Warp 4.0 RC37

Se incorpora `TempoContinuityGate`, un control de continuidad basado en la duración de compases de cuatro pulsos. Un salto de más del 10 % entre compases consecutivos reduce la confianza del análisis a cero, por lo que el Warp automático se limita a marcadores identidad mediante el control de seguridad existente. El algoritmo permite cambios graduales de tempo, y rechaza saltos de seguimiento como 135 a 180 BPM.

## Pruebas locales ejecutadas
- `smoke-tempo-continuity.sh`: pasa para 135 BPM constante, deriva lenta, salto 135->180 y tiempos inválidos.
- `smoke-warp-pipeline.sh`: pasa para procesamiento WSOLA estéreo, exportación PCM16 y reimportación.

## Límites pendientes
- No se ha compilado un APK ni ejecutado la interfaz Android: faltan Android SDK/emulador en el entorno.
- No se ha repetido el análisis completo ni renderizado el FLAC real en esta versión; estas pruebas solo verifican el control de continuidad de forma aislada.
- Un salto de tempo real intencionado puede ser bloqueado conservadoramente; el modo manual permanece disponible.
