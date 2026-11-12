#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KOT'
import com.danim.autowarp.analysis.*
fun main() {
 val a=KickPhaseDrift.Window(0.0,12.0,20,0.9,10.0,5.0,true)
 val b=a.copy(startSeconds=12.0,endSeconds=24.0,reliable=false)
 check(PhaseWindowNavigator.at(listOf(a,b),0.0)==0)
 check(PhaseWindowNavigator.at(listOf(a,b),12.0)==1)
 check(PhaseWindowNavigator.at(listOf(a,b),24.0)==-1)
 check(PhaseWindowNavigator.at(listOf(a,b),Double.NaN)==-1)
 check(kotlin.math.abs(PhaseWindowNavigator.viewport(b,120.0,10.0)-12.0)<1e-6)
 check(PhaseWindowNavigator.viewport(a,120.0,10.0)==0.0)
 check(PhaseWindowNavigator.nearbyKicks(listOf(13.0,12.0,24.0,Double.NaN,16.0),b)==listOf(12.0,13.0,16.0))
 println("PASS: tap selection, edges, viewport bounds, kick filtering")
}
KOT
kotlinc app/src/main/java/com/danim/autowarp/analysis/KickPhaseDrift.kt app/src/main/java/com/danim/autowarp/analysis/KickPulseAudit.kt app/src/main/java/com/danim/autowarp/analysis/PhaseWindowNavigator.kt "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
