#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickOverlapEvidence
fun main() {
 val kick=KickOverlapEvidence.assess(.8,8.0,1.0,1.0,.85)
 val snare=KickOverlapEvidence.assess(.8,1.0,8.0,3.0,.25)
 val layered=KickOverlapEvidence.assess(.8,2.0,7.0,2.0,.75)
 check(!kick.competingAttack && kick.confidence==.8)
 check(snare.competingAttack && snare.confidence < kick.confidence)
 check(!layered.competingAttack && layered.confidence==.8)
 check(KickOverlapEvidence.assess(0.0,0.0,0.0,0.0,0.0).confidence==0.0)
 println("PASS: isolated kick, competing snare, layered kick, silence")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickOverlapEvidence.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
