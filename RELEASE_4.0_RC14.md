# DaniM Auto Warp 4.0 RC14

## Corrección principal
La preescucha A/B renderiza un archivo corregido de 8 segundos cuyo primer fotograma corresponde a `targetStartSeconds`. Antes se intentaba leer el audio corregido desde `targetStartSeconds * sampleRate`, como si el archivo contuviera la canción entera. Esto producía lecturas fuera de rango y fallos cuando se escuchaban posiciones avanzadas. Ahora el audio corregido se lee desde el fotograma local cero, mientras el original sigue usando la correspondencia temporal absoluta del beatgrid.

## Robustez
Un fallo de inicialización de una sesión A/B antigua ya no desactiva la bandera de reproducción de una sesión más reciente.

## Verificación pendiente
No se dispone de Android SDK ni de Gradle Wrapper en este entorno. Es necesario compilar la aplicación completa y probarla en Android antes de considerarla estable. Exportación MP3 320 kbps no implementada.
