package com.danim.autowarp.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class Pcm16SampleMathTest {
    @Test fun boundariesAndInvalidValues() {
        assertEquals(-1f, Pcm16SampleMath.normalized(Short.MIN_VALUE), 0f)
        assertEquals(Short.MAX_VALUE, Pcm16SampleMath.encoded(1f))
        assertEquals(Short.MIN_VALUE, Pcm16SampleMath.encoded(-1f))
        assertEquals(0.toShort(), Pcm16SampleMath.encoded(Float.NaN))
        assertEquals(0.toShort(), Pcm16SampleMath.encoded(Float.POSITIVE_INFINITY))
    }

    @Test fun stereoInterpolationIsChannelIndependent() {
        val stereo = shortArrayOf(-32768, 32767, 0, 0)
        assertEquals(-0.5f, Pcm16SampleMath.interpolated(stereo, 2, 0, 0.5), 0.00001f)
        assertEquals(32767f / 65536f, Pcm16SampleMath.interpolated(stereo, 2, 1, 0.5), 0.00001f)
    }
}
