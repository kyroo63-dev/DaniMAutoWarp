#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.*
fun main() {
 fun checkCase(name:String, scores:List<Double>, expected:Double) {
  val actual=KickSequenceGate.assess(scores).confidenceMultiplier
  check(actual==expected) { "$name expected=$expected actual=$actual" }
  println("PASS $name")
 }
 checkCase("isolated events",listOf(1.0,0.0,0.0,0.0,0.0,0.0),0.0)
 checkCase("two corroborated",listOf(0.9,0.9,0.0,0.0,0.0,0.0),0.0)
 checkCase("three corroborated",listOf(0.9,0.9,0.9,0.0,0.0,0.0),1.0)
 checkCase("dense unsupported",List(40){0.1},0.0)
 checkCase("few candidates conservative fallback",listOf(0.0,0.0,0.0,0.0,0.0),1.0)
 checkCase("exact threshold",listOf(.3,.3,.3,0.0,0.0,0.0),1.0)
 checkCase("mixed realistic",listOf(.7,.5,.1,.6,.1,.1,.1),1.0)
 checkCase("empty",emptyList(),1.0)
 try { KickSequenceGate.assess(listOf(Double.NaN)) ; error("NaN accepted") }
 catch (_:IllegalArgumentException) { println("PASS nonfinite rejected") }
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickSequenceGate.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
