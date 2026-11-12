# DaniM Auto Warp 4.0 RC10

## Fixed
- A/B original-source block bounds are now computed from every output-frame source position. Variable warp marker strengths can produce non-monotonic mappings; using only the endpoints could select an incomplete source window and clamp interpolation to the wrong sample.
- The frame source positions are cached per block to avoid recalculating the mapping inside each channel loop.

## Validation
- Kotlin core compilation required. Android full Gradle build, device playback and real-vinyl quality checks are not verified.
- MP3 320 kbps encoding is not implemented.
