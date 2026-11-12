package com.danim.autowarp.analysis

import com.danim.autowarp.audio.PcmAudio
import com.danim.autowarp.audio.PcmFrameSource
import com.danim.autowarp.audio.PcmAudioFrameSource
import kotlin.math.*

data class WarpMarker(val sourceSeconds: Double, val targetSeconds: Double, val strength: Float = 1f)
data class TransientEvent(val seconds: Double, val kind: TransientKind, val confidence: Double)
enum class TransientKind { KICK, SNARE, HI_HAT, PERCUSSION }
data class AnalysisResult(
    val bpm: Double, val beatTimes: List<Double>, val markers: List<WarpMarker>, val confidence: Double,
    val kickTimes: List<Double> = emptyList(), val otherTransients: List<TransientEvent> = emptyList(),
    val tempoCurve: List<Pair<Double, Double>> = emptyList(),
    /** Signed displacement from the constant-tempo reference grid, in milliseconds. */
    val microTimingMs: List<Double> = emptyList(),
    val tempoMinBpm: Double = bpm, val tempoMaxBpm: Double = bpm,
    /** Tempo estimated across four-beat bars to suppress single-beat jitter. */
    val barTempoCurve: List<Pair<Double, Double>> = emptyList(),
    val downbeatBeatIndex: Int = 0,
    val downbeatConfidence: Double = 0.0,
    val diagnostics: WarpDiagnostics? = null,
    val confidenceZones: List<ConfidenceZone> = emptyList(),
    /** Independent kick-phase diagnostics; not authorization for automatic warp. */
    val kickPhaseWindows: List<KickPhaseDrift.Window> = emptyList(),
    val kickPhaseSegments: KickPhaseSegments.Report? = null
)

/** Multiband onset analysis and adaptive beat tracking for regular dance material. */
object BeatAnalyzer {
    private data class FrameFeature(val time: Double, val low: Double, val mid: Double, val high: Double,
                                    val lowFlux: Double, val midFlux: Double, val highFlux: Double)

    fun analyze(audio: PcmAudio): AnalysisResult = analyze(PcmAudioFrameSource(audio))

    fun analyze(audio: PcmFrameSource): AnalysisResult {
        val durationSeconds = audio.frameCount.toDouble() / audio.sampleRate
        if (audio.frameCount < audio.sampleRate * 3 / 2) return AnalysisResult(0.0, emptyList(), emptyList(), 0.0)
        val hop = (audio.sampleRate / 100).coerceAtLeast(80)
        val features = extractFeatures(audio, hop)
        val low = DoubleArray(features.size) { features[it].lowFlux + features[it].low * 0.18 }
        val midCombined = DoubleArray(features.size) { features[it].midFlux + features[it].highFlux * 0.35 }
        val highFlux = DoubleArray(features.size) { features[it].highFlux }
        val candidates = ArrayList<TransientEvent>()
        val refractory = (0.105 * audio.sampleRate / hop).roundToInt().coerceAtLeast(1)
        var lastKick = -refractory; var lastOther = -refractory
        for (i in 3 until features.size - 3) {
            val f = features[i]
            val threshold = robustThreshold(low, i, 75, 2.0)
            if (low[i] >= low[i - 1] && low[i] > low[i + 1] && low[i] >= low[i - 2] && low[i] > threshold) {
                val total = (f.low + f.mid + f.high).coerceAtLeast(1e-12)
                val lowRatio = f.low / total
                val lowAttack = f.lowFlux / (f.lowFlux + f.midFlux + f.highFlux + 1e-12)
                val score = (0.65 * lowRatio + 0.35 * lowAttack).coerceIn(0.0, 1.0)
                if (score > 0.39 && i - lastKick >= refractory) {
                    val spectral = SpectralKickEvidence.measure(audio, f.time)
                    val rawStrength = (score * (low[i] / threshold).coerceAtMost(2.0) / 2.0 *
                        (0.7 + 0.3 * spectral.score)).coerceIn(0.1, 1.0)
                    val strength = KickOverlapEvidence.assess(rawStrength, f.lowFlux, f.midFlux,
                        f.highFlux, spectral.bassShare).confidence
                    if (spectral.score >= 0.12 || score >= 0.72) {
                        candidates.add(TransientEvent(f.time, TransientKind.KICK, strength)); lastKick = i
                    }
                }
            }
            val highThreshold = robustThreshold(highFlux, i, 35, 2.2)
            val isHatPeak = f.highFlux > highThreshold && f.highFlux >= highFlux[i - 1] &&
                f.highFlux > highFlux[i + 1] && f.high > f.mid * 0.9 && f.high > f.low * 0.35
            if (isHatPeak && i - lastOther >= refractory) {
                candidates.add(TransientEvent(f.time, TransientKind.HI_HAT, (f.highFlux / highThreshold / 3.0).coerceIn(0.1, 1.0)))
                lastOther = i
            } else {
                val otherThreshold = robustThreshold(midCombined, i, 45, 2.6)
                if (midCombined[i] > otherThreshold && i - lastOther >= refractory && (f.mid + f.high) > f.low * 0.65) {
                    val highRatio = f.high / (f.low + f.mid + f.high + 1e-12)
                    val midRatio = f.mid / (f.low + f.mid + f.high + 1e-12)
                    val kind = if (midRatio > 0.38 && f.mid > f.high * 0.85) TransientKind.SNARE else
                        if (highRatio > 0.58) TransientKind.HI_HAT else TransientKind.PERCUSSION
                    candidates.add(TransientEvent(f.time, kind, (midCombined[i] / otherThreshold / 3.0).coerceIn(0.1, 1.0)))
                    lastOther = i
                }
            }
        }
        val kicks = candidates.filter { it.kind == TransientKind.KICK }.sortedBy { it.seconds }
        if (kicks.size < 4) return fallbackGrid(audio, features, candidates, hop)
        val intervals = kicks.zipWithNext().map { it.second.seconds - it.first.seconds }.filter { it in 0.22..1.5 }
        val base = estimatePeriod(low, audio.sampleRate, hop)
            ?: intervals.map(::normalizeBeatInterval).filter { it in 0.30..0.8 }.let { if (it.isEmpty()) 0.5 else median(it) }
        val tracked = trackBeats(kicks, base, durationSeconds)
        if (tracked.size < 4) return fallbackGrid(audio, features, candidates, hop)
        val localPeriods = tracked.zipWithNext().map { it.second - it.first }.filter { it in 0.30..0.8 }
        val medianPeriod = median(localPeriods.ifEmpty { listOf(base) }).coerceIn(0.30, 0.8)
        val bpm = 60.0 / medianPeriod
        val beatTargets = tracked.indices.map { i -> tracked.first() + i * medianPeriod }
        val markers = ArrayList<WarpMarker>().apply { add(WarpMarker(0.0, 0.0, 1f)) }
        tracked.forEachIndexed { i, source ->
            val target = beatTargets[i]
            if (source > 0.005 && source < durationSeconds - 0.005 &&
                target > markers.last().targetSeconds + 0.005 && target < durationSeconds - 0.005) {
                markers.add(WarpMarker(source, target, 1f))
            }
        }
        // Keep the project duration stable; the final section absorbs only the residual drift.
        if (durationSeconds > markers.last().targetSeconds + 0.005)
            markers.add(WarpMarker(durationSeconds, durationSeconds, 1f))
        val tempoCurve = tracked.mapIndexed { i, t ->
            val p = when { i == 0 && tracked.size > 1 -> tracked[1] - t; i > 0 -> t - tracked[i - 1]; else -> medianPeriod }.coerceIn(0.25, 1.0)
            t to 60.0 / p
        }
        val tempos = tempoCurve.map { it.second }
        val microTiming = tracked.indices.map { i -> (tracked[i] - beatTargets[i]) * 1000.0 }
        val barTempoCurve = (0 until tracked.size - 4 step 4).map { i ->
            val barPeriod = (tracked[i + 4] - tracked[i]) / 4.0
            tracked[i] to (60.0 / barPeriod.coerceIn(0.25, 1.0))
        }
        // A local tracker can lock onto percussion subdivisions halfway through a record.
        // Do not warp when consecutive four-beat bars imply an implausible tempo jump.
        val continuity = TempoContinuityGate.assess(tracked)
        // Cross-check the tracked pulse against the low-frequency autocorrelation.
        // Percussion subdivisions can create a plausible but incorrect ~180 BPM grid.
        val referenceBpm = 60.0 / base
        val tempoDisagreement = abs(bpm - referenceBpm) / referenceBpm
        val phaseWindows = KickPhaseDrift.inspect(kicks.map { it.seconds }, referenceBpm, durationSeconds)
        val phaseSegments = KickPhaseSegments.inspect(phaseWindows, referenceBpm)
        val coverage = (kicks.size / max(4.0, durationSeconds * bpm / 60.0)).coerceIn(0.0, 1.0)
        // Reject an apparent grid supported by off-phase percussion rather than
        // kick attacks. A large number of interpolated beats is not evidence.
        val phase = KickPhaseEvidence.assess(tracked, kicks.map { it.seconds }, medianPeriod)
        // Independent kick-phase audit: a beat tracker must agree with a phase
        // supported by the detected attacks, not merely its own interpolation.
        val pulseAudit = KickPulseAudit.assess(kicks.map { it.seconds }, medianPeriod)
        val pulsePhaseError = if (tracked.isNotEmpty() && pulseAudit.reliable) {
            val cycles = kotlin.math.round((tracked.first() - pulseAudit.phaseSeconds) / medianPeriod)
            kotlin.math.abs(tracked.first() - pulseAudit.phaseSeconds - cycles * medianPeriod)
        } else Double.POSITIVE_INFINITY
        val phaseFactor = if (phase.reliable && pulseAudit.reliable && pulsePhaseError <= 0.055)
            phase.supportRatio else 0.0
        val densityGate = KickDensityGate.assessTimeline(kicks.map { it.seconds }, durationSeconds)
        // Conservative rhythmic corroboration: only lower confidence, never promote a weak grid.
        // A coherent run must contain at least three distinct pulse slots. Do not apply
        // this filter when there are too few candidates to evaluate the sequence.
        val rhythmSupport = KickRhythmEvidence.consecutiveSupport(
            kicks.map { it.seconds }, referenceBpm, minRun = 3)
        val rhythmFactor = KickSequenceGate.assess(rhythmSupport).confidenceMultiplier
        val confidence = (coverage * (1.0 - tempoDisagreement / 0.12).coerceIn(0.0, 1.0) *
            (if (continuity.reliable && densityGate.plausible) 1.0 else 0.0) * phaseFactor * rhythmFactor)
            .coerceIn(0.0, 0.95)
        // Never automatically stretch audio when the two tempo estimators disagree.
        // Preserve the detected transients for manual inspection instead.
        val automaticMarkers = if (tempoDisagreement > 0.12 || confidence < 0.20)
            listOf(WarpMarker(0.0, 0.0), WarpMarker(durationSeconds, durationSeconds))
        else markers
        // A conflicting tracked tempo is not a trustworthy BPM estimate. Report the
        // independent low-band periodicity, but never infer a corrected beatgrid from it.
        // The existing identity markers keep automatic processing non-destructive.
        if (tempoDisagreement > 0.12) {
            val safeBpm = referenceBpm.coerceIn(95.0, 180.0)
            val identity = listOf(WarpMarker(0.0, 0.0), WarpMarker(durationSeconds, durationSeconds))
            val safe = WarpSafetyGate.sanitize(identity, durationSeconds)
            val safeDiagnostics = WarpDiagnosticsAnalyzer.build(candidates, emptyList(), emptyList(), safe, 0.0)
            return AnalysisResult(safeBpm, emptyList(), safe.markers, 0.0,
                kicks.map { it.seconds }, candidates.filter { it.kind != TransientKind.KICK },
                diagnostics = safeDiagnostics, kickPhaseWindows = phaseWindows,
                kickPhaseSegments = phaseSegments)
        }
        val barPhase = BarAlignmentAnalyzer.estimate(beatTargets, kicks)
        val safety = WarpSafetyGate.sanitize(automaticMarkers, durationSeconds)
        val diagnostics = WarpDiagnosticsAnalyzer.build(candidates, microTiming, tempoCurve, safety, confidence)
        val zones = ZoneConfidenceAnalyzer.analyze(durationSeconds, tracked, candidates)
        return AnalysisResult(bpm, beatTargets, safety.markers, confidence, kicks.map { it.seconds },
            candidates.filter { it.kind != TransientKind.KICK }, tempoCurve, microTiming,
            tempos.minOrNull() ?: bpm, tempos.maxOrNull() ?: bpm, barTempoCurve,
            barPhase.downbeatBeatIndex, barPhase.confidence, diagnostics, zones,
            phaseWindows, phaseSegments)
    }

    private fun extractFeatures(a: PcmFrameSource, hop: Int): List<FrameFeature> {
        val result = ArrayList<FrameFeature>(); var lp1 = 0.0; var lp2 = 0.0
        var prevLow = 0.0; var prevMid = 0.0; var prevHigh = 0.0
        val alphaLow = 1.0 - exp(-2 * PI * 150 / a.sampleRate)
        val alphaMid = 1.0 - exp(-2 * PI * 1800 / a.sampleRate)
        var frame = 0L
        while (frame < a.frameCount) {
            var eL = 0.0; var eM = 0.0; var eH = 0.0; val end = minOf(a.frameCount, frame + hop)
            val window = a.readFrames(frame, (end - frame).toInt())
            for (i in frame until end) {
                var x = 0.0; for (c in 0 until a.channels) x += window[((i - frame) * a.channels + c).toInt()]; x /= a.channels
                lp1 += alphaLow * (x - lp1); lp2 += alphaMid * (x - lp2)
                val lo = lp1; val mid = lp2 - lp1; val hi = x - lp2
                eL += lo * lo; eM += mid * mid; eH += hi * hi
            }
            val n = (end - frame).coerceAtLeast(1); val l = sqrt(eL / n); val m = sqrt(eM / n); val h = sqrt(eH / n)
            result.add(FrameFeature(frame.toDouble() / a.sampleRate, l, m, h, max(0.0, l - prevLow), max(0.0, m - prevMid), max(0.0, h - prevHigh)))
            prevLow = l; prevMid = m; prevHigh = h; frame = end
        }
        return result
    }
    private fun robustThreshold(v: DoubleArray, i: Int, radius: Int, mult: Double): Double {
        val s = (max(0, i - radius)..min(v.lastIndex, i + radius)).map { v[it] }
        val med = median(s); val mad = median(s.map { abs(it - med) })
        return med + mult * max(mad, med * 0.12) + 1e-10
    }
    private fun estimatePeriod(env: DoubleArray, sr: Int, hop: Int): Double? {
        val minLag = (60.0 / 180 * sr / hop).roundToInt().coerceAtLeast(1)
        val maxLag = (60.0 / 95 * sr / hop).roundToInt().coerceAtMost(env.size / 2)
        if (maxLag <= minLag) return null
        var bestLag = minLag; var best = -1.0
        for (lag in minLag..maxLag) {
            var sum = 0.0; var norm = 0.0
            for (i in lag until env.size) { sum += env[i] * env[i - lag]; norm += env[i] * env[i] }
            val score = sum / (norm + 1e-12); if (score > best) { best = score; bestLag = lag }
        }
        return bestLag * hop.toDouble() / sr
    }

    /** Fills missed kick positions and smooths gradual record-speed changes without chasing single false hits. */
    private fun trackBeats(kicks: List<TransientEvent>, initialPeriod: Double, duration: Double): List<Double> {
        val times = kicks.map { it.seconds }.distinct().filter { it in 0.0..duration }.sorted()
        if (times.isEmpty()) return emptyList()
        var period = initialPeriod.coerceIn(0.30, 0.8); var previousKick = times.first()
        val beats = arrayListOf(previousKick)
        for (kick in times.drop(1)) {
            val delta = kick - previousKick
            if (delta < 0.22 || delta > 3.2) continue
            val multiple = (delta / period).roundToInt().coerceIn(1, 4)
            val measured = (delta / multiple).coerceIn(0.30, 0.8)
            if (abs(measured - period) / period < 0.22) {
                val bounded = measured.coerceIn(period * 0.90, period * 1.10)
                period = (period * 0.78 + bounded * 0.22).coerceIn(0.30, 0.8)
            }
            // Even interpolation avoids shifting phase when one kick is missed by the detector.
            for (step in 1..multiple) {
                val t = if (step == multiple) kick else previousKick + delta * step / multiple
                if (t > beats.last() + 0.12) beats.add(t)
            }
            previousKick = kick
        }
        return beats.filter { it < duration }
    }
    /** No detected kick sequence means that phase and tempo are unverified.
     *  Return the estimated BPM for display only; never fabricate warp anchors.
     */
    private fun fallbackGrid(a: PcmFrameSource, f: List<FrameFeature>, events: List<TransientEvent>, hop: Int): AnalysisResult {
        val env = DoubleArray(f.size) { f[it].lowFlux + f[it].low * 0.18 }
        val period = (estimatePeriod(env, a.sampleRate, hop) ?: 0.5).coerceIn(0.30, 0.8)
        val bpm = 60.0 / period
        val duration = a.frameCount.toDouble() / a.sampleRate
        val identity = listOf(WarpMarker(0.0, 0.0), WarpMarker(duration, duration))
        return AnalysisResult(bpm, emptyList(), identity, 0.0,
            events.filter { it.kind == TransientKind.KICK }.map { it.seconds },
            events.filter { it.kind != TransientKind.KICK })
    }
    private fun normalizeBeatInterval(v: Double): Double { var x = v; while (x < 0.30) x *= 2; while (x > 0.8) x /= 2; return x }
    private fun median(v: List<Double>): Double {
        if (v.isEmpty()) return 0.5
        val s = v.sorted(); val m = s.size / 2
        return if (s.size % 2 == 0) (s[m - 1] + s[m]) / 2.0 else s[m]
    }
}
