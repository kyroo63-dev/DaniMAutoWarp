# DaniM Auto Warp 4.0 RC87

- `KickTempoHoldoutAudit`: estima BPM independientemente en dos mitades cronológicas usando medianas de intervalos consecutivos cercanos al pulso de referencia.
- Marca como inconclusas las secuencias escasas, a medio tiempo o con menos de tres pares válidos por mitad.
- Doce casos de prueba sintéticos y columnas adicionales en la auditoría de audio real.
- Exclusivamente diagnóstico: no autoriza warp ni altera el audio. El filtro depende del BPM de referencia y no prueba identidad de bombo.
- Pendientes: compilación APK Android y validación auditiva A/B.
