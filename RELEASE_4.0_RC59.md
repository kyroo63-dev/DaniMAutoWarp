# DaniM Auto Warp 4.0 RC59

- Nuevo `RenderComparisonAudit.kt`: comparación diferencial entre WAV fuente y exportación, sin confundir los transitorios existentes con defectos añadidos.
- Compara compatibilidad de formato, diferencia de duración, nuevas saturaciones, nuevos saltos heurísticos y cambio de RMS en dB.
- Integrado en `smoke-real-vinyl-render-audit.sh` para la grabación real «Expanden».
- Nueva prueba JVM `smoke-render-comparison.sh`.
- Limitación: comparar recuentos globales de saltos no localiza artefactos ni demuestra calidad perceptual; sigue pendiente motor time-stretch con preservación de tono y APK Android.
