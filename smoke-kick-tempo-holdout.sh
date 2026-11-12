#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.analysis.KickTempoHoldoutAudit as A
fun check(label:String, times:List<Double>, expectedEnough:Boolean, expectedConsistent:Boolean, bpm:Double=120.0) {
 val r=A.inspect(times,bpm)
 require(r.sufficientEvidence==expectedEnough) { "$label sufficient: $r" }
 require(r.consistent==expectedConsistent) { "$label consistent: $r" }
 println("PASS $label early=${r.earlyPairs} late=${r.latePairs} drift=${r.relativeDrift}")
}
fun main() {
 check("steady", (0..19).map { it*.5 },true,true)
 check("small_jitter", (0..19).map { it*.5 + if(it%2==0) .002 else -.002 },true,true)
 check("drift", (0..9).map { it*.5 }+(0..9).map { 5.0+it*.525 },true,false)
 check("few", listOf(.0,.5,1.0,1.5),false,false)
 check("empty", emptyList(),false,false)
 check("half_time", (0..19).map { it*1.0 },false,false)
 check("duplicates", (0..19).flatMap { listOf(it*.5,it*.5) },true,true)
 check("irregular", (0..19).map { it*.3 },false,false)
 check("unsorted", (0..19).map { it*.5 }.reversed(),true,true)
 check("one_half_sparse", (0..9).map { it*.5 }+(0..9).map { 5.0+it*1.0 },false,false)
 try { A.inspect(listOf(0.0,Double.NaN),120.0); error("NaN accepted") } catch (_:IllegalArgumentException) { println("PASS invalid_time") }
 try { A.inspect(listOf(0.0),0.0); error("invalid BPM accepted") } catch (_:IllegalArgumentException) { println("PASS invalid_bpm") }
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/analysis/KickTempoHoldoutAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
