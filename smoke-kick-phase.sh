#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
cat > "$T/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickPhaseEvidence
fun main() {
 val beats=(0..39).map { it * 60.0/135.0 }
 val clean=KickPhaseEvidence.assess(beats,beats,60.0/135)
 check(clean.reliable && clean.supportRatio==1.0 && clean.medianErrorMs==0.0)
 val sparse=KickPhaseEvidence.assess(beats,beats.filterIndexed { i,_->i%4==0 },60.0/135)
 check(!sparse.reliable && sparse.supportRatio<0.55)
 val offbeat=KickPhaseEvidence.assess(beats,beats.map { it+0.15 },60.0/135)
 check(!offbeat.reliable)
 val jitter=KickPhaseEvidence.assess(beats,beats.mapIndexed { i,t->t+(if(i%2==0) 0.012 else -0.012) },60.0/135)
 check(jitter.reliable)
 val duplicateGrid=(0..39).map { it * 0.04 }
 val fewKicks=(0..7).map { it * 0.2 }
 val reused=KickPhaseEvidence.assess(duplicateGrid,fewKicks,0.4)
 check(!reused.reliable && reused.supportRatio <= 0.2)
 val invalid=KickPhaseEvidence.assess(beats.toMutableList().also { it[5]=Double.NaN },beats,60.0/135)
 check(!invalid.reliable)
 println("PASS phase evidence: clean, sparse, offbeat, jitter, unique matches, nonfinite beats")
}
KT
kotlinc app/src/main/java/com/danim/autowarp/analysis/KickPhaseEvidence.kt "$T/Test.kt" -include-runtime -d "$T/test.jar"
java -jar "$T/test.jar"
