# DaniM Auto Warp 4.0 RC67

## Cambios implementados

- Ruta directa PCM16 para mapas de warp estrictamente idénticos (todos los marcadores tienen `sourceSeconds == targetSeconds`, tolerancia submuestra).
- Evita el procesamiento overlap-add WSOLA cuando no hay transformación temporal.
- Compatible con mono/estéreo, previsualizaciones, marcadores de transitorios y tres modos de anclaje.
- Lectura por bloques de hasta 4096 fotogramas; la memoria sigue acotada.
- Script reproducible `smoke-identity-stereo.sh` con un fragmento de 3 segundos del FLAC de usuario a 44,1 kHz, estéreo.

## Pruebas realizadas

- Igualdad exacta de muestras PCM16 para los tres modos de anclaje: PASS.
- Exportación de previsualización de un segundo: igualdad PCM16 PASS.
- Corrección no identidad del 1 %: se mantiene la ruta WSOLA, WAV y duración correctos PASS.
- Test de conservación aproximada de tono RC60: PASS.

**Alcance:** la identidad PCM16 se compara después de decodificar el FLAC a PCM16, no contra los bytes comprimidos originales. No se ha compilado un APK ni se ha realizado una validación auditiva completa.
