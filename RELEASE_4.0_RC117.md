# RC117 — verificación previa a APK
- Se corrige la insignia de versión que seguía mostrando RC112.
- El contador de bombos ya no afirma 64 ataques: muestra el valor calculado, o 0 sin análisis.
- Los modos se renombran según su función real: calidad/velocidad de render, no intensidad de corrección.
- El workflow ejecuta comprobaciones estáticas de contrato de UI antes de Gradle.
- No se ha verificado la compilación Android ni la instalación en teléfono.
