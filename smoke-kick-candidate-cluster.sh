#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickCandidateCluster
import com.danim.autowarp.analysis.KickCandidateCluster.Candidate
fun main() {
 fun c(t:Double,s:Double)=Candidate(t,s)
 val g=KickCandidateCluster
 check(g.group(emptyList(),120.0).retained.isEmpty())
 check(g.group(listOf(c(1.0,.4)),120.0).suppressed==0)
 check(g.group(listOf(c(0.0,.3),c(.1,.9),c(.2,.5)),120.0).retained.single().seconds==.1)
 check(g.group(listOf(c(0.0,.5),c(.5,.7),c(1.0,.6)),120.0).retained.size==3)
 check(g.group(listOf(c(.1,.2),c(0.0,.8)),120.0).retained.single().seconds==0.0)
 check(g.group(listOf(c(.1,.8),c(0.0,.8)),120.0).retained.single().seconds==0.0)
 check(g.group(listOf(c(0.0,.1),c(.2,.2),c(.4,.9)),120.0).retained.size==2)
 check(g.group(listOf(c(0.0,.1),c(.2,.2),c(.4,.9)),120.0).retained.map { it.seconds }==listOf(.2,.4))
 check(g.group(listOf(c(0.0,.9),c(.15,.3),c(.30,.4),c(.45,.8)),120.0).retained.map { it.seconds }==listOf(0.0,.45))
 check(g.group(listOf(c(0.0,.9),c(.15,.3),c(.30,.4),c(.45,.8)).reversed(),120.0).retained.map { it.seconds }==listOf(0.0,.45))
 check(runCatching {g.group(listOf(c(0.0,Double.NaN)),120.0)}.isFailure)
 check(runCatching {g.group(listOf(c(0.0,.3)),0.0)}.isFailure)
 println("12 pruebas KickCandidateCluster: OK")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickCandidateCluster.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
