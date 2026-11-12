# DaniM Auto Warp 4.0 RC58

- Nueva prueba reproducible `smoke-real-vinyl-render-audit.sh`: decodifica 8 segundos de la grabación FLAC de usuario con ffmpeg y genera WAV PCM16 de identidad y warp de prueba (25 ms en el marcador central).
- Usa `Pcm16WavFileSource`, `StreamingWarpProcessor` y `StreamingRenderAudit` para verificar número de frames, duración, cabecera, memoria de ventana y niveles RMS.
- Resultado observado: 8.000 s; RMS original 0.2489, identidad 0.2489, warp 0.2483; saturaciones identidad 119 y warp 82; saltos heurísticos original 4354, identidad 4354, warp 3784.
- Los avisos también existen en el audio original; no implican necesariamente artefactos nuevos. El warp es una prueba técnica, no una corrección automática del beatgrid.
- La prueba no redistribuye ni incluye el FLAC del usuario.
- No se ha compilado APK: SDK Android 35 y Gradle no disponibles. El motor de interpolación cúbica de streaming no conserva el tono.
