# RC124 - Sincronización más segura

- La sincronización no sigue enlaces simbólicos locales ni remotos.
- Publicar modificaciones en `.github/workflows/` requiere `--allow-workflow` además de `--push`.
- La ejecución sin `--push` solo presenta una vista previa.
- CI actualizado a RC124.

Para sincronizar: `python sincronizar_github.py` y, después de revisar, `python sincronizar_github.py --push --allow-workflow`.

**No se ha publicado en GitHub ni compilado un APK.**
