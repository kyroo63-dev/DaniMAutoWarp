# DaniM Auto Warp 4.0 RC3 — playback safety fixes

- Validate A/B warp timeline before stopping an existing preview or allocating a new AudioTrack.
- Handle original audio windows near the final PCM frame without invalid clamp bounds.
- Reject an uninitialized AudioTrack before calling play.

## Unresolved release blockers
- Full Gradle Android build and device-level UI/playback tests are not available in this environment.
- MP3 320 kbps export remains unimplemented; WAV/AIFF/FLAC options remain available.
- Real vinyl quality assessment and long-file stress testing remain pending.
