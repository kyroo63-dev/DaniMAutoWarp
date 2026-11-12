# DaniM Auto Warp 4.0.0-rc1 — validación pendiente

## Correcciones de estabilización
- La reproducción A/B utiliza PreparedAbTimeline.sourceAt(t) directamente, evitando una llamada incompatible con la API del módulo.
- Se limita la preescucha a la duración restante del beatgrid.
- Se evita que el cierre de una reproducción antigua desactive el estado de otra nueva.
- Fundido cruzado de ganancia unitaria para señales idénticas, evitando una subida artificial de nivel.

## Estado real
El ZIP contiene **código fuente**, no un APK. No se ha ejecutado la compilación Android completa ni probado el comportamiento en un teléfono. MP3 320 kbps no está implementado: Android no garantiza un codificador MP3 en todos los dispositivos. No debe anunciarse como versión final estable.

## Para validar en Android Studio
1. Abrir la carpeta DaniMAutoWarp, instalar JDK 17 y Android SDK 35.
2. Ejecutar `:app:testDebugUnitTest` y `:app:assembleDebug`.
3. Instalar el APK debug en Android 8+ y probar importación MP3/WAV/AIFF, análisis, corrección, A/B y exportación WAV/AIFF/FLAC.
4. Probar 10 grabaciones reales de vinilo, inspeccionar BPM y marcadores, comparar original/corregido con auriculares, revisar transitorios, clipping, duración y ausencia de fallos.
5. Resolver cualquier fallo antes de publicar la 4.0 estable.
