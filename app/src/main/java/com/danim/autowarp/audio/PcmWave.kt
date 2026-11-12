package com.danim.autowarp.audio

import java.io.InputStream
import java.io.OutputStream
import kotlin.math.roundToInt

/** Interleaved floating-point PCM. Samples are normalized to [-1, 1). */
data class PcmAudio(val sampleRate: Int, val channels: Int, val samples: FloatArray) {
    init { require(sampleRate > 0 && channels in 1..2 && samples.size % channels == 0) }
    val frames: Int get() = samples.size / channels
    val durationSeconds: Double get() = frames.toDouble() / sampleRate
}

object PcmWave {
    fun read(input: InputStream): PcmAudio {
        val b = input.readBytes()
        require(b.size >= 44 && ascii(b,0,4)=="RIFF" && ascii(b,8,4)=="WAVE") { "No es un WAV RIFF válido" }
        fun u16(p:Int)=(b[p].toInt() and 255) or ((b[p+1].toInt() and 255) shl 8)
        fun i32(p:Int)=(b[p].toInt() and 255) or ((b[p+1].toInt() and 255) shl 8) or ((b[p+2].toInt() and 255) shl 16) or (b[p+3].toInt() shl 24)
        val riffSize = i32(4).toLong() and 0xFFFF_FFFFL
        require(riffSize >= 36 && riffSize + 8L <= b.size.toLong()) { "Cabecera RIFF truncada" }
        val riffEnd = riffSize + 8L
        var p=12; var format=0; var channels=0; var rate=0; var bits=0; var start=-1; var size=0; var blockAlign=0; var byteRate=0; var fmtSeen=false; var dataSeen=false
        while(p.toLong()+8L<=riffEnd) {
            val id=ascii(b,p,4); val chunk=i32(p+4).toLong() and 0xFFFF_FFFFL; val s=p+8
            if(s.toLong()>riffEnd) break
            if(id=="fmt ") {
                require(!fmtSeen) { "WAV contiene varios bloques fmt" }
                fmtSeen=true
                require(chunk>=16 && s.toLong()+chunk<=riffEnd) { "Bloque WAV fmt truncado" }
                format=u16(s); channels=u16(s+2); rate=i32(s+4)
                byteRate=i32(s+8); blockAlign=u16(s+12); bits=u16(s+14)
                if(format==0xFFFE) {
                    require(chunk>=40 && u16(s+16)>=22) { "WAV extensible fmt incompleto" }
                    val validBits=u16(s+18)
                    require(validBits==0 || validBits==bits) { "WAV extensible validBits no compatible" }
                    // KSDATAFORMAT_SUBTYPE_PCM / IEEE_FLOAT have this exact GUID suffix.
                    val guidTail=byteArrayOf(0,0,0,0,16,0,-128,0,0,-86,0,56,-101,113)
                    require((0 until 14).all { b[s+26+it]==guidTail[it] }) { "Subformato WAV extensible desconocido" }
                    require(u16(s+24)==1 || u16(s+24)==3) { "Subformato WAV extensible no compatible" }
                    format=u16(s+24)
                }
            }
            if(id=="data") {
                require(!dataSeen) { "WAV contiene varios bloques data" }
                dataSeen=true
                require(s.toLong() + chunk <= riffEnd) { "Bloque WAV data truncado" }
                require(chunk <= Int.MAX_VALUE.toLong()) { "Bloque WAV demasiado grande" }
                start=s; size=chunk.toInt()
            }
            val chunkEnd = s.toLong() + chunk
            require(chunkEnd <= riffEnd) { "Bloque WAV truncado" }
            val next = chunkEnd + (chunk and 1L)
            require(next <= riffEnd) { "Falta byte de relleno de bloque WAV impar" }
            p = next.toInt()
        }
        require(fmtSeen && start >= 0 && size > 0) { "No se encontró un bloque WAV data válido" }
        require(format==1 || format==3) { "WAV admitido: PCM entero o IEEE float" }
        require(channels in 1..2 && rate>0 && start>=0 && bits in setOf(8,16,24,32)) { "Formato WAV no compatible" }
        require(format!=3 || bits==32) { "WAV float debe ser de 32 bits" }
        require(blockAlign == channels * ((bits + 7) / 8)) { "WAV blockAlign no coincide con el formato PCM" }
        require(byteRate.toLong() == rate.toLong() * blockAlign.toLong()) { "WAV byteRate no coincide con el formato PCM" }
        val bytesPer=(bits+7)/8
        val frameBytes = bytesPer * channels
        require(size % frameBytes == 0) { "WAV data contiene una trama PCM incompleta" }
        val count = size / bytesPer
        val out=FloatArray(count)
        for(i in 0 until count) {
            val q=start+i*bytesPer
            out[i]=when {
                format==3 -> java.lang.Float.intBitsToFloat(i32(q)).takeIf { it.isFinite() }?.coerceIn(-1f,0.999999f) ?: 0f
                bits==8 -> ((b[q].toInt() and 255)-128)/128f
                bits==16 -> (((b[q].toInt() and 255) or (b[q+1].toInt() shl 8)).toShort()).toFloat()/32768f
                bits==24 -> { var v=(b[q].toInt() and 255) or ((b[q+1].toInt() and 255) shl 8) or ((b[q+2].toInt() and 255) shl 16); if(v and 0x800000!=0) v=v or -0x1000000; v/8388608f }
                else -> i32(q).toDouble().div(2147483648.0).toFloat()
            }
        }
        return PcmAudio(rate,channels,out)
    }
    /** Writes bounded-size PCM blocks; classic RIFF/WAV is limited to just under 4 GiB. */
    fun write(audio:PcmAudio, output:OutputStream) {
        val byteRateLong = audio.sampleRate.toLong() * audio.channels.toLong() * 2L
        require(byteRateLong <= 0xFFFF_FFFFL) { "Frecuencia de muestreo incompatible con WAV PCM16" }
        val dataBytesLong = audio.samples.size.toLong() * 2L
        require(dataBytesLong <= 0xFFFF_FFFFL - 36L) { "La pista supera el límite de WAV RIFF; exporta en segmentos" }
        val dataBytes = dataBytesLong.toInt()
        fun p16(v:Int){output.write(v and 255);output.write((v ushr 8) and 255)}
        fun p32(v:Int){p16(v and 65535);p16(v ushr 16)}
        output.write("RIFF".toByteArray());p32(36+dataBytes);output.write("WAVEfmt ".toByteArray());p32(16)
        p16(1);p16(audio.channels);p32(audio.sampleRate);p32(byteRateLong.toInt());p16(audio.channels*2);p16(16)
        output.write("data".toByteArray());p32(dataBytes)
        val blockSamples = 8192
        val bytes = ByteArray(blockSamples * 2)
        var offset = 0
        while (offset < audio.samples.size) {
            val count = minOf(blockSamples, audio.samples.size - offset)
            for (i in 0 until count) {
                val raw = audio.samples[offset + i]
                // Invalid floating-point samples must never become arbitrary PCM values.
                val safe = if (raw.isFinite()) raw.coerceIn(-1f, 0.9999695f) else 0f
                val value = (safe * 32768f).roundToInt().coerceIn(-32768, 32767)
                bytes[i * 2] = (value and 255).toByte()
                bytes[i * 2 + 1] = ((value ushr 8) and 255).toByte()
            }
            output.write(bytes, 0, count * 2)
            offset += count
        }
        output.flush()
    }
    private fun ascii(b:ByteArray,p:Int,n:Int)=String(b,p,n,Charsets.US_ASCII)
}
