# DaniM Auto Warp 4.0 RC2 — stabilization

## Corrected
- Continuous A/B now reads corrected PCM from the selected timeline offset rather than always starting at frame zero.
- Preview length is limited by corrected audio remaining after that offset and by the final warp marker.
- The start position is validated against the marker interval.
- Clarified that the A/B fade is linear, not equal-power, to avoid +3 dB gain on correlated signals.

## Release blockers
- Full Android Gradle build and device tests have not been performed in this environment.
- MP3 320 kbps export is not implemented; do not advertise it as supported.
- Real vinyl recordings and long-file performance still need evaluation.

## Validation
- Kotlin DSP core compiled with kotlinc (see build log if supplied).
- APK installation, UI, playback timing, and export need Android hardware testing.
