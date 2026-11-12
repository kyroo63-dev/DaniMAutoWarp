#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Models.kt" <<'KT'
package com.danim.autowarp.analysis
data class WarpMarker(val sourceSeconds: Double, val targetSeconds: Double, val strength: Float = 1f)
KT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.*
fun main() {
    val markers = (0..10).map { WarpMarker(it.toDouble(), it.toDouble()) }
    val ok = ManualPhaseNudge.apply(markers, 3.0, 5.0, 10)
    check(ok.accepted && ok.changed == 2 && ok.markers[3].targetSeconds == 3.01 && ok.markers[5] == markers[5])
    check(markers[3].targetSeconds == 3.0) // immutable input / undo snapshot
    check(!ManualPhaseNudge.apply(markers, 20.0, 21.0, 5).accepted)
    check(!ManualPhaseNudge.apply(markers, 3.0, 5.0, 20).accepted)
    check(!ManualPhaseNudge.apply(listOf(markers[0], markers[1]), 0.0, 1.0, 5).accepted)
    val tight = listOf(WarpMarker(0.0,0.0), WarpMarker(.01,.01), WarpMarker(.02,.02), WarpMarker(.03,.03))
    check(!ManualPhaseNudge.apply(tight, .01, .02, 10).accepted)
    println("PASS: nudge, boundaries, undo snapshot, empty, invalid, unsafe stretch")
}
KT
kotlinc "$TMP/Models.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualPhaseNudge.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
