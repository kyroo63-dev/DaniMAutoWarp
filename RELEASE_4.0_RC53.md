# DaniM Auto Warp 4.0 RC53

- Añade indicador visual ampliado cuando el origen y el destino de la alineación manual se encuentran a menos de 12 dp en pantalla.
- Mantiene las líneas originales y la vista previa sin cambios en el audio ni en los marcadores.
- Nueva prueba `smoke-manual-grid-detail.sh` para umbral de magnificación, zoom, recorte y geometría inválida.
- Verificados los smoke tests de overlay, vista previa, cuadrícula manual e inserción de marcadores.

## Limitaciones

No se ha compilado un APK ni probado la interfaz en Android: este entorno carece de Android SDK/Gradle Wrapper. El indicador es una ayuda visual, no una ampliación real de la forma de onda ni una prueba de calidad auditiva. Sigue pendiente validar la corrección sobre la grabación completa.
