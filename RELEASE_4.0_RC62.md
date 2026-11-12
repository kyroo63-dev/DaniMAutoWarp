# DaniM Auto Warp 4.0 RC62

## Cambios
- Nuevo `audio/TransientFidelity.kt`: medición diagnóstica de error temporal relativo del pico y ratio de energía RMS en ventanas de transitorios.
- `smoke-transient-fidelity.sh`: casos controlados de desplazamiento de 5 ms, reducción de amplitud al 50 %, alineación prevista y límites inválidos.
- Conserva el motor WSOLA y sus pruebas de transitorios de RC61.

## Pruebas realizadas
- `smoke-transient-fidelity.sh`: PASS.
- `smoke-wsola-transients.sh`: PASS para 2,85, 3,00 y 3,15 segundos.

## Limitaciones
- La métrica utiliza el máximo absoluto dentro de una ventana; no es un detector perceptual de artefactos ni una prueba de calidad sonora.
- El módulo es una herramienta de diagnóstico independiente; aún no está integrado en la interfaz de Android ni en la exportación automática.
- No se ha compilado APK ni se ha validado auditivamente la grabación completa de vinilo.
