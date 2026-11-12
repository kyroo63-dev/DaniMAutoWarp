# DaniM Auto Warp 4.0 RC90

- Full-track *sampling* diagnostic: 11 independent 24-second windows at 0, 48, ... 480 seconds, covering 264 of 507.48 seconds (52.0%). **This is not continuous full-song analysis**; 24-second gaps remain between clips and a short tail is untested.
- `smoke-real-long-audit.sh` now defaults to these windows and exports `absolute_event_s` in the events CSV; local event times remain available as `event_s`.
- `summarize-full-timeline.py` exports a compact timeline and explicitly labels independent clips.
- On real FLAC: isolated detector reports warp=true only at offsets 0 and 48 seconds, false at the remaining nine. This does **not** mean a full-track correction is safe or that joins will be artifact-free.
- Diagnostic tools only; no production engine changes, no automatic merging of independent clip markers, no fabricated missing beats.
- Passed `smoke-kick-tempo-window-audit.sh` (12 cases) and `smoke-beat-tracking.sh`; real FLAC audit executed successfully.
- No Android SDK available for APK compilation or UI testing. No listening A/B validation performed.
