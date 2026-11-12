#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Smoke.kt" <<'KOTLIN'
import com.danim.autowarp.audio.*
import java.io.ByteArrayOutputStream
fun main() {
    val input = PcmAudio(44100, 1, floatArrayOf(-1f, 0f, 1f, Float.NaN, Float.POSITIVE_INFINITY))
    val bytes = ByteArrayOutputStream().also { PcmWave.write(input, it) }.toByteArray()
    val decoded = PcmWave.read(bytes.inputStream())
    check(decoded.samples.size == 5 && decoded.samples[0] == -1f)
    check(decoded.samples[3] == 0f && decoded.samples[4] == 0f)
    // A WAV header cannot represent a PCM16 byte rate above unsigned 32-bit.
    check(runCatching { PcmWave.write(PcmAudio(Int.MAX_VALUE, 2, floatArrayOf(0f, 0f)), ByteArrayOutputStream()) }.isFailure)
    val highRate = ByteArrayOutputStream().also {
        PcmWave.write(PcmAudio(1_000_000_000, 1, floatArrayOf(0f)), it)
    }.toByteArray()
    check(PcmWave.read(highRate.inputStream()).sampleRate == 1_000_000_000)
    val corrupt = bytes.copyOf()
    // Unsigned chunk size 0xFFFFFFFF must not wrap into a negative Int.
    for (i in 16..19) corrupt[i] = 0xff.toByte()
    check(runCatching { PcmWave.read(corrupt.inputStream()) }.isFailure)
    println("PASS: WAV roundtrip, invalid samples, byte-rate overflow, unsigned chunk validation")
}
KOTLIN
kotlinc app/src/main/java/com/danim/autowarp/audio/PcmWave.kt "$TMP/Smoke.kt" -include-runtime -d "$TMP/smoke.jar"
java -jar "$TMP/smoke.jar"
