package com.danim.autowarp.audio

import com.danim.autowarp.analysis.TransientEvent
import com.danim.autowarp.analysis.WarpMarker
import kotlin.math.*

/**
 * Transient-aware, stereo-linked phase vocoder.
 * - Identity phase locking keeps bins around spectral peaks coherent.
 * - Spectral-flux and supplied transient markers trigger phase resets.
 * - One shared mid-channel phase trajectory preserves inter-channel phase offsets.
 * - Marker strength blends corrected timing with the unwarped timeline (zone amount).
 * This is a reference implementation; it is not a commercial élastique replacement.
 */
object PhaseVocoderWarp {
    fun render(input: PcmAudio, markers: List<WarpMarker>, fftSize: Int = 2048,
               transients: List<TransientEvent> = emptyList()): PcmAudio {
        require(markers.size >= 2)
        require(fftSize >= 512 && fftSize and (fftSize - 1) == 0)
        val map = markers.sortedBy { it.targetSeconds }
        require(map.zipWithNext().all { (a,b) -> b.targetSeconds > a.targetSeconds && b.sourceSeconds > a.sourceSeconds })
        require(map.all { it.sourceSeconds in 0.0..input.durationSeconds && it.targetSeconds >= 0.0 })
        val anchoredEnd = map.last().sourceSeconds >= input.durationSeconds - 1.5 / input.sampleRate
        val duration = if (anchoredEnd) map.last().targetSeconds else map.last().targetSeconds +
            (map.last().targetSeconds - map[map.lastIndex-1].targetSeconds).coerceAtLeast(0.25)
        val outFrames = (duration * input.sampleRate).roundToInt().coerceAtLeast(1)
        val hop = fftSize / 4
        val half = fftSize / 2
        val window = DoubleArray(fftSize) { 0.5 - 0.5 * cos(2.0 * PI * it / (fftSize - 1)) }
        val output = FloatArray(outFrames * input.channels)
        val weights = DoubleArray(outFrames)
        val transientFrames = transients.filter { it.confidence >= 0.35 }
            .map { (it.seconds * input.sampleRate).roundToInt() }.toIntArray()
        var transientCursor = 0

        fun mapping(targetFrame: Int): Pair<Double, Double> {
            val t = targetFrame.toDouble() / input.sampleRate
            var lo=0; var hi=map.lastIndex-1
            while(lo<hi) { val mid=(lo+hi) ushr 1; if(t>map[mid+1].targetSeconds) lo=mid+1 else hi=mid }
            val a=map[lo]; val b=map[min(lo+1,map.lastIndex)]
            val f=((t-a.targetSeconds)/(b.targetSeconds-a.targetSeconds)).coerceIn(0.0,1.0)
            val mapped=a.sourceSeconds+(b.sourceSeconds-a.sourceSeconds)*f
            val strength=(a.strength+(b.strength-a.strength)*f).toDouble().coerceIn(0.0,1.0)
            return (t+(mapped-t)*strength)*input.sampleRate to strength
        }
        fun sample(channel: Int, position: Double): Double {
            val p=position.coerceIn(0.0,(input.frames-1).toDouble()); val i=p.toInt(); val f=p-i
            val a=input.samples[i*input.channels+channel].toDouble()
            val b=input.samples[min(i+1,input.frames-1)*input.channels+channel].toDouble()
            return a+(b-a)*f
        }
        fun spectrumAt(center: Double): Pair<DoubleArray,DoubleArray> {
            val re=DoubleArray(fftSize); val im=DoubleArray(fftSize)
            for(i in 0 until fftSize) {
                var mono=0.0
                for(c in 0 until input.channels) mono += sample(c,center+i-half)
                re[i]=(mono/input.channels)*window[i]
            }
            fft(re,im,false)
            return re to im
        }

        val previousMagnitude=DoubleArray(half+1)
        val previousReferencePhase=DoubleArray(half+1)
        val synthPhase=DoubleArray(half+1)
        var previousSource=Double.NaN
        var frameStart=0
        while(frameStart < outFrames) {
            val (center, _) = mapping(frameStart)
            val (refRe, refIm)=spectrumAt(center)
            val magnitude=DoubleArray(half+1) { k -> hypot(refRe[k],refIm[k]) }
            val phase=DoubleArray(half+1) { k -> atan2(refIm[k],refRe[k]) }
            var flux=0.0; var magSum=1e-12
            for(k in 2 until half) { flux += max(0.0,magnitude[k]-previousMagnitude[k]); magSum += previousMagnitude[k] }
            val spectralOnset = !previousSource.isNaN() && flux/magSum > 0.32
            val sourceHop=if(previousSource.isNaN()) hop.toDouble() else (center-previousSource).coerceAtLeast(1.0)
            while(transientCursor<transientFrames.size && transientFrames[transientCursor] < center-hop) transientCursor++
            val suppliedOnset = transientCursor<transientFrames.size && abs(transientFrames[transientCursor]-center)<=max(hop,fftSize/3)
            val resetPhase = previousSource.isNaN() || spectralOnset || suppliedOnset

            val peaks=ArrayList<Int>()
            for(k in 2 until half) if(magnitude[k]>=magnitude[k-1] && magnitude[k]>magnitude[k+1]) peaks.add(k)
            if(peaks.isEmpty()) peaks.add((1 until half).maxByOrNull { magnitude[it] } ?: 1)
            for(k in 0..half) {
                if(resetPhase) synthPhase[k]=phase[k]
                else {
                    val delta=wrapPhase(phase[k]-previousReferencePhase[k]-2.0*PI*k*sourceHop/fftSize)
                    synthPhase[k]+=2.0*PI*k*hop/fftSize + delta*hop/sourceHop
                }
            }
            // Identity phase locking: retain each bin's phase offset from its nearest peak.
            val lockedPhase=DoubleArray(half+1)
            for(k in 0..half) {
                val peak=peaks.minByOrNull { abs(it-k) } ?: k
                lockedPhase[k]=synthPhase[peak]+wrapPhase(phase[k]-phase[peak])
            }

            // Analyze each channel, then apply the same reference phase trajectory while
            // retaining that channel's instantaneous phase offset from the shared mid signal.
            for(channel in 0 until input.channels) {
                val re=DoubleArray(fftSize); val im=DoubleArray(fftSize)
                for(i in 0 until fftSize) re[i]=sample(channel,center+i-half)*window[i]
                fft(re,im,false)
                val channelPhase=DoubleArray(half+1) { k -> atan2(im[k],re[k]) }
                for(k in 0..half) {
                    val relative=wrapPhase(channelPhase[k]-phase[k])
                    val mag=hypot(re[k],im[k])
                    val p=lockedPhase[k]+relative
                    re[k]=mag*cos(p); im[k]=mag*sin(p)
                }
                for(k in 1 until half) { re[fftSize-k]=re[k]; im[fftSize-k]=-im[k] }
                fft(re,im,true)
                for(i in 0 until fftSize) {
                    val outIndex=frameStart+i-half
                    if(outIndex in 0 until outFrames) {
                        val w=window[i]
                        output[outIndex*input.channels+channel] += (re[i]*w).toFloat()
                        if(channel==0) weights[outIndex]+=w*w
                    }
                }
            }
            for(k in 0..half) { previousMagnitude[k]=magnitude[k]; previousReferencePhase[k]=phase[k] }
            previousSource=center
            frameStart+=hop
        }
        for(i in 0 until outFrames) {
            val norm=weights[i].coerceAtLeast(1e-8)
            for(c in 0 until input.channels) output[i*input.channels+c]=(output[i*input.channels+c].toDouble()/norm).coerceIn(-1.0,1.0).toFloat()
        }
        return PcmAudio(input.sampleRate,input.channels,output)
    }

    private fun wrapPhase(value: Double): Double {
        var x=value
        while(x>PI) x-=2.0*PI
        while(x< -PI) x+=2.0*PI
        return x
    }
    private fun fft(real: DoubleArray, imag: DoubleArray, inverse: Boolean) {
        val n=real.size; var j=0
        for(i in 1 until n) { var bit=n shr 1; while(j and bit != 0) { j=j xor bit; bit=bit shr 1 }; j=j xor bit
            if(i<j) { val r=real[i]; real[i]=real[j]; real[j]=r; val q=imag[i]; imag[i]=imag[j]; imag[j]=q } }
        var len=2
        while(len<=n) { val angle=(if(inverse) 2.0 else -2.0)*PI/len; val wr0=cos(angle); val wi0=sin(angle); val h=len/2; var base=0
            while(base<n) { var wr=1.0; var wi=0.0; for(k in 0 until h) { val u=base+k; val v=u+h
                val vr=real[v]*wr-imag[v]*wi; val vi=real[v]*wi+imag[v]*wr
                real[v]=real[u]-vr; imag[v]=imag[u]-vi; real[u]+=vr; imag[u]+=vi
                val next=wr*wr0-wi*wi0; wi=wr*wi0+wi*wr0; wr=next }; base+=len }; len=len shl 1 }
        if(inverse) for(i in 0 until n) { real[i]=real[i]/n.toDouble(); imag[i]=imag[i]/n.toDouble() }
    }
}
