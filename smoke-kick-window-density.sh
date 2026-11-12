#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickDensityGate
fun main() {
 val g=KickDensityGate
 check(g.assessTimeline(listOf(0.0,0.5,1.0,1.5,2.0,2.5),8.0).plausible)
 val burst=(0..11).map { it * 0.12 }
 val result=g.assessTimeline(burst,8.0)
 check(result.global.plausible && !result.plausible && result.maximumWindowDensity > 3.5)
 check(!g.assessTimeline((0..39).map { it * 0.19 },8.0).plausible)
 check(g.assessTimeline(emptyList(),8.0).plausible)
 check(g.assessTimeline(listOf(0.1,0.2),0.5).plausible.not())
 println("PASS: 5 timeline density tests (steady, burst, global excess, empty, short clip)")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickDensityGate.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
