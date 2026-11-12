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
import kotlin.math.abs
fun main() {
    val identity = listOf(WarpMarker(0.0, 0.0), WarpMarker(10.0, 10.0))
    val aligned = ManualBeatGrid.align(identity, 5.01, 120.0, confirmedByUser = true)
    check(aligned.accepted && aligned.markers.size == 3)
    check(abs(aligned.markers[1].targetSeconds - 5.0) < 1e-9)
    check(identity.size == 2)
    check(!ManualBeatGrid.align(identity, 5.01, 120.0, confirmedByUser = false).accepted)
    check(!ManualBeatGrid.align(identity, 5.01, Double.NaN, confirmedByUser = true).accepted)
    check(!ManualBeatGrid.align(identity, 5.01, 300.0, confirmedByUser = true).accepted)
    check(!ManualBeatGrid.align(identity, 5.01, 120.0, Double.NaN, true).accepted)
    check(!ManualBeatGrid.align(identity, 0.0, 120.0, confirmedByUser = true).accepted)
    check(!ManualBeatGrid.align(listOf(WarpMarker(4.8,4.8), WarpMarker(5.2,5.2)), 5.01, 120.0, 400.0, true).accepted) // target crosses neighbor
    val changed = ManualBeatGrid.align(aligned.markers, 5.01, 120.0, 10.0, true)
    check(changed.accepted && changed.markers.size == 3 && abs(changed.markers[1].targetSeconds - 5.01) < 1e-9)
    val adjacent = listOf(WarpMarker(0.0,0.0),WarpMarker(5.0,5.0),WarpMarker(10.0,10.0))
    check(!ManualBeatGrid.align(adjacent, 5.01, 120.0, confirmedByUser = true).accepted)
    println("PASS: manual grid insertion, phase offset, edit, validation, continuity, no implicit changes")
}
KT
kotlinc "$TMP/Models.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualBeatGrid.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
