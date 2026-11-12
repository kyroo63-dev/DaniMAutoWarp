#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
cat > "$T/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickPulseAudit
fun main() {
 val p=60.0/136.0
 val clean=(0..79).map { 0.13+it*p+(if(it%2==0) 0.008 else -0.008) }
 val result=KickPulseAudit.assess(clean,p)
 check(result.reliable && result.supportRatio>0.95 && result.medianErrorMs<20)
 val off=(0..79).map { it*0.283 + 0.05 }
 check(!KickPulseAudit.assess(off,p).reliable)
 check(!KickPulseAudit.assess(listOf(0.1,0.2),p).reliable)
 check(!KickPulseAudit.assess(clean+Double.NaN,p).reliable)
 println("PASS kick pulse audit: periodic phase, mismatched tempo, sparse and invalid attacks")
}
KT
kotlinc app/src/main/java/com/danim/autowarp/analysis/KickPulseAudit.kt "$T/Test.kt" -include-runtime -d "$T/test.jar"
java -jar "$T/test.jar"
