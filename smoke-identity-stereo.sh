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
  val duration=src.frameCount.toDouble()/src.sampleRate
  val markers=listOf(WarpMarker(0.0,0.0),WarpMarker(duration,duration))
  val transient=listOf(TransientEvent(0.5,0.99),TransientEvent(1.5,0.99))
  val originalSamples=src.readFrames(0,src.frameCount.toInt())
  val original=ByteArray(originalSamples.size*2)
  originalSamples.forEachIndexed { i, v ->
   val q=(v.coerceIn(-1f,0.999969f)*32768f).roundToInt()
   original[2*i]=(q and 255).toByte();original[2*i+1]=((q ushr 8) and 255).toByte()
  }
  for(mode in TransientAnchorMode.entries) {
   val output=ByteArrayOutputStream()
   val r=StreamingWsolaProcessor.renderWav(src,markers,output,WarpQuality.BALANCED,transient,anchorMode=mode)
   check(output.toByteArray().copyOfRange(44,output.size()).contentEquals(original)) { "Identity stereo not bit-exact: $mode" }
   check(r.grainsProcessed==0L && r.maxSourceFramesBuffered<=4096)
   println("PASS identity stereo $mode frames=${r.outputFrames} exactBytes=${output.size()}")
  }
  val preview=ByteArrayOutputStream()
  val r=StreamingWsolaProcessor.renderWav(src,markers,preview,previewStartSeconds=0.5,previewDurationSeconds=1.0)
  check(r.outputFrames==44100L)
  check(preview.size()==44+44100*4)
  check(preview.toByteArray().copyOfRange(44,preview.size()).contentEquals(original.copyOfRange(22050*4,66150*4)))
  println("PASS identity preview stereo exact PCM")
  val changed=ByteArrayOutputStream()
  val w=StreamingWsolaProcessor.renderWav(src,listOf(WarpMarker(0.0,0.0),WarpMarker(duration,duration*1.01)),changed,WarpQuality.FAST)
  check(w.grainsProcessed>0L && changed.size().toLong()==44L+w.outputFrames*4L)
  println("PASS nonidentity stereo WSOLA frames=${w.outputFrames}")
 }
}
KT
kotlinc "$TMP/Stubs.kt" "$TMP/Quality.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWsolaProcessor.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar" "$TMP/source.wav"
