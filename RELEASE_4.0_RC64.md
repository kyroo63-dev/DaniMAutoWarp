# DaniM Auto Warp 4.0 RC64

## Cambio
Corrección de la posición del anclaje WSOLA para tener en cuenta el desplazamiento del ataque dentro del hop de salida, en vez de colocar siempre el transitorio en el inicio del grano.

## Resultados medidos con cinco bombos sintéticos
| Duración | RC63 RMS ratio | RC64 RMS ratio | Error máximo RC64 |
|---|---:|---:|---:|
| 2,85 s | 0,612 | 0,718 | 17,25 ms |
| 3,00 s | 0,796 | 0,789 | 10,63 ms |
| 3,15 s | 0,852 | 0,827 | 6,00 ms |

Los resultados son mixtos; NO demuestra una mejora universal. Falta prueba auditiva con audio real y compilación Android.
