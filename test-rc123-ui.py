from pathlib import Path
import ast
root = Path(__file__).resolve().parent
main = (root / "app/src/main/java/com/danim/autowarp/MainActivity.kt").read_text()
build = (root / "app/build.gradle.kts").read_text()
workflow = (root / ".github/workflows/android-apk.yml").read_text()
upload = (root / "sincronizar_github.py").read_text()
checks = {
    "versionName RC123": 'versionName = "4.0.0-rc123"' in build,
    "versionCode 163": "versionCode = 163" in build,
    "interfaz Compose conservada": "@Composable" in main,
    "onda usa amplitud finita": "amplitude.isFinite()" in main,
    "onda ocupa ancho": "(column + 0.5f) * size.width / columns" in main,
    "compilacion APK": ":app:assembleDebug" in workflow,
    "publicacion APK": "DaniMAutoWarp-RC123-debug-apk" in workflow,
    "test unitarios": ":app:testDebugUnitTest" in workflow,
    "scripts disparan CI": '"test-rc*.py"' in workflow,
    "script sincronizacion sintacticamente valido": bool(ast.parse(upload)),
    "sincronizacion requiere push explicito": 'if not args.push:' in upload,
    "sincronizacion preserva archivos remotos ajenos": 'git("add", "--", *copied, cwd=repo)' in upload,
}
for name, ok in checks.items(): print(("PASS" if ok else "FAIL") + ": " + name)
if not all(checks.values()): raise SystemExit(1)
