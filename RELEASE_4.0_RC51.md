# DaniM Auto Warp 4.0 RC51

## Novedades
- Vista previa no destructiva de la alineación manual del bombo seleccionado.
- Indica posición de origen, destino de la cuadrícula, desplazamiento en milisegundos y porcentajes de estiramiento de los tramos adyacentes.
- Muestra el motivo del rechazo antes de confirmar el cambio.
- Motor independiente `ManualGridPreview.inspect`, basado en la misma validación de `ManualBeatGrid.align` que usa la acción de confirmación.

## Validación
- `smoke-manual-grid-preview.sh`: PASS.
- `smoke-manual-beatgrid.sh`: PASS.
- Sin compilación de APK ni verificación visual en dispositivo Android en este entorno.
- No se ha repetido el análisis del FLAC real en esta versión.

## Limitaciones
- La vista previa estima estiramientos de segmentos a partir de marcadores; no sustituye la escucha A/B ni una medición de artefactos de audio.
- La corrección automática continúa protegida ante análisis de baja confianza.
