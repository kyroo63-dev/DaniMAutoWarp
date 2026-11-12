# DaniM Auto Warp 4.0 RC97

- New offline `audit-rc97-rhythm-runs.py` for phase/tempo-consistent candidate chains inside independent clips.
- Replays RC91 event metadata after RC94 anchored clustering and RC95 evidence gate.
- Does not extrapolate missing beats, merge clips, calibrate kick identity, or authorize production warp.
- Results on 22 segments: 499 filtered events, 13 clips meet **diagnostic-only** chain stability, 0 newly authorized warps.
- `test-rc97-rhythm-runs.py`: 12 synthetic assertions passed.
- Android APK build, device UI tests, and listening A/B tests are not performed.
