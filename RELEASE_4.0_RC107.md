# DaniM Auto Warp 4.0 RC107

## Correcciones de interfaz Android
- La pantalla nativa de revisión ahora permite desplazamiento vertical, evitando que controles de clasificación, importación y exportación queden fuera de la pantalla en móviles pequeños.
- El reproductor MediaPlayer se libera al cambiar de ataque mediante DisposableEffect(index), además de onStop, para evitar reproducción solapada al navegar.
- Añadida indicación visual de desplazamiento.
- Prueba de contrato de código fuente para nueve condiciones de interfaz y ciclo de vida.

## Limitaciones
- Las pruebas de contrato no equivalen a compilación Android ni a pruebas instrumentadas.
- No hay APK compilado ni prueba en dispositivo; no se ha validado la escucha A/B.
- Sin etiquetas humanas verificadas no puede certificarse precisión de clasificación ni habilitar warp automático continuo.
