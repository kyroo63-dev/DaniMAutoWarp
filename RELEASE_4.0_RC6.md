# DaniM Auto Warp 4.0 RC6 — estabilización

- La reproducción A/B valida los límites con marcadores ordenados por tiempo de destino, incluso si el proyecto proporciona marcadores desordenados.
- Comprueba el resultado de `AudioTrack.getMinBufferSize` antes de reservar el buffer; los códigos de error negativos ya no se convierten en un tamaño aparentemente válido.
- Versión actualizada a 4.0.0-rc6 (45).

## Validación

El núcleo DSP puede compilarse con `kotlinc`, pero eso no compila la app Android. Falta Gradle Wrapper y un Android SDK configurado. Se requieren pruebas en teléfono y con grabaciones reales antes de etiquetar la app como estable.

## Limitaciones conocidas

MP3 320 kbps no implementado. El sistema A/B sigue utilizando bloques de audio y no garantiza conmutación instantánea a nivel de hardware.
