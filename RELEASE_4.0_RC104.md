# DaniM Auto Warp 4.0 RC104 — Revisión visual y auditiva

Se añade `review-rc104.html`, interfaz local sin servidor para importar el CSV de RC103, seleccionar los 64 WAV, reproducir ataques, mostrar forma de onda, navegar, etiquetar con atajos y exportar un CSV compatible con el evaluador RC103.

La interfaz es una herramienta auxiliar en navegador, **no una pantalla Android integrada**. No se recopilan datos ni se asignan etiquetas automáticamente. No se ha validado la identidad real de bombos ni el warp completo. No se ha compilado APK.

## Uso

1. Descomprimir `rc104-review-kit.zip`.
2. Abrir `review-rc104.html` en un navegador moderno.
3. Cargar `rc103-review-queue.csv` y seleccionar todos los WAV de `clips/`.
4. Indicar un identificador de revisor, escuchar y clasificar.
5. Exportar `rc104-review-labeled.csv`; evaluar con `python3 review-rc103-attacks.py evaluate rc104-review-labeled.csv`.

Las muestras fueron seleccionadas de forma estratificada; no representan por sí solas toda la pista.
