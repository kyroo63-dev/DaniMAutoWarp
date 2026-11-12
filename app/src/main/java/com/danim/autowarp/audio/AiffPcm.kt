package com.danim.autowarp.audio

import java.io.InputStream
import kotlin.math.pow

/** AIFF (uncompressed PCM, big-endian) reader. AIFC/compressed AIFF is deliberately rejected. */
object AiffPcm {
    fun read(input: InputStream): PcmAudio {
        val b=input.readBytes()
        fun id(p:Int)=String(b,p,4,Charsets.US_ASCII)
        fun u16(p:Int)=((b[p].toInt() and 255) shl 8) or (b[p+1].toInt() and 255)
        fun u32(p:Int)=((b[p].toLong() and 255) shl 24) or ((b[p+1].toLong() and 255) shl 16) or ((b[p+2].toLong() and 255) shl 8) or (b[p+3].toLong() and 255)
        require(b.size>=54 && id(0)=="FORM" && id(8)=="AIFF") { "No es AIFF PCM válido (AIFC no compatible)" }
        var p=12; var channels=0; var frames=0; var bits=0; var rate=0; var data=-1; var dataSize=0
        while(p+8<=b.size) {
            val tag=id(p); val len=u32(p+4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(); val s=p+8
            if(s+len>b.size) break
            if(tag=="COMM" && len>=18) {
                channels=u16(s); frames=u32(s+2).toInt(); bits=u16(s+6)
                val exp=u16(s+8); var mant=0.0; for(i in 0 until 8) mant=mant*256.0+(b[s+10+i].toInt() and 255)
                if(exp!=0 && exp!=0x7fff) rate=(mant*2.0.pow((exp-16383-63).toDouble())).toInt()
            }
            if(tag=="SSND" && len>=8) { val offset=u32(s).toInt(); data=s+8+offset; dataSize=len-8-offset }
            p=s+len+(len and 1)
        }
        require(channels in 1..2 && frames>0 && rate>0 && bits in setOf(8,16,24,32) && data>=0 && data+dataSize<=b.size) { "AIFF admitido: PCM 8/16/24/32 bits, mono o estéreo" }
        val bytesPer=(bits+7)/8; val count=minOf(frames*channels,dataSize/bytesPer); val aligned=count-(count%channels); val out=FloatArray(aligned)
        for(i in 0 until aligned) {
            val q=data+i*bytesPer
            var v=0L; for(j in 0 until bytesPer) v=(v shl 8) or (b[q+j].toLong() and 255)
            val shift=64-bits; val signed=(v shl shift) shr shift
            out[i]=(signed.toDouble()/2.0.pow((bits-1).toDouble())).toFloat()
        }
        return PcmAudio(rate,channels,out)
    }
}
