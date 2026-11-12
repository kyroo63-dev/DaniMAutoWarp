package com.danim.autowarp.audio

import com.danim.autowarp.analysis.WarpMarker
import java.io.OutputStream
import kotlin.math.roundToInt

/** Random-access, interleaved float PCM source. Implementations should only allocate the requested window. */
interface PcmFrameSource {
    val sampleRate: Int
    val channels: Int
    val frameCount: Long
    fun readFrames(firstFrame: Long, count: Int): FloatArray
}

data class StreamingWarpReport(
    val outputFrames: Long,
    val blocksWritten: Long,
    val peak: Float,
    val clippedSamples: Long,
    val maxSourceFramesBuffered: Int,
    /** This first bounded-memory renderer uses high-quality cubic resampling; it is not pitch-preserving. */
    val pitchPreserving: Boolean = false
)

/**
 * Bounded-memory, monotonic marker renderer. It reads only the source window needed for each
 * output block and writes PCM16 WAV incrementally. Cubic interpolation avoids the harshness of
 * linear interpolation, but changes pitch with local speed; use the in-memory WSOLA/phase-vocoder
 * engines when pitch preservation is required. This is an explicit low-memory fallback, not a
 * replacement for a streaming WSOLA/phase-vocoder engine.
 */
object StreamingWarpProcessor {
    fun renderWav(source: PcmFrameSource, markers: List<WarpMarker>, output: OutputStream,
                  blockFrames: Int = 16_384): StreamingWarpReport {
        require(blockFrames in 256..131_072)
        require(source.sampleRate > 0 && source.channels in 1..2 && source.frameCount > 1)
        val m = markers.sortedBy { it.targetSeconds }
        require(m.size >= 2 && m.zipWithNext().all { (a,b) ->
            a.sourceSeconds.isFinite() && b.sourceSeconds.isFinite() && a.targetSeconds.isFinite() && b.targetSeconds.isFinite() &&
                b.sourceSeconds > a.sourceSeconds && b.targetSeconds > a.targetSeconds &&
                a.sourceSeconds >= 0.0 && b.sourceSeconds <= source.frameCount.toDouble()/source.sampleRate
        }) { "Marcadores inválidos o no monótonos" }
        require(m.first().sourceSeconds <= 1.0/source.sampleRate && m.first().targetSeconds <= 1.0/source.sampleRate) {
            "El primer marcador debe estar en el inicio"
        }
        val outFrames = (m.last().targetSeconds * source.sampleRate).roundToInt().toLong().coerceAtLeast(1)
        val dataBytes = outFrames * source.channels * 2L
        require(dataBytes <= 0xFFFF_FFFFL - 36L) { "Salida superior al límite WAV RIFF" }
        writeHeader(output, source.sampleRate, source.channels, dataBytes.toInt())

        fun sourcePosition(targetFrame: Long): Double {
            val t = targetFrame.toDouble()/source.sampleRate
            var lo=0; var hi=m.lastIndex-1
            while(lo<hi) { val mid=(lo+hi) ushr 1; if(t>m[mid+1].targetSeconds) lo=mid+1 else hi=mid }
            val a=m[lo]; val b=m[lo+1]
            val f=((t-a.targetSeconds)/(b.targetSeconds-a.targetSeconds)).coerceIn(0.0,1.0)
            val warped=a.sourceSeconds+(b.sourceSeconds-a.sourceSeconds)*f
            val strength=(a.strength+(b.strength-a.strength)*f).toDouble().coerceIn(0.0,1.0)
            val identity=t.coerceIn(0.0, source.frameCount.toDouble()/source.sampleRate)
            return ((identity*(1.0-strength)+warped*strength)*source.sampleRate).coerceIn(0.0,source.frameCount-1.001)
        }

        val pcmBytes=ByteArray(blockFrames*source.channels*2)
        var written=0L; var blocks=0L; var peak=0f; var clipped=0L; var maxBuffered=0
        while(written<outFrames) {
            val count=minOf(blockFrames.toLong(),outFrames-written).toInt()
            val firstPos=sourcePosition(written)
            val lastPos=sourcePosition(written+count-1L)
            val srcFirst=(kotlin.math.floor(firstPos).toLong()-1).coerceAtLeast(0)
            val srcLast=(kotlin.math.ceil(lastPos).toLong()+3).coerceAtMost(source.frameCount)
            val window=source.readFrames(srcFirst,(srcLast-srcFirst).toInt())
            require(window.size==(srcLast-srcFirst).toInt()*source.channels) { "La fuente devolvió una ventana incompleta" }
            maxBuffered=maxOf(maxBuffered,(srcLast-srcFirst).toInt())
            var p=0
            for(i in 0 until count) {
                val pos=sourcePosition(written+i)
                val base=kotlin.math.floor(pos).toLong()
                val frac=(pos-base).toFloat()
                val local=(base-srcFirst).toInt()
                for(c in 0 until source.channels) {
                    fun sample(frameOffset:Int):Float {
                        val ix=(local+frameOffset).coerceIn(0,(window.size/source.channels)-1)
                        return window[ix*source.channels+c]
                    }
                    val y0=sample(-1); val y1=sample(0); val y2=sample(1); val y3=sample(2)
                    val a= -0.5f*y0+1.5f*y1-1.5f*y2+0.5f*y3
                    val b= y0-2.5f*y1+2f*y2-0.5f*y3
                    val cc= -0.5f*y0+0.5f*y2
                    val value=((a*frac+b)*frac+cc)*frac+y1
                    peak=maxOf(peak,kotlin.math.abs(value))
                    if(value>0.999969f || value< -1f) clipped++
                    val q=(value.coerceIn(-1f,0.999969f)*32768f).roundToInt()
                    pcmBytes[p++]=(q and 255).toByte(); pcmBytes[p++]=((q ushr 8) and 255).toByte()
                }
            }
            output.write(pcmBytes,0,p); written+=count; blocks++
        }
        output.flush()
        return StreamingWarpReport(written,blocks,peak,clipped,maxBuffered)
    }

    private fun writeHeader(out:OutputStream,rate:Int,channels:Int,dataBytes:Int) {
        fun p16(v:Int){out.write(v and 255);out.write((v ushr 8) and 255)}
        fun p32(v:Int){p16(v and 65535);p16(v ushr 16)}
        out.write("RIFF".toByteArray());p32(36+dataBytes);out.write("WAVEfmt ".toByteArray());p32(16)
        p16(1);p16(channels);p32(rate);p32(rate*channels*2);p16(channels*2);p16(16)
        out.write("data".toByteArray());p32(dataBytes)
    }
}

/** File-backed random-access reader for uncompressed PCM16 RIFF/WAVE. Close it with `close()`. */
class Pcm16WavFileSource(private val file: java.io.File) : PcmFrameSource, java.io.Closeable {
    private val raf = java.io.RandomAccessFile(file, "r")
    private val dataOffset: Long
    override val sampleRate: Int
    override val channels: Int
    override val frameCount: Long
    init {
        require(raf.length() >= 44)
        fun readAscii(n:Int)=ByteArray(n).also { raf.readFully(it) }.toString(Charsets.US_ASCII)
        fun readU32():Long = (raf.readUnsignedByte().toLong()) or (raf.readUnsignedByte().toLong() shl 8) or
            (raf.readUnsignedByte().toLong() shl 16) or (raf.readUnsignedByte().toLong() shl 24)
        raf.seek(0); require(readAscii(4)=="RIFF"); readU32(); require(readAscii(4)=="WAVE")
        var fmtFound=false; var dataFound=false; var rate=0; var ch=0; var bits=0; var offset=0L; var bytes=0L
        while(raf.filePointer+8<=raf.length() && !(fmtFound && dataFound)) {
            val id=readAscii(4); val size=readU32(); val start=raf.filePointer
            when(id) {
                "fmt " -> { require(size>=16); val format=raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8); ch=raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8); rate=readU32().toInt(); readU32(); raf.skipBytes(2); bits=raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8); require(format==1 && bits==16 && ch in 1..2 && rate>0) { "Streaming WAV admite PCM16 mono/estéreo" }; fmtFound=true }
                "data" -> { offset=start; bytes=size; dataFound=true }
            }
            val next=start+size+(size and 1L); if(next>raf.length()) break; raf.seek(next)
        }
        require(fmtFound && dataFound && bytes>0) { "WAV sin cabecera fmt/data válida" }
        sampleRate=rate; channels=ch; dataOffset=offset; frameCount=bytes/(channels*2L)
    }
    @Synchronized override fun readFrames(firstFrame:Long,count:Int):FloatArray {
        require(firstFrame>=0 && count>=0 && firstFrame+count<=frameCount)
        val out=FloatArray(count*channels); val bytes=ByteArray(out.size*2)
        raf.seek(dataOffset+firstFrame*channels*2L); raf.readFully(bytes)
        for(i in out.indices) { val v=(bytes[i*2].toInt() and 255) or (bytes[i*2+1].toInt() shl 8); out[i]=v.toShort().toFloat()/32768f }
        return out
    }
    override fun close() = raf.close()
}
