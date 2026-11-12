#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.*
fun main() {
 fun c(t:Double,s:Double=.9)=KickCandidateCluster.Candidate(t,s)
 fun test(n:String,b:Boolean) { check(b) { n }; println("PASS $n") }
 val good=(0..15).map { c(it*.5) }
 test("coherent holdout",KickHoldoutPhaseAudit.inspect(good,120.0).sufficientEvidence)
 test("shuffled input",KickHoldoutPhaseAudit.inspect(good.reversed(),120.0).sufficientEvidence)
 test("empty",!KickHoldoutPhaseAudit.inspect(emptyList(),120.0).sufficientEvidence)
 test("short",!KickHoldoutPhaseAudit.inspect(good.take(5),120.0).sufficientEvidence)
 test("late phase drift",!KickHoldoutPhaseAudit.inspect((0..7).map { c(it*.5) }+(8..15).map { c(it*.5+.13) },120.0).sufficientEvidence)
 test("late offgrid",!KickHoldoutPhaseAudit.inspect((0..7).map { c(it*.5) }+(8..15).map { c(it*.5+.22) },120.0).sufficientEvidence)
 test("half-time train can pass: limitation",KickHoldoutPhaseAudit.inspect((0..15).map { c(it*1.0) },120.0).sufficientEvidence)
 test("invalid bpm",runCatching { KickHoldoutPhaseAudit.inspect(good,0.0) }.isFailure)
 test("invalid evidence",runCatching { KickHoldoutPhaseAudit.inspect(listOf(c(0.0,Double.NaN)),120.0) }.isFailure)
 test("invalid time",runCatching { KickHoldoutPhaseAudit.inspect(listOf(c(-1.0)),120.0) }.isFailure)
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickCandidateCluster.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickHoldoutPhaseAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
