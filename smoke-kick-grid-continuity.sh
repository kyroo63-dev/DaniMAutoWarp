#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickGridContinuityAudit as A
fun main() {
 fun checkGrid(t:List<Double>)=A.inspect(t,120.0)
 check(!checkGrid(emptyList()).reliable)
 check(!checkGrid(listOf(1.0)).reliable)
 check(!checkGrid(listOf(0.0,.5,1.0)).reliable)
 check(checkGrid(listOf(0.0,.5,1.0,1.5)).reliable)
 check(checkGrid(listOf(0.0,.5,1.0,1.5)).missingInteriorBeats==0)
 check(!checkGrid(listOf(0.0,1.0,2.0,3.0)).reliable)
 check(checkGrid(listOf(0.0,.5,1.0,1.5,2.0)).longestConsecutiveRun==5)
 check(checkGrid(listOf(0.0,.5,.51,1.0,1.5)).distinctMatchedBeats==4)
 check(checkGrid(listOf(0.0,.5,1.0,1.5,2.0,2.5,3.0,3.5,4.0,4.5)).coverage==1.0)
 check(checkGrid(listOf(0.0,.5,1.0,1.5,4.0)).missingInteriorBeats==4)
 check(runCatching { A.inspect(listOf(Double.NaN),120.0) }.isFailure)
 check(runCatching { A.inspect(listOf(0.0),0.0) }.isFailure)
 println("12 pruebas KickGridContinuityAudit: OK")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickGridContinuityAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
