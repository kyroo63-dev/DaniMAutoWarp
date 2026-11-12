#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KOTLIN'
import com.danim.autowarp.analysis.TempoContinuityGate
fun main() {
  val stable=(0..100).map { it * 60.0 / 135.0 }
  check(TempoContinuityGate.assess(stable).reliable)
  val drift=(0..100).scan(0.0) { t,i -> t + 60.0 / (135.0 + i * 0.015) }.drop(1)
  check(TempoContinuityGate.assess(drift).reliable)
  val jump=(0..100).scan(0.0) { t,i -> t + 60.0 / (if (i < 52) 135.0 else 180.0) }.drop(1)
  check(!TempoContinuityGate.assess(jump).reliable)
  check(!TempoContinuityGate.assess(listOf(0.0, 0.0, 1.0)).reliable)
  check(!TempoContinuityGate.assess(listOf(0.0, Double.NaN)).reliable)
  println("PASS: stable 135 BPM, gradual vinyl drift, abrupt 135->180 BPM, invalid beat tracks")
}
KOTLIN
kotlinc app/src/main/java/com/danim/autowarp/analysis/TempoContinuityGate.kt "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
