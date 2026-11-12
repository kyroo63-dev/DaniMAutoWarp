#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
fun report(frames:Long=44100,clips:Long=0,jumps:Long=0,rms:Double=0.25,rate:Int=44100) =
    StreamingRenderAuditReport(frames,rate,2,0.9,rms,0.0,clips,jumps,0.0,true)
fun main() {
    val baseline=report(clips=100,jumps=400)
    val same=RenderComparisonAudit.compare(baseline,baseline)
    check(same.compatible && same.warnings.isEmpty() && same.durationDeltaMs==0.0)
    val improved=RenderComparisonAudit.compare(baseline,report(clips=10,jumps=100))
    check(improved.addedClippedSamples==0L && improved.addedDiscontinuities==0L)
    val degraded=RenderComparisonAudit.compare(baseline,report(frames=45000,clips=110,jumps=450,rms=0.4))
    check(degraded.addedClippedSamples==10L && degraded.addedDiscontinuities==50L)
    check(degraded.warnings.size==4)
    check(!RenderComparisonAudit.compare(baseline,report(rate=48000)).compatible)
    println("PASS: comparación fuente/exportación, mejoras, regresiones, formato y duración")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingRenderAudit.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/RenderComparisonAudit.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
