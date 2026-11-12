#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.TransientFidelity
import kotlin.math.*
fun main() {
 val rate=8000; val src=FloatArray(rate); val out=FloatArray(rate)
 for (i in 0..31) { src[4000+i]=(exp(-i/7.0)).toFloat(); out[4040+i]=(0.5*exp(-i/7.0)).toFloat() }
 val m=TransientFidelity.measure(src,out,rate,0.5,0.5)
 check(abs(m.timingErrorMs-5.0)<0.01) { "$m" }
 check(abs(m.energyRatio-0.5)<0.001) { "$m" }
 check(abs(m.sourcePeak-1.0)<1e-6 && abs(m.outputPeak-0.5)<1e-6)
 val aligned=TransientFidelity.measure(src,out,rate,0.5,0.505)
 check(abs(aligned.timingErrorMs)<0.01) { "$aligned" }
 check(runCatching { TransientFidelity.measure(src,out,rate,-1.0,0.5) }.isFailure)
 println("PASS: transient fidelity timing, energy, target alignment, invalid bounds")
}
KT
kotlinc "$ROOT/app/src/main/java/com/danim/autowarp/audio/TransientFidelity.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
