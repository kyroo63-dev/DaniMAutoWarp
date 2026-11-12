#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickTempoWindowAudit
fun main() {
 fun checkCase(name: String, condition: Boolean) { check(condition) { name }; println("PASS $name") }
 fun pulse(period: Double, start: Double, n: Int) = (0 until n).map { start + it * period }
 val steady = KickTempoWindowAudit.inspect(pulse(0.5,0.0,32),120.0,16.0)
 checkCase("steady windows",steady.supportedWindows==4 && steady.stable)
 checkCase("zero drift",steady.largestAdjacentDrift<1e-8)
 val sparse=KickTempoWindowAudit.inspect(listOf(0.1,1.1,2.1),120.0,12.0)
 checkCase("sparse inconclusive",!sparse.sufficientEvidence)
 val gap=KickTempoWindowAudit.inspect(pulse(0.5,0.0,8)+pulse(0.5,8.0,8),120.0,12.0)
 checkCase("gap no fabricated adjacent comparison",gap.adjacentComparisons==0)
 val drifting=KickTempoWindowAudit.inspect(pulse(0.5,0.0,8)+pulse(0.54,4.0,8),120.0,8.0)
 checkCase("drift detected",drifting.sufficientEvidence && !drifting.stable)
 checkCase("duplicates harmless",KickTempoWindowAudit.inspect(pulse(0.5,0.0,16)+listOf(0.0,0.5),120.0,8.0).stable)
 checkCase("unsorted harmless",KickTempoWindowAudit.inspect(pulse(0.5,0.0,16).reversed(),120.0,8.0).stable)
 checkCase("short tail inconclusive",KickTempoWindowAudit.inspect(pulse(0.5,0.0,16),120.0,10.0).supportedWindows==2)
 checkCase("empty inconclusive",!KickTempoWindowAudit.inspect(emptyList(),120.0,8.0).stable)
 checkCase("invalid time rejected",runCatching { KickTempoWindowAudit.inspect(listOf(-1.0),120.0,8.0) }.isFailure)
 checkCase("invalid BPM rejected",runCatching { KickTempoWindowAudit.inspect(emptyList(),0.0,8.0) }.isFailure)
 checkCase("invalid window rejected",runCatching { KickTempoWindowAudit.inspect(emptyList(),120.0,8.0,0.0) }.isFailure)
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickTempoWindowAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
