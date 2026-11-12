#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.*
fun main() {
 fun c(t:Double,s:Double)=KickCandidateCluster.Candidate(t,s)
 fun checkCase(name:String, actual:Boolean) { check(actual) { name }; println("PASS $name") }
 val bpm=120.0
 val strong=(0..7).map { c(it*.5,.8) }
 val good=KickSpectralGridAudit.inspect(strong,bpm,0.0)
 checkCase("eight strong beats",good.reliable && good.longestJointRun==8)
 checkCase("empty",!KickSpectralGridAudit.inspect(emptyList(),bpm,Double.NaN).reliable)
 checkCase("single",!KickSpectralGridAudit.inspect(listOf(c(0.0,1.0)),bpm,0.0).reliable)
 checkCase("weak spectrum",!KickSpectralGridAudit.inspect(strong.map { it.copy(evidence=.1) },bpm,0.0).reliable)
 checkCase("off grid",!KickSpectralGridAudit.inspect(strong.map { it.copy(seconds=it.seconds+.125) },bpm,0.0).reliable)
 checkCase("sparse beats",!KickSpectralGridAudit.inspect((0..7).map { c(it*1.0,.9) },bpm,0.0).reliable)
 checkCase("duplicates not beats",!KickSpectralGridAudit.inspect((0..7).map { c(0.0,.9) },bpm,0.0).reliable)
 checkCase("short run",!KickSpectralGridAudit.inspect(strong.take(4),bpm,0.0).reliable)
 checkCase("spectral threshold",KickSpectralGridAudit.inspect(strong,bpm,0.0,.9).jointlySupported==0)
 checkCase("bad bpm",runCatching { KickSpectralGridAudit.inspect(strong,0.0,0.0) }.isFailure)
 checkCase("invalid score",runCatching { KickSpectralGridAudit.inspect(listOf(c(0.0,Double.NaN)),bpm,0.0) }.isFailure)
 checkCase("missing beat",KickSpectralGridAudit.inspect(strong.filterIndexed { i,_->i!=3 },bpm,0.0).distinctJointBeats==7)
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickCandidateCluster.kt" "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickSpectralGridAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
