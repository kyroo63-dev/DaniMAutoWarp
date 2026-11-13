from pathlib import Path
import re
root=Path(__file__).resolve().parent
main=(root/"app/src/main/java/com/danim/autowarp/MainActivity.kt").read_text()
build=(root/"app/build.gradle.kts").read_text()
workflow=(root/".github/workflows/android-apk.yml").read_text()
checks={
"version RC118": 'versionName = "4.0.0-rc118"' in build,
"no fake 64 kick count": not re.search(r'("64"|\\b64\\s*ataques)',main,re.I),
"audio waveform from cache": "waveform?.peaks" in main,
"tempo graph time axis": "valid[i].first-minTime" in main,
"waveform canvas peak cap": "coerceIn(1, 4096)" in main,
"real transients": "analysis.otherTransients" in main,
"quality choices": all(x in main for x in ["WarpQuality.FAST","WarpQuality.BALANCED","WarpQuality.HIGH"]),
"APK artifact": "app/build/outputs/apk/debug/*.apk" in workflow,
"JVM tests": ":app:testDebugUnitTest" in workflow,
"test reports artifact": "DaniMAutoWarp-RC118-test-reports" in workflow,
}
for name, ok in checks.items():print(("PASS" if ok else "FAIL"),name)
if not all(checks.values()):raise SystemExit(1)
