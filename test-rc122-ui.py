from pathlib import Path
root = Path(__file__).resolve().parent
main = (root / "app/src/main/java/com/danim/autowarp/MainActivity.kt").read_text()
build = (root / "app/build.gradle.kts").read_text()
workflow = (root / ".github/workflows/android-apk.yml").read_text()
checks = {
    "RC122 version": 'versionName = "4.0.0-rc122"' in build,
    "RC122 versionCode": "versionCode = 162" in build,
    "waveform samples from real cache": "waveform?.peaks" in main,
    "finite waveform amplitude guard": "amplitude.isFinite()" in main,
    "waveform uses full canvas width": "(column + 0.5f) * size.width / columns" in main,
    "waveform event time coordinates": "(seconds / duration).toFloat() * size.width" in main,
    "column count bounded": "coerceIn(1, 4096)" in main,
    "CI builds APK": ":app:assembleDebug" in workflow,
    "CI runs tests": ":app:testDebugUnitTest" in workflow,
    "CI publishes APK": "DaniMAutoWarp-RC122-debug-apk" in workflow,
}
for label, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + label)
if not all(checks.values()): raise SystemExit(1)
