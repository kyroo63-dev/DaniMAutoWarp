#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KOTLIN'
import com.danim.autowarp.analysis.*
fun main() {
 val bpm=136.0
 fun w(i:Int, phase:Double?, reliable:Boolean=true)=KickPhaseDrift.Window(i*12.0,(i+1)*12.0,27,0.8,10.0,phase,reliable)
 val stable=KickPhaseSegments.inspect(listOf(w(0,10.0),w(1,15.0),w(2,20.0)),bpm)
 check(stable.segments.size==1 && stable.segments[0].windowCount==3 && !stable.eligibleForAutomaticWarp)
 val gap=KickPhaseSegments.inspect(listOf(w(0,10.0),w(1,null,false),w(2,15.0)),bpm)
 check(gap.segments.size==2 && gap.rejectedWindows==1)
 val jump=KickPhaseSegments.inspect(listOf(w(0,10.0),w(1,90.0),w(2,95.0)),bpm)
 check(jump.segments.size==2 && jump.discontinuities==1)
 val empty=KickPhaseSegments.inspect(emptyList(),bpm)
 check(empty.segments.isEmpty() && !empty.eligibleForAutomaticWarp)
 println("PASS: contiguous phase segments, gaps, 80ms jumps, empty input; no automatic warp authorization")
}
KOTLIN
kotlinc app/src/main/java/com/danim/autowarp/analysis/KickPulseAudit.kt app/src/main/java/com/danim/autowarp/analysis/KickPhaseDrift.kt app/src/main/java/com/danim/autowarp/analysis/KickPhaseSegments.kt "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
