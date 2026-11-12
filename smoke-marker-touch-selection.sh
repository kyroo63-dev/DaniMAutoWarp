#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KOTLIN'
import com.danim.autowarp.analysis.MarkerTouchSelection as Hit
fun main() {
    val m = listOf(0.0, 1.0, 1.02, 2.0)
    check(Hit.nearest(m, 100f, 200f, 0.0, 2.0, 24f) == 1)
    check(Hit.nearest(m, 102f, 200f, 0.0, 2.0, 24f) == 2)
    check(Hit.nearest(m, 150f, 200f, 0.0, 2.0, 24f) == -1)
    check(Hit.nearest(m, 100f, 0f, 0.0, 2.0, 24f) == -1)
    check(Hit.nearest(m, 100f, 200f, 0.0, 0.0, 24f) == -1)
    check(Hit.nearest(m, 100f, 200f, 0.0, 2.0, -1f) == -1)
    check(Hit.nearest(listOf(0.0, 2.0), 100f, 200f, 0.0, 2.0, 24f) == -1)
    check(Hit.nearest(m, 2f, 200f, 1.0, 0.1, 24f) == 1)
    println("PASS: marker touch selection, zoom, invalid geometry, endpoint protection")
}
KOTLIN
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/MarkerTouchSelection.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
