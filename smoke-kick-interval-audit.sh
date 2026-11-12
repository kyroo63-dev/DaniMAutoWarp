#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickIntervalAudit
fun main() {
 fun check(ok:Boolean, label:String) { require(ok) { label }; println("PASS $label") }
 val a=KickIntervalAudit.inspect(listOf(0.0,0.5,1.0,1.5),120.0)
 check(a.pulsePairs==3 && a.longestPulseRun==4,"regular sequence")
 val b=KickIntervalAudit.inspect(listOf(0.0,0.10,0.20,0.30),120.0)
 check(b.closePairs==3 && b.pulsePairs==0,"dense low-frequency attacks")
 val c=KickIntervalAudit.inspect(listOf(0.0,1.0,2.0),120.0)
 check(c.missedPulsePairs==2 && c.longestPulseRun==1,"missing beats")
 val d=KickIntervalAudit.inspect(listOf(0.0,0.73,1.40),120.0)
 check(d.irregularPairs==2,"off-grid intervals")
 check(KickIntervalAudit.inspect(listOf(0.1),120.0).medianIntervalSeconds==null,"isolated candidate")
 check(KickIntervalAudit.inspect(listOf(0.0,0.0,0.5),120.0).candidateCount==2,"duplicate timestamps")
 check(runCatching { KickIntervalAudit.inspect(listOf(Double.NaN),120.0) }.isFailure,"invalid timestamp")
 check(runCatching { KickIntervalAudit.inspect(listOf(0.0),0.0) }.isFailure,"invalid bpm")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickIntervalAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
