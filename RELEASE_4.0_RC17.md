# DaniM Auto Warp 4.0 RC17

- Validate playable A/B preview length before cancelling the current session.
- Reject invalid or sub-sample Warp ranges before allocating AudioTrack.
- Bound playback to both the rendered preview and the available target Warp interval.
- Add JVM unit tests for bounds and a standalone executable smoke test.

Not yet validated: full Android Gradle build, device playback, real vinyl audio quality. MP3 320 kbps export is not implemented.
