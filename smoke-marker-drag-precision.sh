#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KOTLIN'
import com.danim.autowarp.analysis.MarkerDragPrecision as Drag
import kotlin.math.abs
fun near(a: Double?, b: Double) = a != null && abs(a-b) < 1e-8
fun main() {
    check(Drag.proposedSource(1.0, 100f, 100f, 200f, 2.0, 0.0, 2.0) == null)
    check(near(Drag.proposedSource(1.0, 110f, 111f, 200f, 2.0, 0.0, 2.0), 1.01))
    check(near(Drag.proposedSource(1.0, 110f, 109f, 200f, 2.0, 0.0, 2.0), 0.99))
    check(near(Drag.proposedSource(1.0, 100f, 300f, 200f, 2.0, 0.0, 1.2), 1.199))
    check(Drag.proposedSource(1.0, 100f, 101f, 0f, 2.0, 0.0, 2.0) == null)
    check(Drag.proposedSource(1.0, 100f, 101f, 200f, Double.NaN, 0.0, 2.0) == null)
    check(Drag.proposedSource(1.0, 100f, 101f, 200f, 2.0, 0.9999, 1.0001) == null)
    println("PASS: marker drag offset, fine motion, clamp, invalid geometry")
}
KOTLIN
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/MarkerDragPrecision.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
