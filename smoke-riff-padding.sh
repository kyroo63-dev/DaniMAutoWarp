#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Padding.kt" <<'KOTLIN'
import com.danim.autowarp.audio.*
import java.io.ByteArrayOutputStream
fun main() {
    val wav=ByteArrayOutputStream().also { PcmWave.write(PcmAudio(44100,1,floatArrayOf(0f)),it) }.toByteArray()
    val junk=byteArrayOf(74,85,78,75,1,0,0,0,42,0)
    val valid=wav.copyOfRange(0,12)+junk+wav.copyOfRange(12,wav.size)
    val size=valid.size-8
    for(i in 0..3) valid[4+i]=(size ushr (8*i)).toByte()
    check(PcmWave.read(valid.inputStream()).frames==1)
    val invalid=valid.copyOfRange(0,21)+valid.copyOfRange(22,valid.size)
    val invalidSize=invalid.size-8
    for(i in 0..3) invalid[4+i]=(invalidSize ushr (8*i)).toByte()
    check(runCatching { PcmWave.read(invalid.inputStream()) }.isFailure)
    // An odd-sized trailing unknown chunk also requires a pad byte.
    val trailing=wav+byteArrayOf(74,85,78,75,1,0,0,0,42)
    val trailingSize=trailing.size-8
    for(i in 0..3) trailing[4+i]=(trailingSize ushr (8*i)).toByte()
    check(runCatching { PcmWave.read(trailing.inputStream()) }.isFailure)
    println("PASS: RIFF odd chunk padding, valid and invalid")
}
KOTLIN
kotlinc app/src/main/java/com/danim/autowarp/audio/PcmWave.kt "$TMP/Padding.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
