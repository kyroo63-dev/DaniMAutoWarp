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
    val identity = listOf(WarpMarker(0.0, 0.0), WarpMarker(60.0, 60.0))
    val no = ManualKickAnchor.insert(identity, 12.5, false)
    check(!no.accepted && no.markers == identity)
    val yes = ManualKickAnchor.insert(identity, 12.5, true)
    check(yes.accepted && yes.markers.size == 3)
    check(yes.markers[1].sourceSeconds == 12.5 && yes.markers[1].targetSeconds == 12.5)
    check(identity.size == 2) // undo snapshot remains untouched
    check(!ManualKickAnchor.insert(yes.markers, 12.51, true).accepted)
    check(!ManualKickAnchor.insert(identity, 0.0, true).accepted)
    check(!ManualKickAnchor.insert(identity, 60.0, true).accepted)
    check(!ManualKickAnchor.insert(identity, Double.NaN, true).accepted)
    val warped = listOf(WarpMarker(0.0, 0.0), WarpMarker(10.0, 20.0))
    check(!ManualKickAnchor.insert(warped, 5.0, true).accepted)
    val offset = listOf(WarpMarker(0.0, 0.0), WarpMarker(10.0, 10.1))
    val interpolate = ManualKickAnchor.insert(offset, 5.0, true)
    check(interpolate.accepted && kotlin.math.abs(interpolate.markers[1].targetSeconds - 5.05) < 1e-8)
    val nudge = ManualPhaseNudge.apply(yes.markers, 12.0, 13.0, 10)
    check(nudge.accepted && nudge.changed == 1)
    println("PASS: explicit confirmation, neutral insert, interpolation, spacing, boundaries, undo, nudge")
}
KT
kotlinc "$TMP/Models.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualKickAnchor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualPhaseNudge.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
