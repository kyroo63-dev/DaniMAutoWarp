# DaniM Auto Warp 4.0 RC65

- Nuevo `TransientAnchorMode` (`LEGACY`, `OFFSET_AWARE`, `ADAPTIVE`) en el renderizador WSOLA streaming.
- El modo por defecto `ADAPTIVE` selecciona el anclaje con desplazamiento para ratios de lectura >1.025 y el anclaje anterior en los demás casos. Se trata de una heurística basada en la regresión sintética, no de una medida perceptual ni de una optimización por bombo.
- `smoke-wsola-fidelity-report.sh` comprueba los tres modos con cinco golpes sintéticos y tres duraciones; `smoke-streaming-wsola-pitch.sh` comprueba tono y memoria.
- Resultados ADAPTIVE: 2.85 s 71.8%, 3.00 s 79.6%, 3.15 s 85.2% de energía media relativa de transitorios; error máximo 17.25, 10.63 y 6.62 ms respectivamente.
- Limitaciones: no se ha compilado un APK, ni validado mediante escucha o con la grabación completa de Expanden; la energía de ataque puede disminuir y el error temporal no es nulo.
