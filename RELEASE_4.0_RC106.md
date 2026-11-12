# DaniM Auto Warp 4.0 RC106

## Cambios reales
- Nueva clase `ReviewCsvImport.kt` con lectura CSV que admite comillas escapadas, CRLF y BOM UTF-8.
- Valida columnas obligatorias, identificadores, asociación con el WAV, duplicados y categorías permitidas.
- Importación mediante selector de documentos Android y persistencia de etiquetas válidas en SharedPreferences.
- Conserva etiquetas existentes cuando no figuran en el archivo importado.
- 11 pruebas Kotlin ejecutadas satisfactoriamente (`test-rc106-import.kt`).

## Limitaciones
- No se ha compilado el APK (SDK Android/Gradle no disponibles en este entorno).
- No se han ejecutado pruebas en dispositivo ni escuchas A/B.
- Las etiquetas humanas todavía no existen; los valores previos del detector no son verdad de referencia.
- No se autoriza warp automático continuo por estos cambios.

## Próximas prioridades
- Importación/exportación con pruebas instrumentadas Android y gestión de conflictos entre etiquetas.
- Verificación de reproducción y UI en dispositivo.
- Evaluación del detector cuando existan etiquetas humanas verificadas.
