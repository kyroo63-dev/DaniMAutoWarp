package com.danim.autowarp

import kotlin.math.abs
import kotlin.math.max

/** Strict RIFF/WAVE PCM16 decoder for preview peaks. No Android dependencies. */
object ReviewWav {
    private fun u16(b: ByteArray, i: Int): Int = (b[i].toInt() and 255) or ((b[i+1].toInt() and 255) shl 8)
    private fun u32(b: ByteArray, i: Int): Long = (u16(b,i).toLong() or (u16(b,i+2).toLong() shl 16))
    private fun tag(b: ByteArray, i: Int) = String(b, i, 4, Charsets.US_ASCII)

    fun peaks(bytes: ByteArray, bins: Int = 200): List<Float> {
        require(bins in 1..4096) { "Número de columnas no válido" }
        require(bytes.size >= 44 && tag(bytes,0)=="RIFF" && tag(bytes,8)=="WAVE") { "No es WAV RIFF" }
        require(u32(bytes,4) <= bytes.size.toLong()-8) { "RIFF truncado" }
        val end = (u32(bytes,4)+8).toInt()
        var pos = 12
        var channels = 0
        var blockAlign = 0
        var dataOffset = -1
        var dataLength = 0
        var fmtSeen = false
        while (pos <= end-8) {
            val len = u32(bytes,pos+4)
            require(len <= end.toLong()-pos-8) { "Chunk WAV truncado" }
            val next = pos.toLong()+8+len+(len and 1L)
            require(next <= end) { "Padding WAV truncado" }
            when(tag(bytes,pos)) {
                "fmt " -> {
                    require(len >= 16) { "fmt WAV incompleto" }
                    require(u16(bytes,pos+8)==1 && u16(bytes,pos+22)==16) { "Se requiere PCM16" }
                    channels = u16(bytes,pos+10)
                    blockAlign = u16(bytes,pos+20)
                    require(channels in 1..2 && blockAlign==channels*2) { "Canales WAV no compatibles" }
                    fmtSeen = true
                }
                "data" -> if(dataOffset<0) { dataOffset=pos+8; dataLength=len.toInt() }
            }
            pos=next.toInt()
        }
        require(fmtSeen && dataOffset>=0 && dataLength>=blockAlign && dataLength%blockAlign==0) { "Audio WAV incompleto" }
        val frames=dataLength/blockAlign
        return List(bins) { bin ->
            val start=bin.toLong()*frames/bins
            val stop=max(start+1,(bin.toLong()+1)*frames/bins).coerceAtMost(frames.toLong())
            var peak=0
            for(frame in start until stop) for(channel in 0 until channels) {
                val at=dataOffset+frame.toInt()*blockAlign+channel*2
                val sample=u16(bytes,at).toShort().toInt()
                peak=max(peak,abs(sample))
            }
            peak/32768f
        }
    }
}
