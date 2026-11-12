# DaniM Auto Warp 4.0 RC21

- WAV RIFF parser now interprets chunk sizes as unsigned 32-bit values rather than silently turning high-bit lengths into zero.
- Rejects truncated fmt chunks rather than accepting incomplete metadata.
- Includes reproducible offline Kotlin core smoke test script.
- Full Android APK and device tests remain pending.
