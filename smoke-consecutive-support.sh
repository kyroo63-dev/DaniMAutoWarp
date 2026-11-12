#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickRhythmEvidence
fun main() {
 fun checkScores(name:String, t:List<Double>, positive:Int) {
  val scores=KickRhythmEvidence.consecutiveSupport(t,120.0)
  check(scores.count { it>0.0 } == positive) { "$name: $scores" }
  println("PASS $name: $scores")
 }
 checkScores("isolated",listOf(0.5),0)
 checkScores("two_hits",listOf(0.5,1.0),2)
 checkScores("three_hits",listOf(0.5,1.0,1.5),3)
 checkScores("duplicate_slot",listOf(0.5,0.501),0)
 checkScores("gap",listOf(0.5,1.0,2.0),2)
 checkScores("offbeat",listOf(0.5,1.0,1.5,1.77),3)
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickRhythmEvidence.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
