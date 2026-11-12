# DaniM Auto Warp 4.0 RC109

- Se restaura el último ataque seleccionado mediante su identificador estable, no su posición.
- Se guarda el identificador al navegar por los 64 ataques.
- La importación CSV persiste el lote completo con `SharedPreferences.Editor.commit()` antes de actualizar la UI; ante error de escritura no anuncia éxito.
- Navegación y fusión de etiquetas extraídas a `ReviewProgress.kt`, comprobables sin SDK Android.
- Se mantienen el lector WAV de RC108, las muestras y la protección frente a etiquetas desconocidas.
- Pruebas JVM específicas: 10 aserciones. Sin compilación APK ni prueba en dispositivo.
- No se autoriza warp continuo sin validación humana.
