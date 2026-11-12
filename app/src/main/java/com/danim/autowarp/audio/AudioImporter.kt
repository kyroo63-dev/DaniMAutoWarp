package com.danim.autowarp.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.min

/** Imports PCM WAV/AIFF directly and compressed formats (including MP3) through Android MediaCodec. */
object AudioImporter {
    fun read(context: Context, uri: Uri, mime: String?): PcmAudio {
        val type=(mime ?: "").lowercase()
        val name=(uri.lastPathSegment ?: "").lowercase()
        return when {
            type.contains("aiff") || type.contains("aif") || name.endsWith(".aiff") || name.endsWith(".aif") -> context.contentResolver.openInputStream(uri)!!.use { AiffPcm.read(it) }
            type.contains("wav") || type.contains("wave") || name.endsWith(".wav") -> context.contentResolver.openInputStream(uri)!!.use { PcmWave.read(it) }
            else -> decodeWithMediaCodec(context,uri)
        }
    }

    private fun decodeWithMediaCodec(context:Context, uri:Uri):PcmAudio {
        val extractor=MediaExtractor(); extractor.setDataSource(context,uri,null)
        var track=-1; var format:MediaFormat?=null
        for(i in 0 until extractor.trackCount) {
            val f=extractor.getTrackFormat(i); val mime=f.getString(MediaFormat.KEY_MIME) ?: continue
            if(mime.startsWith("audio/")) { track=i; format=f; break }
        }
        val selectedFormat=format
        require(track>=0 && selectedFormat!=null) { "No se encontró una pista de audio compatible" }
        extractor.selectTrack(track)
        val mime=selectedFormat.getString(MediaFormat.KEY_MIME)!!
        val codec=MediaCodec.createDecoderByType(mime)
        val chunks=ArrayList<FloatArray>(); var total=0; var outRate=selectedFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE); var outChannels=selectedFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT); var pcmEncoding=AudioFormat.ENCODING_PCM_16BIT
        codec.configure(selectedFormat,null,null,0); codec.start()
        val info=MediaCodec.BufferInfo(); var inputDone=false; var outputDone=false
        try {
            while(!outputDone) {
                if(!inputDone) {
                    val index=codec.dequeueInputBuffer(10_000)
                    if(index>=0) {
                        val buffer=codec.getInputBuffer(index)!!; buffer.clear()
                        val size=extractor.readSampleData(buffer,0)
                        if(size<0) { codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone=true }
                        else { codec.queueInputBuffer(index,0,size,extractor.sampleTime,0); extractor.advance() }
                    }
                }
                when(val index=codec.dequeueOutputBuffer(info,10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val of=codec.outputFormat
                        outRate=of.getInteger(MediaFormat.KEY_SAMPLE_RATE); outChannels=of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        if(of.containsKey(MediaFormat.KEY_PCM_ENCODING)) pcmEncoding=of.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if(index>=0) {
                        val buffer=codec.getOutputBuffer(index)!!
                        if(info.size>0) {
                            buffer.position(info.offset); buffer.limit(info.offset+info.size)
                            val view=buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
                            val count=when(pcmEncoding) { AudioFormat.ENCODING_PCM_FLOAT -> info.size/4; AudioFormat.ENCODING_PCM_8BIT -> info.size; AudioFormat.ENCODING_PCM_24BIT_PACKED -> info.size/3; AudioFormat.ENCODING_PCM_32BIT -> info.size/4; else -> info.size/2 }
                            val data=FloatArray(count)
                            for(i in 0 until count) data[i]=when(pcmEncoding) {
                                AudioFormat.ENCODING_PCM_FLOAT -> view.float.coerceIn(-1f,0.999999f)
                                AudioFormat.ENCODING_PCM_8BIT -> ((view.get().toInt() and 255)-128)/128f
                                AudioFormat.ENCODING_PCM_24BIT_PACKED -> { val a=view.get().toInt() and 255; val b=view.get().toInt() and 255; val c=view.get().toInt(); var v=a or (b shl 8) or (c shl 16); if(v and 0x800000!=0) v=v or -0x1000000; v/8388608f }
                                AudioFormat.ENCODING_PCM_32BIT -> view.int.toDouble().div(2147483648.0).toFloat()
                                else -> (view.short.toInt()/32768f)
                            }
                            chunks.add(data); total+=data.size
                        }
                        outputDone=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                        codec.releaseOutputBuffer(index,false)
                    }
                }
            }
        } finally { try { codec.stop() } catch(_:Exception){}; codec.release(); extractor.release() }
        require(outChannels in 1..2 && outRate>0 && total>0) { "El códec devolvió audio vacío o con más de dos canales" }
        val samples=FloatArray(total); var at=0; chunks.forEach { it.copyInto(samples,at); at+=it.size }
        return PcmAudio(outRate,outChannels,samples)
    }
}
