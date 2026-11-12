package com.danim.autowarp.audio

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PcmWaveTest {
    @Test fun pcm16WaveRoundTripPreservesShapeAndSamples() {
        val source=PcmAudio(44100,2,floatArrayOf(-1f,-0.5f,0f,0.5f,0.25f,0.75f))
        val bytes=ByteArrayOutputStream();PcmWave.write(source,bytes)
        val decoded=PcmWave.read(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(44100,decoded.sampleRate);assertEquals(2,decoded.channels);assertEquals(3,decoded.frames)
        assertEquals(-1f,decoded.samples[0],0.0001f);assertEquals(0.5f,decoded.samples[3],0.0001f)
    }

    @Test fun readsPcm24Wave() {
        val b=ByteArrayOutputStream()
        fun p16(v:Int){b.write(v and 255);b.write(v ushr 8 and 255)}
        fun p32(v:Int){p16(v and 65535);p16(v ushr 16)}
        b.write("RIFF".toByteArray());p32(39);b.write("WAVEfmt ".toByteArray());p32(16);p16(1);p16(1);p32(48000);p32(144000);p16(3);p16(24)
        b.write("data".toByteArray());p32(3);b.write(byteArrayOf(0,0,64))
        val audio=PcmWave.read(ByteArrayInputStream(b.toByteArray()))
        assertEquals(0.5f,audio.samples[0],0.00001f)
    }
}
