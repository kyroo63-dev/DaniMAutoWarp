from pathlib import Path
r=Path(__file__).resolve().parent
s=(r/'sincronizar_github.py').read_text()
g=(r/'app/build.gradle.kts').read_text()
w=(r/'.github/workflows/android-apk.yml').read_text()
checks={
 'version RC124':'versionName = "4.0.0-rc124"' in g,
 'versionCode 164':'versionCode = 164' in g,
 'symlinks omitted':'f.is_symlink()' in s,
 'remote symlinks blocked':'target.is_symlink()' in s,
 'workflow push requires confirmation':'--allow-workflow' in s,
 'dry run default':'if not args.push:' in s,
 'only staged files committed':'git("add", "--", *copied' in s,
 'CI APK build':':app:assembleDebug' in w,
 'CI JVM tests':':app:testDebugUnitTest' in w,
 'CI artifact':'DaniMAutoWarp-RC124-debug-apk' in w,
}
for name,passed in checks.items(): print(('PASS' if passed else 'FAIL')+': '+name)
if not all(checks.values()):raise SystemExit(1)
