#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickCandidateEvidenceGate as G
fun main() {
 fun c(t:Double,b:Double=.9,s:Double=.9)=G.Candidate(t,b,s)
 val seq=listOf(c(0.0),c(.5),c(1.0),c(1.5))
 check(G.inspect(emptyList(),120.0).accepted==0)
 check(G.inspect(seq,120.0).accepted==4)
 check(G.inspect(listOf(c(0.0)),120.0).accepted==0)
 check(G.inspect(listOf(c(0.0),c(.5)),120.0).accepted==0)
 check(G.inspect(seq.map { it.copy(bassShare=.1) },120.0).accepted==0)
 check(G.inspect(seq.map { it.copy(spectralScore=.1) },120.0).accepted==0)
 check(G.inspect(seq+listOf(c(.23)),120.0).decisions.first { it.candidate.seconds == .23 }.accepted.not())
 check(G.inspect(seq.reversed(),120.0).accepted==4)
 check(G.inspect(seq,120.0).decisions.first().supportedNeighbours==2)
 check(runCatching {G.inspect(seq,0.0)}.isFailure)
 check(runCatching {G.inspect(listOf(c(Double.NaN)),120.0)}.isFailure)
 check(runCatching {G.inspect(seq,120.0,beatTolerance=.6)}.isFailure)
 println("12 pruebas KickCandidateEvidenceGate: OK")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickCandidateEvidenceGate.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
