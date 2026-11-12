# RC22 — Validación estricta RIFF/WAV

- Se comprueba el tamaño RIFF declarado y se limita la lectura de chunks a ese tamaño.
- Se rechazan bloques `data` truncados, vacíos o con longitud no representable.
- Se evita aceptar datos extra fuera del contenedor RIFF como si fueran chunks válidos.
- Se añade una prueba de regresión ejecutable con `kotlinc`.
- APK Android y dispositivo físico: no verificados.
