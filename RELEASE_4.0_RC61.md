# DaniM Auto Warp 4.0 RC61

## Cambios
- Nueva prueba reproducible `smoke-wsola-transients.sh` para ataques sintéticos de bombo (75 Hz y 1600 Hz, envolvente exponencial).
- Tres transformaciones temporales (2,85 / 3,00 / 3,15 segundos) con cinco transitorios anclados a la curva de warp.
- Comprueba duración exacta, cabecera WAV, memoria de lectura acotada, ausencia de valores no finitos y presencia de energía en los ataques previstos.

## Resultados
- 2,85 s: 5 anclajes, pico 0,519, ataques presentes.
- 3,00 s: 5 anclajes, pico 0,682, ataques presentes.
- 3,15 s: 6 anclajes, pico 0,686, ataques presentes.
- Las tres pruebas JVM superadas. El número de anclajes indica ventanas WSOLA bloqueadas, no cantidad de bombos.

## Limitaciones
- Los ataques sintéticos no demuestran fidelidad de transitorios ni calidad auditiva en vinilo real.
- No se ha compilado APK: falta Android SDK/Gradle configurado en este entorno.
- No se afirma corrección automática fiable de beatgrid en la grabación «Expanden».
