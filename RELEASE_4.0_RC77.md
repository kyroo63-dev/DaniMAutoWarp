# DaniM Auto Warp 4.0 RC77

## Diagnóstico espectral por evento

El script `smoke-real-detector-audit.sh` ahora exporta, además del CSV de resumen, un CSV `*-events.csv` con la hora, tipo, confianza del detector y evidencia espectral (`bass_share`, `mid_share`, `spectral_score`) de cada candidato de transitorio. Se utiliza la implementación de producción `BeatAnalyzer` y `SpectralKickEvidence` sobre cinco secciones de 8 segundos de la grabación real. No se modifica la política de autorización del warp ni el clasificador de producción.

El CSV permite estudiar hipótesis sobre falsas detecciones; no constituye etiquetas musicales verificadas ni una medida de precisión/recall. Los eventos KICK reconstruidos desde `kickTimes` reciben `detector_confidence=1.0` como marcador de posición: la API pública no expone la confianza original de cada bombo. No interpretar ese campo como confianza calculada.

## Estado

La compilación Android/APK y la escucha auditiva A/B siguen pendientes. El audio del usuario no está incluido en el proyecto.
