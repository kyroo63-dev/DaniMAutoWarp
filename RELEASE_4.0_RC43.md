# DaniM Auto Warp 4.0 RC43

- Added `KickPhaseDrift` diagnostic for independent 12-second phase-support windows. This is diagnostic only; no automatic warp authorization.
- Added `smoke-kick-phase-drift.sh`: stable 136 BPM, 75 ms phase shift, sparse and invalid attacks. PASS.
- Extended `smoke-real-vinyl.sh` with phase-window coverage and adjacent reliable phase differences.
- Real FLAC `Javi Golo & Andres Gomes - Expanden.flac`: six 60-second excerpts at offsets 0, 80, 160, 240, 320, 420 s; each reported 136.36 BPM, 0.000 confidence and two identity warp markers. Reliable phase windows per excerpt: 5/5, 4/5, 2/5, 3/5, 4/5, 5/5. Maximum measured adjacent reliable shifts: 30, 30, 10, 30, 20, 30 ms respectively. These phase observations do not prove a correct beatgrid or a valid time stretch.
- No Android SDK build, APK, or real corrected audio A/B produced.
