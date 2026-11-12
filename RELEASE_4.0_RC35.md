# DaniM Auto Warp 4.0 RC35

Protección de corrección automática con comprobación cruzada del BPM seguido frente a autocorrelación de graves. Si la discrepancia supera 12 % o la confianza es menor de 20 %, el sistema utiliza marcadores identidad (no modifica el tiempo). La confianza máxima es 95 %.

Prueba real: Javi Golo & Andres Gomes - Expanden.flac, extractos de 75 s convertidos a PCM mono 8 kHz para análisis.
- 01:00–02:15: BPM 136.36, confianza 0.950, 68 marcadores.
- 05:20–06:35: BPM 181.82, confianza 0.000, 2 marcadores identidad.

El segundo BPM aún es incorrecto; ahora se impide que el sistema lo use para estirar audio automáticamente. No se ha corregido ni escuchado la pista completa. No hay compilación ni ejecución Android en este entorno.

Prueba reproducible: bash smoke-real-vinyl.sh /ruta/a/archivo.flac (ffmpeg, Java, kotlinc).
