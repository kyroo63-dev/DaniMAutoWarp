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
 val original = listOf(WarpMarker(0.0,0.0),WarpMarker(10.0,10.0))
 val p = ManualGridPreview.inspect(original, 5.01, 120.0)
 check(p.accepted && abs(p.proposedTargetSeconds!! - 5.0)<1e-8)
 check(abs(p.shiftMs!! + 10.0)<1e-8)
 check(p.leftStretchPercent!! < 0 && p.rightStretchPercent!! > 0)
 check(original.size==2)
 check(!ManualGridPreview.inspect(original, 5.01, 300.0).accepted)
 check(!ManualGridPreview.inspect(original, 0.0, 120.0).accepted)
 check(!ManualGridPreview.inspect(original, 5.01, 120.0, confirmedByUser=false).accepted)
 println("PASS: preview target, delta, stretch, read-only, rejected parameters")
}
KT
kotlinc "$TMP/Models.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualBeatGrid.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualGridPreview.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
