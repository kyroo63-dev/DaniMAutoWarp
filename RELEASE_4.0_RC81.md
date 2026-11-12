# DaniM Auto Warp 4.0 RC81

- Extracted the conservative consecutive-pulse confidence gate into `KickSequenceGate` for independent verification.
- Added `smoke-kick-sequence-gate.sh` with nine deterministic cases: isolated, two/three corroborated, dense unsupported, few-candidate fallback, exact threshold, mixed input, empty input and invalid values.
- The gate cannot increase confidence; raw candidates remain available for manual review.
- Ran production BeatAnalyzer against five 8-second clips from the supplied real FLAC: confidence 0, .95, 0, 0, 0; only the 75s window permits automatic warp. No change from RC80.
- No annotated kick ground truth; detector accuracy not established. No Android SDK / APK compilation or listening validation performed.
