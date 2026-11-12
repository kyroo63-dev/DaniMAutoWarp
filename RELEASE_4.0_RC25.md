# DaniM Auto Warp 4.0 RC25

## Build pipeline
- CI now runs `smoke-core.sh` before Android JUnit tests and debug APK assembly.
- On a failed CI job, test reports are uploaded for debugging.
- Updated Android version to 4.0.0-rc25 (versionCode 64).

## Verification
- Local Kotlin smoke test: run in this release process.
- Full Android build and real-device testing: not executed here.
- No installable APK is included.
