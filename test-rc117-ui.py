from pathlib import Path
import re
s=Path("app/src/main/java/com/danim/autowarp/MainActivity.kt").read_text()
b=Path("app/build.gradle.kts").read_text()
checks={
 "badge and release version": 'Text("RC117"' in s and 'versionName = "4.0.0-rc117"' in b,
 "real kick count": '${result?.kickTimes?.size ?: 0} ATAQUES' in s,
 "no fixed attack count": '64 ATAQUES' not in s,
 "waveform from cache": 'waveform?.peaks' in s,
 "transients from analysis": 'analysis.kickTimes, analysis.otherTransients' in s,
 "BPM graph uses time": 'valid[i].first-minTime' in s,
 "render presets named accurately": all(x in s for x in ('Triple("Rápido"','Triple("Equilibrado"','Triple("Alta calidad"')),
 "audio player lifecycle": 'override fun onStop()' in s and 'AudioPreview.stop()' in s,
 "picker import": 'open.launch(arrayOf("audio/*"))' in s,
}
for name, ok in checks.items(): print(("PASS" if ok else "FAIL"),name)
assert all(checks.values()), "RC117 UI contract failed"
