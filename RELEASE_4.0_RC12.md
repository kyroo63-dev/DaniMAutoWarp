# DaniM Auto Warp 4.0 RC12 — playback lifecycle stabilization

- The A/B stop action now signals cancellation without touching AudioTrack from another thread.
- The playback thread remains the sole owner of AudioTrack.stop/release; this removes a pause/flush vs blocking write race.
- Cancellation is checked at each 256-frame block and during partial writes.
- Existing audio analysis and export functions are unchanged.

## Validation status
- Kotlin non-Android core compilation: see delivery notes.
- Full Android Gradle build and physical-device playback: **not performed**.
- MP3 320 kbps export: **not implemented**.
- A blocked platform AudioTrack.write may delay cancellation; device validation is still required.
