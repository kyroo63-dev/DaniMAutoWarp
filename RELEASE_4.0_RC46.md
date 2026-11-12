# DaniM Auto Warp 4.0 RC46

- Visualización opcional sobre la forma de onda de ventanas de fase fiables (violeta) y dudosas (naranja).
- Informe de ventanas fiables, segmentos continuos, discontinuidades y saltos máximos.
- Nuevo `PhaseTimeline.project` para recortar correctamente las ventanas al zoom y desplazamiento actuales.
- Pruebas: `smoke-phase-timeline.sh`, `smoke-kick-phase-segments.sh`, `smoke-kick-phase-drift.sh`, `smoke-low-confidence.sh`, `smoke-beat-tracking.sh` superadas.
- Corregidas dependencias de compilación de dos scripts smoke antiguos.

## Limitaciones

No se ha compilado ni ejecutado un APK Android (SDK no disponible). La visualización Compose no ha sido probada en un dispositivo. No se ha validado la corrección automática sobre la grabación completa. El diagnóstico nunca autoriza por sí solo el Warp.
