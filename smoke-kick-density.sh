#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"; TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickDensityGate
fun main() {
 check(KickDensityGate.assess(19,8.0).plausible)
 check(!KickDensityGate.assess(40,8.0).plausible)
 check(KickDensityGate.assess(0,8.0).plausible)
 check(!KickDensityGate.assess(36,10.0).plausible)
 println("PASS: plausible, excessive, empty, and threshold kick-density checks")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickDensityGate.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
