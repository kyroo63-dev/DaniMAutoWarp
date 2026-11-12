#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KOTLIN'
import com.danim.autowarp.analysis.*
import kotlin.math.*
fun main() {
 val bpm=136.0; val p=60.0/bpm
 val stable=(0..135).map { it*p + 0.002*sin(it.toDouble()) }
 val a=KickPhaseDrift.inspect(stable,bpm,60.0)
 check(a.size==5 && a.all { it.reliable }) { "Stable pulse rejected: $a" }
 val drift=(0..135).map { it*p + if(it>=55) 0.075 else 0.0 }
 val b=KickPhaseDrift.inspect(drift,bpm,60.0)
 check(b.size==5 && b.any { it.reliable })
 val shift=KickPhaseDrift.phaseDifferenceMs(b[0],b[3],bpm)
 check(shift != null && abs(shift-75.0)<5.0) { "Missing phase shift: $shift" }
 val sparse=KickPhaseDrift.inspect(listOf(0.0,1.0,Double.NaN),bpm,30.0)
 check(sparse.none { it.reliable })
 println("PASS: 5 stable windows, 75 ms phase shift detected, sparse/invalid kicks rejected")
}
KOTLIN
kotlinc app/src/main/java/com/danim/autowarp/analysis/KickPulseAudit.kt app/src/main/java/com/danim/autowarp/analysis/KickPhaseDrift.kt "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
