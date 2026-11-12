# DaniM Auto Warp 4.0 RC36

En caso de desacuerdo superior al 12 % entre BPM seguido y autocorrelación de graves, el BPM informado usa la referencia independiente y se descarta la cuadrícula seguida: confianza 0, sin beatTimes, dos marcadores identidad y sin Warp automático. No se presenta una curva de tempo inventada.

Prueba con Javi Golo & Andres Gomes - Expanden.flac (mono 8 kHz, extractos 75 s):
- 01:00–02:15: 136.36 BPM, confianza 0.95, 68 marcadores.
- 05:20–06:35: 136.36 BPM, confianza 0.00, 2 marcadores identidad.

Pasaron smoke-core, smoke-riff, smoke-riff-padding y smoke-warp-pipeline. smoke-beat-tracking no terminó dentro del límite de tiempo de esta ejecución. Sin compilación Android ni prueba auditiva completa.
