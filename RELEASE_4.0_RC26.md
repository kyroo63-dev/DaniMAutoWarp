# DaniM Auto Warp 4.0 RC26

## CI build pipeline correction

The previous GitHub Actions workflow invoked `bash smoke-core.sh`, which requires the `kotlinc` CLI. `setup-java` does not install that command, so CI could fail before it even reached Android compilation.

- Removed the undeclared `kotlinc` dependency from GitHub Actions.
- JVM unit tests now run through Gradle with stack traces on failures.
- APK assembly remains a separate step, and artifacts upload only on success.
- `smoke-core.sh` remains available for local machines with Kotlin CLI installed.

## Verification

- Local Kotlin smoke test: run in this release environment.
- Full Android Gradle compilation: not performed (Android SDK / Gradle unavailable locally).
- No APK included.
