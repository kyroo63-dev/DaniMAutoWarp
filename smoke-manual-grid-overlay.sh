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
 val markers = listOf(WarpMarker(0.0,0.0), WarpMarker(10.0,10.0))
 val p = ManualGridPreview.inspect(markers, 5.01, 120.0)
 check(p.accepted)
 val a = ManualGridOverlay.project(p, 4.0, 6.0)!!
 check(abs(a.sourceFraction!! - 0.505) < 1e-8)
 check(abs(a.targetFraction!! - 0.5) < 1e-8)
 val b = ManualGridOverlay.project(p, 5.005, 5.02)!!
 check(b.sourceFraction != null && b.targetFraction == null)
 check(ManualGridOverlay.project(p, 5.0, 5.0) == null)
 check(ManualGridOverlay.project(p.copy(accepted=false), 4.0, 6.0) == null)
 check(markers.size == 2 && markers[0].targetSeconds == 0.0)
 println("PASS: waveform overlay positions, clipping, invalid viewport, rejected preview, read-only")
}
KT
kotlinc "$TMP/Models.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualBeatGrid.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualGridPreview.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualGridOverlay.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
