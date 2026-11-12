# Compilar DaniM Auto Warp desde un movil Android

## Preparacion (una sola vez)
1. Crea una cuenta gratuita en https://github.com/ si no tienes una.
2. En el navegador del movil entra en https://github.com/new y crea un repositorio **privado** vacio, sin README inicial.
3. Extrae el ZIP de este proyecto en el telefono. Debes subir **el contenido** de la carpeta extraida a la raiz del repositorio, conservando subcarpetas como `app/` y `.github/workflows/`.
4. GitHub web no siempre permite subir una carpeta entera comodamente desde Android y puede ocultar `.github` por ser una carpeta con punto. Puedes usar un cliente Git para Android compatible con subir carpetas y archivos ocultos, o un servicio de importacion de repositorios. Comprueba en GitHub que existen `app/build.gradle.kts`, `settings.gradle.kts` y `.github/workflows/android-apk.yml`.
5. La rama principal debe llamarse `main`.

## Lanzar la compilacion
1. Abre tu repositorio en GitHub y entra en la pestana **Actions**.
2. Selecciona **Compilar APK DaniM Auto Warp**.
3. Pulsa **Run workflow** y confirma la rama `main`.
4. Espera a que termine el trabajo **build-debug-apk** (verde significa que ha terminado correctamente).
5. Abre la ejecucion terminada, baja hasta **Artifacts** y descarga `DaniMAutoWarp-RC110-debug-apk`.
6. Extrae ese ZIP y abre el fichero `app-debug.apk` en tu Android para instalarlo.

## Si falla
- Entra en la ejecucion roja > `build-debug-apk` > `Compilar APK debug`.
- Copia el primer error real del registro, especialmente `e: ...`, `FAILURE:` o `What went wrong`.
- Enviame el error para corregir el codigo o la configuracion.

## Limitaciones
- Este paquete incluye un flujo de compilacion, **no un APK ya compilado**.
- El proyecto aun no ha superado una compilacion Android verificada; pueden aparecer errores del codigo fuente.
- El APK debug esta firmado automaticamente con una clave de desarrollo y no es adecuado para distribucion publica.
- Los artifacts de GitHub caducan (configurados a 14 dias).
