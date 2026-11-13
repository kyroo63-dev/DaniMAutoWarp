# Subir RC123 y compilar el APK

Repositorio: https://github.com/kyroo63-dev/DaniMAutoWarp

**Estado verificado:** el conector GitHub permite lectura pero devuelve HTTP 403
`Resource not accessible by integration` al escribir. Por ello no se ha
subido RC123 ni lanzado una compilación.

## Método recomendado: ordenador con Git instalado

1. Descomprime este ZIP conservando todas las carpetas, incluida `.github`.
2. Abre una terminal dentro de la carpeta descomprimida.
3. Configura el acceso a GitHub en tu ordenador (por ejemplo, Git Credential Manager).
4. Comprueba los cambios propuestos: `python sincronizar_github.py`
5. Si estás de acuerdo, publícalos: `python sincronizar_github.py --push`

El script clona la rama `main`, copia los archivos del proyecto y crea un
commit. No elimina archivos existentes en GitHub que falten en el ZIP.
No requiere pegar contraseñas ni tokens dentro del código.

## Desde Android

Puedes usar un cliente Git para Android que admita carpetas ocultas o un
entorno de terminal con Git y Python. Extrae el ZIP, conserva `.github/`,
y ejecuta los mismos comandos. La interfaz web de GitHub no es adecuada
para subir cientos de archivos manteniendo su estructura.

## Obtener el APK

Entra en https://github.com/kyroo63-dev/DaniMAutoWarp/actions,
selecciona `Compilar APK DaniM Auto Warp`, abre la ejecución reciente y,
si finaliza correctamente, descarga el artefacto
`DaniMAutoWarp-RC123-debug-apk` (contiene `app-debug.apk`).
Si falla, revisa el primer error de `Compilar APK debug` y compártelo.

**No hay APK compilado ni pruebas Android ejecutadas en esta entrega.**
