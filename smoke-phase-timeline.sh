#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
cat > "$tmp/Test.kt" <<'KT'
import com.danim.autowarp.analysis.*
fun main() {
 val windows = listOf(
  KickPhaseDrift.Window(0.0,12.0,20,0.9,10.0,0.0,true),
  KickPhaseDrift.Window(12.0,24.0,2,0.1,90.0,null,false),
  KickPhaseDrift.Window(24.0,36.0,21,0.9,12.0,4.0,true))
 val spans = PhaseTimeline.project(windows, 6.0, 30.0)
 check(spans.size==3 && spans[0].left==0f && spans[0].right==0.25f)
 check(!spans[1].reliable && spans[2].reliable && spans[2].right==1f)
 check(PhaseTimeline.project(windows, 30.0, 30.0).isEmpty())
 check(PhaseTimeline.project(windows, 40.0, 50.0).isEmpty())
 println("PASS phase timeline: clipping, unreliable zones, empty ranges")
}
KT
kotlinc app/src/main/java/com/danim/autowarp/analysis/KickPulseAudit.kt app/src/main/java/com/danim/autowarp/analysis/KickPhaseDrift.kt app/src/main/java/com/danim/autowarp/analysis/PhaseTimeline.kt "$tmp/Test.kt" -include-runtime -d "$tmp/test.jar"
java -jar "$tmp/test.jar"
