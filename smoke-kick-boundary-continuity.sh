#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickBoundaryContinuityAudit as A
fun main() {
 var passed = 0
 fun test(name: String, ok: Boolean) { check(ok) { name }; passed++ }
 fun seg(start:Double,end:Double,bpm:Double=120.0,phase:Double=0.1,good:Boolean=true) = A.Segment(start,end,bpm,phase,good)
 val a=seg(0.0,24.0)
 test("aligned",A.inspect(a,seg(24.0,48.0)).continuous)
 test("local to absolute phase",A.inspect(seg(0.0,23.9),seg(23.9,47.9,phase=0.2)).continuous)
 test("phase discontinuity",!A.inspect(a,seg(24.0,48.0,phase=0.32)).continuous)
 test("tempo discontinuity",!A.inspect(a,seg(24.0,48.0,bpm=130.0)).continuous)
 test("unreliable left",!A.inspect(seg(0.0,24.0,good=false),seg(24.0,48.0)).continuous)
 test("unreliable right",!A.inspect(a,seg(24.0,48.0,good=false)).continuous)
 test("gap",!A.inspect(a,seg(24.5,48.5)).continuous)
 test("overlap",!A.inspect(a,seg(23.0,47.0)).continuous)
 test("rounding tolerance",A.inspect(a,seg(24.001,48.0)).contiguous)
 test("phase wrap",A.inspect(a,seg(24.0,48.0,phase=0.6)).continuous)
 test("tempo minor",A.inspect(a,seg(24.0,48.0,bpm=120.1)).continuous)
 test("invalid BPM",runCatching { A.inspect(a,seg(24.0,48.0,bpm=0.0)) }.isFailure)
 test("invalid time",runCatching { A.inspect(a,seg(24.0,24.0)) }.isFailure)
 println("PASS $passed boundary continuity tests")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickBoundaryContinuityAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
