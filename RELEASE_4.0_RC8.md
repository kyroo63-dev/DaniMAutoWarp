# DaniM Auto Warp 4.0 RC8 — PCM scaling correction

## Critical audio fix

`ContinuousAbPreview` now converts both original and corrected signed PCM16 samples to normalized float before calling `AbCrossfade.mix`. Previously both were passed as ~[-32768,32767] and then clipped to [-1,1], producing heavily distorted preview output. The mix is converted back to PCM16 only once at the output boundary.

## Verification

- Core Kotlin compile (non-Android files): passed with Kotlin compiler.
- Regression checks for normalized PCM mixing: passed with Kotlin compiler.
- Full Android APK build and physical device playback: **not run**, Android SDK/Gradle absent.
- MP3 export: not implemented.
