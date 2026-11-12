# DaniM Auto Warp 4.0 RC60

## Alcance
- Nueva prueba JVM reproducible `smoke-streaming-wsola-pitch.sh` para el motor WSOLA streaming ya integrado.
- Genera WAV PCM16 mono a 8 kHz con tono sintético de 440 Hz y estiramientos 0.9×, 1× y 1.1× en duración.
- Comprueba cabecera/tamaño, duración, estimación de frecuencia y máximo de muestras fuente leídas por ventana.
- Resultados medidos: 441, 439 y 440 Hz respectivamente; memoria fuente máxima 480, 480 y 476 fotogramas.

## Limitaciones
- La estimación por cruces por cero es aproximada y no evalúa transitorios ni artefactos musicales.
- La prueba no demuestra calidad comparable a élastique Pro ni valida la grabación real completa.
- APK Android no compilado en este entorno; requiere Android SDK y Gradle.
