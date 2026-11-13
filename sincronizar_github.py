#!/usr/bin/env python3
"""Previsualiza o sincroniza DaniM Auto Warp con el repositorio indicado.
Requiere Git y credenciales GitHub configuradas por el usuario. No borra
archivos remotos que no estén presentes en este proyecto.
"""
import argparse
import shutil
import subprocess
import tempfile
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = "https://github.com/kyroo63-dev/DaniMAutoWarp.git"
SKIP_DIRS = {".git", ".gradle", "build", ".idea", "__pycache__", ".venv"}
SKIP_FILES = {"local.properties", ".DS_Store"}

def git(*args, cwd=None):
    return subprocess.run(["git", *args], cwd=cwd, check=True, text=True)

def main():
    p = argparse.ArgumentParser(description="Sincronización segura de DaniM Auto Warp")
    p.add_argument("--push", action="store_true", help="Confirma el commit y envía a GitHub")
    p.add_argument("--allow-workflow", action="store_true", help="Confirma que tu autenticación permite editar GitHub Actions")
    args = p.parse_args()
    if not (ROOT / "app/build.gradle.kts").exists():
        p.error("No se encuentra app/build.gradle.kts junto al script")
    with tempfile.TemporaryDirectory(prefix="danim-github-") as tmp:
        repo = Path(tmp) / "DaniMAutoWarp"
        git("clone", "--branch", "main", REPO, str(repo))
        copied = []
        skipped_links = []
        for f in ROOT.rglob("*"):
            if any(part in SKIP_DIRS for part in f.relative_to(ROOT).parts):
                continue
            # Never follow symbolic links: they may point outside this project.
            if f.is_symlink() or any(parent.is_symlink() for parent in f.parents if parent != ROOT):
                skipped_links.append(str(f.relative_to(ROOT)))
                continue
            if not f.is_file():
                continue
            if f.name in SKIP_FILES or f.suffix in {".apk", ".aab", ".keystore", ".jks"}:
                continue
            rel = f.relative_to(ROOT)
            target = repo / rel
            # A symlink already present in the remote checkout must not be followed.
            if target.is_symlink() or any(parent.is_symlink() for parent in target.parents if parent != repo):
                raise RuntimeError(f"Ruta remota insegura (enlace simbólico): {rel}")
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(f, target)
            copied.append(str(rel))
        if skipped_links:
            print("Enlaces simbólicos omitidos:", len(skipped_links))
        if not copied:
            raise RuntimeError("No hay archivos seguros para sincronizar")
        git("add", "--", *copied, cwd=repo)
        git("diff", "--cached", "--stat", cwd=repo)
        print("\nArchivos preparados:", len(copied))
        if not args.push:
            print("SIMULACIÓN: no se ha publicado nada. Ejecuta con --push para enviar.")
            return
        changed = subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=repo)
        if changed.returncode == 0:
            print("No hay cambios que publicar")
            return
        if changed.returncode != 1:
            raise RuntimeError("Error al comprobar los cambios de Git")
        staged = subprocess.check_output(["git", "diff", "--cached", "--name-only"], cwd=repo, text=True).splitlines()
        if any(name.startswith(".github/workflows/") for name in staged) and not args.allow_workflow:
            raise SystemExit("El cambio incluye workflows de GitHub Actions. Repite con --push --allow-workflow si tu cuenta tiene permisos para modificarlos.")
        git("commit", "-m", "DaniM Auto Warp RC124: sincronizar fuente y CI", cwd=repo)
        git("push", "origin", "main", cwd=repo)
        print("Cambios publicados en GitHub. Consulta la pestaña Actions.")

if __name__ == "__main__":
    main()
