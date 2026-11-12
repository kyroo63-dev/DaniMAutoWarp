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
 val p = ManualGridPreview.inspect(listOf(WarpMarker(0.0,0.0),WarpMarker(10.0,10.0)),5.01,120.0)
 check(p.accepted)
 val pos = ManualGridOverlay.project(p,4.0,6.0)!!
 val near = ManualGridOverlay.detail(pos,1000f)!!
 check(near.needsMagnification && near.separationPx!! < 12f)
 check(ManualGridOverlay.detail(pos,5000f)!!.needsMagnification == false)
 check(ManualGridOverlay.detail(ManualGridOverlay.Positions(null,0.3),1000f)!!.separationPx == null)
 check(ManualGridOverlay.detail(null,1000f) == null)
 check(ManualGridOverlay.detail(pos,Float.NaN) == null)
 check(ManualGridOverlay.detail(ManualGridOverlay.Positions(Double.NaN,2.0),1000f) == null)
 println("PASS: magnification threshold, zoom, clipping, invalid geometry")
}
KT
kotlinc "$TMP/Models.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualBeatGrid.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualGridPreview.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/ManualGridOverlay.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
