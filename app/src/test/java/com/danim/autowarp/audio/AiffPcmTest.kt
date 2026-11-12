package com.danim.autowarp.audio

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AiffPcmTest {
    @Test fun readsBigEndian16BitAiff() {
        val b=ByteArrayOutputStream()
        fun p16(v:Int){b.write(v ushr 8 and 255);b.write(v and 255)}
        fun p32(v:Int){p16(v ushr 16);p16(v)}
        b.write("FORM".toByteArray());p32(50);b.write("AIFFCOMM".toByteArray());p32(18);p16(1);p32(2);p16(16)
        b.write(byteArrayOf(0x40,0x0e.toByte(),0xac.toByte(),0x44,0,0,0,0,0,0))
        b.write("SSND".toByteArray());p32(12);p32(0);p32(0);p16(0x4000);p16(0xc000)
        val a=AiffPcm.read(ByteArrayInputStream(b.toByteArray()))
        assertEquals(44100,a.sampleRate);assertEquals(1,a.channels);assertEquals(2,a.frames)
        assertEquals(0.5f,a.samples[0],0.0001f);assertEquals(-0.5f,a.samples[1],0.0001f)
    }
}
