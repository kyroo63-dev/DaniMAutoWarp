#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KOTLIN'
import com.danim.autowarp.audio.*
import java.io.ByteArrayOutputStream
fun main() {
 val good = ByteArrayOutputStream().also { PcmWave.write(PcmAudio(44100,1,floatArrayOf(0f,0.5f)),it) }.toByteArray()
 check(PcmWave.read(good.inputStream()).frames == 2)
 fun reject(a:ByteArray) = check(runCatching { PcmWave.read(a.inputStream()) }.isFailure)
 reject(good.copyOf(good.size-1)) // truncated data
 reject(good.copyOf().also { it[4] = 0x7f }) // declared RIFF exceeds physical file
 reject(good.copyOf().also { it[40] = 0xff.toByte(); it[41] = 0xff.toByte(); it[42] = 0xff.toByte(); it[43] = 0x7f }) // oversize data
 reject(good.copyOf().also { it[40] = 0; it[41] = 0; it[42] = 0; it[43] = 0 }) // empty data
 reject(good.copyOf().also { it[40] = 3; it[41] = 0; it[42] = 0; it[43] = 0; it[4] = 39; it[44] = 0 }) // truncated sample inside data chunk
 val stereo = ByteArrayOutputStream().also { PcmWave.write(PcmAudio(44100,2,floatArrayOf(0f,0f)),it) }.toByteArray()
 reject(stereo.copyOf().also { it[40] = 2; it[41] = 0; it[42] = 0; it[43] = 0; it[4] = 38 }) // incomplete stereo frame
 reject(good.copyOf().also { it[32] = 0 }) // inconsistent block alignment
 reject(good.copyOf().also { it[28] = 0; it[29] = 0; it[30] = 0; it[31] = 0 }) // inconsistent byte rate
 // Reject duplicate fmt and data chunks instead of silently selecting the last.
 fun extraChunk(id:String, payload:ByteArray):ByteArray {
   val chunk = id.toByteArray(Charsets.US_ASCII) + byteArrayOf(payload.size.toByte(),0,0,0) + payload + (if(payload.size%2==1) byteArrayOf(0) else byteArrayOf())
   val out = good.copyOfRange(0,good.size) + chunk
   val riffSize = out.size-8
   for(i in 0..3) out[4+i]=(riffSize ushr (8*i)).toByte()
   return out
 }
 reject(extraChunk("data", byteArrayOf(0,0)))
 reject(extraChunk("fmt ", good.copyOfRange(20,36)))
 // Extensible PCM with a foreign GUID must be rejected.
 val fmt=good.copyOfRange(20,36)
 val ext=fmt.copyOf(40)
 ext[0]=-2; ext[1]=-1 // WAVE_FORMAT_EXTENSIBLE
 ext[16]=22; ext[17]=0 // cbSize
 ext[18]=16; ext[19]=0 // validBits
 ext[24]=1 // PCM subtype
 // Deliberately leave the remaining GUID bytes invalid.
 val data=good.copyOfRange(44,good.size)
 val extWav=byteArrayOf(82,73,70,70,0,0,0,0,87,65,86,69) +
   "fmt ".toByteArray() + byteArrayOf(40,0,0,0) + ext +
   "data".toByteArray() + byteArrayOf(data.size.toByte(),0,0,0) + data
 val n=extWav.size-8
 for(i in 0..3) extWav[4+i]=(n ushr (8*i)).toByte()
 reject(extWav)
 println("PASS: WAV valid + 11 malformed RIFF cases")
}
KOTLIN
kotlinc app/src/main/java/com/danim/autowarp/audio/PcmWave.kt "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
