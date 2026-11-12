package com.danim.autowarp.audio

import com.danim.autowarp.analysis.WarpMarker
import com.danim.autowarp.analysis.TransientEvent
import java.io.OutputStream
import kotlin.math.*

data class StreamingWsolaReport(
    val outputFrames: Long,
    val grainsProcessed: Long,
    val blocksWritten: Long,
    val averageStretchRatio: Double,
    val maxStretchRatio: Double,
    val maxSourceFramesBuffered: Int,
    val pitchPreserving: Boolean = true,
    val lockedTransientAnchors: Int = 0
)

enum class TransientAnchorMode { LEGACY, OFFSET_AWARE, ADAPTIVE, CONTENT_AWARE }

/** Stateful, bounded-memory WSOLA renderer. Stereo channels share each alignment decision. */
object StreamingWsolaProcessor {
    fun renderWav(source: PcmFrameSource, markers: List<WarpMarker>, output: OutputStream,
                  quality: WarpQuality = WarpQuality.BALANCED,
                  transients: List<TransientEvent> = emptyList(),
                  previewStartSeconds: Double = 0.0, previewDurationSeconds: Double? = null,
                  anchorMode: TransientAnchorMode = TransientAnchorMode.ADAPTIVE): StreamingWsolaReport {
        require(source.sampleRate > 0 && source.channels in 1..2 && source.frameCount > 1)
        val m = markers.sortedBy { it.targetSeconds }
        require(m.size >= 2 && m.all { it.sourceSeconds.isFinite() && it.targetSeconds.isFinite() && it.strength.isFinite() && it.strength in 0f..1f })
        require(m.first().sourceSeconds <= 1.0 / source.sampleRate && m.first().targetSeconds <= 1.0 / source.sampleRate)
        require(m.zipWithNext().all { (a,b) ->
            val ratio = (b.sourceSeconds - a.sourceSeconds) / (b.targetSeconds - a.targetSeconds)
            b.sourceSeconds > a.sourceSeconds && b.targetSeconds > a.targetSeconds &&
                a.sourceSeconds >= 0.0 && b.sourceSeconds <= source.frameCount.toDouble()/source.sampleRate &&
                a.targetSeconds >= 0.0 && ratio in 0.25..4.0
        }) { "Marcadores inválidos, no monótonos o con ratio fuera de 0,25×–4×" }
        // Promote confident detected attacks to explicit source/target anchors. Their target
        // positions are obtained from the user's warp map, so they preserve the intended grid.
        fun baseSourceAtTarget(targetSeconds: Double): Double {
            var lo = 0; var hi = m.lastIndex - 1
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (targetSeconds > m[mid + 1].targetSeconds) lo = mid + 1 else hi = mid }
            val a = m[lo]; val b = m[lo + 1]
            val f = ((targetSeconds - a.targetSeconds) / (b.targetSeconds - a.targetSeconds)).coerceIn(0.0, 1.0)
            val warped = a.sourceSeconds + (b.sourceSeconds - a.sourceSeconds) * f
            val strength = (a.strength + (b.strength - a.strength) * f).toDouble().coerceIn(0.0, 1.0)
            return targetSeconds * (1.0 - strength) + warped * strength
        }
        // Invert the actual strength-blended map, not just the full-strength marker curve.
        // This keeps transient anchors on the same target timeline as the rendered audio.
        fun baseTargetAt(sourceSeconds: Double): Double {
            var lo = m.first().targetSeconds; var hi = m.last().targetSeconds
            repeat(48) {
                val mid = (lo + hi) * 0.5
                if (baseSourceAtTarget(mid) < sourceSeconds) lo = mid else hi = mid
            }
            return (lo + hi) * 0.5
        }
        val transientAnchors = transients.asSequence()
            .filter { it.seconds.isFinite() && it.confidence >= 0.48 && it.seconds > 0.015 &&
                it.seconds < source.frameCount.toDouble() / source.sampleRate - 0.015 }
            .map { WarpMarker(it.seconds, baseTargetAt(it.seconds), it.confidence.toFloat().coerceIn(0f, 1f)) }
            .filter { candidate -> m.none { kotlin.math.abs(it.sourceSeconds - candidate.sourceSeconds) < 0.018 } }
            .sortedBy { it.sourceSeconds }.toList()
        val anchors = (m + transientAnchors).sortedBy { it.sourceSeconds }.fold(ArrayList<WarpMarker>()) { acc, marker ->
            if (acc.isEmpty() || (marker.sourceSeconds > acc.last().sourceSeconds && marker.targetSeconds > acc.last().targetSeconds)) acc.add(marker)
            acc
        }
        require(anchors.size >= 2) { "No hay suficientes anclajes válidos" }

        val fullOutputFrames = (anchors.last().targetSeconds * source.sampleRate).roundToLong().coerceAtLeast(1L)
        require(previewStartSeconds.isFinite() && previewStartSeconds >= 0.0)
        require(previewDurationSeconds == null || (previewDurationSeconds.isFinite() && previewDurationSeconds > 0.0))
        val startFrame = (previewStartSeconds * source.sampleRate).roundToLong().coerceIn(0L, fullOutputFrames - 1)
        val endFrame = previewDurationSeconds?.let {
            (startFrame + (it * source.sampleRate).roundToLong().coerceAtLeast(1L)).coerceAtMost(fullOutputFrames)
        } ?: fullOutputFrames
        val outputFrames = endFrame - startFrame
        val dataBytes = outputFrames * source.channels * 2L
        require(dataBytes <= 0xFFFF_FFFFL - 36L) { "Salida superior al límite WAV RIFF" }

        fun segmentAt(t: Double): Int {
            var lo=0; var hi=anchors.lastIndex-1
            while(lo<hi) { val mid=(lo+hi) ushr 1; if(t>anchors[mid+1].targetSeconds) lo=mid+1 else hi=mid }
            return lo
        }
        fun sourceAt(frame: Long): Double {
            val t=frame.toDouble()/source.sampleRate; val i=segmentAt(t); val a=anchors[i]; val b=anchors[i+1]
            val f=((t-a.targetSeconds)/(b.targetSeconds-a.targetSeconds)).coerceIn(0.0,1.0)
            val warped=a.sourceSeconds+(b.sourceSeconds-a.sourceSeconds)*f
            val strength=(a.strength+(b.strength-a.strength)*f).toDouble().coerceIn(0.0,1.0)
            return (t*(1.0-strength)+warped*strength)*source.sampleRate
        }
        fun ratioAt(frame: Long): Double {
            val i=segmentAt(frame.toDouble()/source.sampleRate); val a=anchors[i]; val b=anchors[i+1]
            val strength=((a.strength+b.strength)*0.5).toDouble().coerceIn(0.0,1.0)
            return (1.0-strength+(b.sourceSeconds-a.sourceSeconds)/(b.targetSeconds-a.targetSeconds)*strength).coerceIn(0.25,4.0)
        }

        // A true identity map needs no overlap-add: pass the original PCM through.
        // This prevents unnecessary transient smearing and remains bounded-memory,
        // including stereo and preview exports. Only the exact identity case qualifies.
        if (m.all { abs(it.sourceSeconds - it.targetSeconds) * source.sampleRate < 1e-6 } &&
            fullOutputFrames == source.frameCount) {
            writeHeader(output, source.sampleRate, source.channels, dataBytes.toInt())
            val chunkFrames = 4096
            val bytes = ByteArray(chunkFrames * source.channels * 2)
            var copied = 0L
            var blocks = 0L
            while (copied < outputFrames) {
                val count = min(chunkFrames.toLong(), outputFrames - copied).toInt()
                val samples = source.readFrames(startFrame + copied, count)
                require(samples.size == count * source.channels) { "La fuente devolvió una ventana incompleta" }
                var p = 0
                for (sample in samples) {
                    val q = (sample.coerceIn(-1f, 0.999969f) * 32768f).roundToInt()
                    bytes[p++] = (q and 255).toByte()
                    bytes[p++] = ((q ushr 8) and 255).toByte()
                }
                output.write(bytes, 0, p)
                copied += count
                blocks++
            }
            output.flush()
            return StreamingWsolaReport(outputFrames, 0L, blocks, 1.0, 1.0,
                min(chunkFrames.toLong(), outputFrames).toInt(), true, 0)
        }

        val grain=(source.sampleRate*quality.grainMs/1000).coerceIn(256,8192)
        val hop=(grain/4).coerceAtLeast(64); val overlap=grain-hop
        val search=(source.sampleRate*quality.searchMs/1000).coerceAtLeast(16)
        val accum=DoubleArray((grain+hop)*source.channels); val weights=DoubleArray(grain+hop)
        val bytes=ByteArray(hop*source.channels*2)
        writeHeader(output,source.sampleRate,source.channels,dataBytes.toInt())
        var emitted=0L; var grains=0L; var blocks=0L; var maxBuffered=0; var ratioSum=0.0; var ratioMax=0.0; var locked=0

        while(emitted<outputFrames) {
            val position = startFrame + emitted
            val expected=sourceAt(position).coerceIn(0.0,source.frameCount-1.001)
            val ratio=ratioAt(position); ratioSum+=ratio; ratioMax=max(ratioMax,max(ratio,1.0/ratio))
            val targetSeconds = position.toDouble() / source.sampleRate
            // Transient anchors are sorted by source time and, because the warp map is
            // monotone, by target time. Binary search avoids scanning every transient
            // for every grain (important for full-length vinyl recordings).
            var left = 0
            var right = transientAnchors.size
            while (left < right) {
                val mid = (left + right) ushr 1
                if (transientAnchors[mid].targetSeconds < targetSeconds) left = mid + 1
                else right = mid
            }
            val nearest = when {
                transientAnchors.isEmpty() -> null
                left == 0 -> transientAnchors[0]
                left == transientAnchors.size -> transientAnchors.last()
                else -> {
                    val before = transientAnchors[left - 1]
                    val after = transientAnchors[left]
                    if (targetSeconds - before.targetSeconds <= after.targetSeconds - targetSeconds) before else after
                }
            }
            val nearTransient = nearest?.takeIf {
                abs(it.targetSeconds - targetSeconds) <= hop.toDouble() / source.sampleRate * 0.55
            }
            val radius=if (nearTransient != null) 0 else min(search,max(8,(search*sqrt(ratio)).roundToInt()))
            val maxStart=(source.frameCount-grain).coerceAtLeast(0L)
            // Preserve the attack's position *inside* the output hop. Previously the
            // attack source frame was used as the grain start, shifting the onset
            // whenever its target position was not exactly on a hop boundary.
            val anchoredExpected = nearTransient?.let {
                val targetFrame = it.targetSeconds * source.sampleRate
                val sourceFrame = it.sourceSeconds * source.sampleRate
                // Conservative heuristic from the RC69 real-audio benchmark: offset-aware
                // improved both timing and energy for a 1% speed-up; legacy
                // preserved slightly more energy for a 1% slow-down.
                // This is NOT a per-attack perceptual or quality optimizer.
                val useOffset = when (anchorMode) {
                    TransientAnchorMode.LEGACY -> false
                    TransientAnchorMode.OFFSET_AWARE -> true
                    TransientAnchorMode.ADAPTIVE -> ratio > 1.005
                    // Experimental per-event decision: preserve the offset only for strong
                    // attacks significantly displaced from the output hop boundary.
                    // This is deliberately opt-in until multi-section validation.
                    TransientAnchorMode.CONTENT_AWARE -> {
                        val phaseOffset = abs(targetFrame - position)
                        val hopFraction = phaseOffset / hop.coerceAtLeast(1)
                        it.strength >= 0.85f && hopFraction >= 0.15 && ratio > 1.0
                    }
                }
                if (useOffset) (sourceFrame + (position - targetFrame) * ratio).roundToLong()
                else sourceFrame.roundToLong()
            } ?: expected.roundToLong()
            val low=(anchoredExpected-radius).coerceIn(0L,maxStart)
            val high=(anchoredExpected+radius).coerceIn(low,maxStart)
            val readCount=(high-low+grain).coerceAtMost(source.frameCount-low).toInt()
            if (nearTransient != null) locked++
            val candidates=source.readFrames(low,readCount)
            require(candidates.size==readCount*source.channels) { "La fuente devolvió una ventana incompleta" }
            maxBuffered=max(maxBuffered,readCount)

            var best=anchoredExpected.coerceIn(low,high)
            if(grains>0) {
                var bestScore=Double.NEGATIVE_INFINITY; val stride=quality.step; var candidate=low
                while(candidate<=high) {
                    val score=score(accum,weights,candidates,(candidate-low).toInt(),overlap,source.channels,source.sampleRate)
                    if(score>bestScore) { bestScore=score; best=candidate }; candidate+=stride
                }
                if(quality!=WarpQuality.FAST) for(c in max(low,best-stride)..min(high,best+stride)) {
                    val s=score(accum,weights,candidates,(c-low).toInt(),overlap,source.channels,source.sampleRate)
                    if(s>bestScore) { bestScore=s; best=c }
                }
            }
            val addFrames=min(min(grain.toLong(),source.frameCount-best),(outputFrames-emitted).coerceAtMost(grain.toLong()).toInt().toLong()).toInt()
            require(addFrames>0) { "No hay suficientes muestras para completar el render" }
            for(n in 0 until addFrames) {
                val phase=n.toDouble()/(grain-1).coerceAtLeast(1); val w=sin(PI*phase).pow(2.0).coerceAtLeast(1e-5)
                weights[n]+=w; val ix=((best-low).toInt()+n)*source.channels
                for(c in 0 until source.channels) accum[n*source.channels+c]+=candidates[ix+c]*w
            }
            val writeFrames=min(hop.toLong(),outputFrames-emitted).toInt(); var p=0
            for(n in 0 until writeFrames) {
                val weight=weights[n].coerceAtLeast(1e-9)
                for(c in 0 until source.channels) {
                    val sample=(accum[n*source.channels+c]/weight).toFloat().coerceIn(-1f,0.999969f)
                    val q=(sample*32768f).roundToInt(); bytes[p++]=(q and 255).toByte(); bytes[p++]=((q ushr 8) and 255).toByte()
                }
            }
            output.write(bytes,0,p); emitted+=writeFrames; blocks++; grains++
            val remaining=accum.size/source.channels-writeFrames
            for(i in 0 until remaining) {
                weights[i]=weights[i+writeFrames]
                for(c in 0 until source.channels) accum[i*source.channels+c]=accum[(i+writeFrames)*source.channels+c]
            }
            for(i in remaining until weights.size) weights[i]=0.0
            for(i in remaining*source.channels until accum.size) accum[i]=0.0
        }
        output.flush()
        return StreamingWsolaReport(emitted,grains,blocks,ratioSum/grains.coerceAtLeast(1),ratioMax,maxBuffered,true,locked)
    }

    private fun score(accum:DoubleArray,weights:DoubleArray,source:FloatArray,offset:Int,overlap:Int,channels:Int,sampleRate:Int):Double {
        var dot=0.0; var aa=0.0; var bb=0.0; val stride=max(2,sampleRate/3000); var n=0
        while(n<overlap) {
            val w=weights[n]
            if(w>1e-8) {
                var a=0.0; var b=0.0
                for(c in 0 until channels) { a+=accum[n*channels+c]/w; b+=source[(offset+n)*channels+c] }
                a/=channels; b/=channels; val emphasis=0.35+0.65*n.toDouble()/overlap.coerceAtLeast(1)
                dot+=a*b*emphasis; aa+=a*a*emphasis; bb+=b*b*emphasis
            }
            n+=stride
        }
        return dot/sqrt(max(1e-18,aa*bb))
    }
    private fun writeHeader(out:OutputStream,rate:Int,channels:Int,dataBytes:Int) {
        fun p16(v:Int){out.write(v and 255);out.write((v ushr 8) and 255)}
        fun p32(v:Int){p16(v and 65535);p16(v ushr 16)}
        out.write("RIFF".toByteArray());p32(36+dataBytes);out.write("WAVEfmt ".toByteArray());p32(16)
        p16(1);p16(channels);p32(rate);p32(rate*channels*2);p16(channels*2);p16(16);out.write("data".toByteArray());p32(dataBytes)
    }
}

/** Adapter used when the decoded PCM is already resident in memory. */
class PcmAudioFrameSource(private val audio: PcmAudio) : PcmFrameSource {
    override val sampleRate: Int get() = audio.sampleRate
    override val channels: Int get() = audio.channels
    override val frameCount: Long get() = audio.frames.toLong()
    override fun readFrames(firstFrame: Long, count: Int): FloatArray {
        require(firstFrame >= 0 && count >= 0 && firstFrame + count <= frameCount)
        val from = firstFrame.toInt() * channels
        return audio.samples.copyOfRange(from, from + count * channels)
    }
}
