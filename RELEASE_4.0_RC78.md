# DaniM Auto Warp 4.0 RC78

Se incorpora `KickRhythmEvidence`, un diagnóstico de fase rítmica de los candidatos de bombo, independiente de la evidencia espectral. El CSV de auditoría incluye `rhythm_score` para cada candidato. La nueva señal es **solo diagnóstica**: no modifica la clasificación de producción ni autoriza el warp automático. La periodicidad y la fase se estiman a partir de los mismos candidatos, por lo que no son validación independiente ni precisión/recall musical.

Pruebas: ejecutar `smoke-real-detector-audit.sh <grabación.flac> <salida.csv>`. La compilación de APK y la escucha A/B siguen pendientes.
