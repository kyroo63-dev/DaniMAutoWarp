#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
INPUT="${1:-/mnt/data/Javi Golo & Andres Gomes - Expanden.flac}"
[[ -f "$INPUT" ]] || { echo "SKIP: recording not found"; exit 2; }
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
ffmpeg -nostdin -hide_banner -loglevel error -y -ss 75 -t 3 -i "$INPUT" -ac 2 -ar 44100 -c:a pcm_s16le "$TMP/source.wav"
cat > "$TMP/Stubs.kt" <<'KT'
package com.danim.autowarp.analysis
data class WarpMarker(val sourceSeconds:Double,val targetSeconds:Double,val strength:Float=1f)
data class TransientEvent(val seconds:Double,val confidence:Double)
KT
cat > "$TMP/Quality.kt" <<'KT'
package com.danim.autowarp.audio
enum class WarpQuality(val grainMs:Int,val searchMs:Int,val step:Int) { FAST(48,6,8),BALANCED(72,10,4),HIGH(96,14,2) }
data class PcmAudio(val sampleRate:Int,val channels:Int,val samples:FloatArray) { val frames get()=samples.size/channels }
KT
cat > "$TMP/Test.kt" <<'KT'
import com.danim.autowarp.audio.*
import com.danim.autowarp.analysis.*
import java.io.*
import kotlin.math.*
fun main(args:Array<String>) {
 Pcm16WavFileSource(File(args[0])).use { src ->
  check(src.sampleRate==44100 && src.channels==2)
  val original=src.readFrames(0,src.frameCount.toInt())
  val duration=src.frameCount.toDouble()/src.sampleRate
  val results=mutableListOf<String>()
  for(factor in listOf(0.99,1.01)) for(mode in TransientAnchorMode.entries) {
   val out=ByteArrayOutputStream()
   val report=StreamingWsolaProcessor.renderWav(src,listOf(WarpMarker(0.0,0.0),WarpMarker(duration,duration*factor)),out,WarpQuality.FAST,anchorMode=mode)
   val bytes=out.toByteArray()
   check(bytes.size.toLong()==44L+report.outputFrames*4L)
   check(report.grainsProcessed>0 && report.maxSourceFramesBuffered<src.sampleRate)
   val samples=FloatArray(report.outputFrames.toInt()*2) { i ->
    val p=44+i*2; (((bytes[p].toInt() and 255) or (bytes[p+1].toInt() shl 8)).toShort().toInt()/32768f)
   }
   check(samples.all { it.isFinite() && abs(it)<=1f })
   val hop=(src.sampleRate*WarpQuality.FAST.grainMs/1000).coerceIn(256,8192)/4
   val boundaries=mutableListOf<Double>(); val other=mutableListOf<Double>()
   for(frame in 1 until report.outputFrames.toInt()) {
    val d=max(abs(samples[2*frame]-samples[2*(frame-1)]),abs(samples[2*frame+1]-samples[2*(frame-1)+1])).toDouble()
    if(frame%hop==0) boundaries.add(d) else other.add(d)
   }
   val bMax=boundaries.maxOrNull()!!
   val bMean=boundaries.average()
   val oMean=other.average()
   val peak=samples.maxOf { abs(it) }
   check(bMax<0.95) { "Severe hop boundary jump $factor $mode: $bMax" }
   check(bMean<0.1) { "Elevated mean boundary jump $factor $mode: $bMean" }
   results.add("factor=$factor mode=$mode frames=${report.outputFrames} maxBoundary=${"%.5f".format(bMax)} meanBoundary=${"%.5f".format(bMean)} meanOther=${"%.5f".format(oMean)} peak=${"%.4f".format(peak)}")
  }
  results.forEach(::println)
  println("PASS: 6 stereo 44.1kHz small-warp renders; valid WAV, bounded buffer, finite PCM and boundary thresholds. Listening quality not verified.")
 }
}
KT
kotlinc "$TMP/Stubs.kt" "$TMP/Quality.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWsolaProcessor.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar" "$TMP/source.wav"
