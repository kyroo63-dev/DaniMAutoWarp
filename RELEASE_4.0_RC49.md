# DaniM Auto Warp 4.0 RC49

- Añade un marcador **neutro** sobre un bombo seleccionado y confirmado expresamente por el usuario.
- Permite pasar de los dos marcadores de seguridad a tres marcadores editables, conservando el mapa temporal hasta que se ajuste manualmente.
- Rechaza marcadores duplicados, demasiado próximos, fuera del rango, datos no finitos y tramos con ratio de estiramiento fuera de 0,85–1,15.
- Integra navegación entre bombos candidatos y confirmación en la interfaz de diagnóstico.
- La operación usa `commitMarkers`, por lo que admite deshacer y persistencia de sesión.
- El botón confirma la selección del usuario, **no verifica acústicamente** que el evento sea realmente un bombo.
- El ajuste posterior requiere revisión auditiva; no se garantiza corrección automática ni calidad de audio.
- No hay compilación APK ni prueba en emulador/dispositivo en este entorno.
