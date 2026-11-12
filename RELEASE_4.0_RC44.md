# DaniM Auto Warp 4.0 RC44

## Nuevo
- `KickPhaseSegments`: agrupa ventanas adyacentes con fase de bombo coherente; interrumpe los segmentos al encontrar una ventana no fiable o un salto de fase superior al umbral (35 ms por defecto).
- No interpola a través de huecos ni convierte evidencia de fase en autorización de Warp.
- `smoke-kick-phase-segments.sh`: prueba continuidad, ventana rechazada, salto de 80 ms y entrada vacía.

## Validación
- `smoke-kick-phase-segments.sh`: PASS.
- No se ha repetido en RC44 el análisis integral de «Expanden» ni se ha compilado APK Android.

## Pendiente
- Integrar la segmentación en la pantalla de diagnóstico y el análisis completo de audio real.
- Verificar la fase con la grabación original y habilitar procesamiento A/B solo con evidencia suficiente.
