#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
INPUT="${1:-}"
if [[ ! -f "$INPUT" ]]; then echo 'Uso: smoke-real-detector-audit.sh <audio.flac|audio.wav> [output.csv]' >&2; exit 2; fi
OUT="${2:-$PWD/rc75-detector-audit.csv}"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Adapter.kt" <<'KT'
package com.danim.autowarp.audio
class PcmAudioFrameSource(private val audio:PcmAudio):PcmFrameSource {
 override val sampleRate get()=audio.sampleRate
 override val channels get()=audio.channels
 override val frameCount get()=audio.frames.toLong()
 override fun readFrames(firstFrame:Long,count:Int):FloatArray {
  require(firstFrame>=0 && count>=0 && firstFrame+count<=frameCount)
  return audio.samples.copyOfRange(firstFrame.toInt()*channels,(firstFrame.toInt()+count)*channels)
 }
}
KT
cat > "$TMP/Audit.kt" <<'KT'
import com.danim.autowarp.analysis.*
import com.danim.autowarp.audio.*
import java.io.File
import java.util.Locale
fun main(args:Array<String>) {
 val out=File(args[0]);out.parentFile?.mkdirs()
 val details=File(out.parentFile ?: File("."),out.nameWithoutExtension+"-events.csv")
 val detailWriter=details.printWriter()
 detailWriter.println("offset_s,event_s,absolute_event_s,kind,detector_confidence,bass_share,mid_share,spectral_score,rhythm_score,consecutive_support")
 out.printWriter().use { csv ->
  csv.println("offset_s,duration_s,bpm,confidence,kick_candidates,other_candidates,first_kick_s,median_kick_interval_s,automatic_warp,close_pairs,pulse_pairs,missed_pulse_pairs,irregular_pairs,longest_pulse_run,cluster_retained,cluster_suppressed,grid_matched,grid_distinct_beats,grid_missing,grid_longest_run,grid_coverage,grid_reliable,spectral_grid_matched,spectral_supported,jointly_supported,distinct_joint_beats,longest_joint_run,joint_coverage,joint_reliable,holdout_training,holdout_validation,holdout_matched,holdout_ratio,holdout_phase_error_s,holdout_sufficient,tempo_early_pairs,tempo_late_pairs,tempo_early_bpm,tempo_late_bpm,tempo_relative_drift,tempo_sufficient,tempo_consistent,window_supported,window_adjacent,window_largest_drift,window_stable")
  for (p in args.drop(1)) {
   val audio=File(p).inputStream().use(PcmWave::read)
   val r=BeatAnalyzer.analyze(audio)
   val offset=File(p).nameWithoutExtension.substringBefore('_').toInt()
   val source=PcmAudioFrameSource(audio)
   val kickEvents=r.kickTimes.map { TransientEvent(it,TransientKind.KICK,1.0) }
   val rhythm=KickRhythmEvidence.assess(r.kickTimes,r.bpm)
   val consecutive=KickRhythmEvidence.consecutiveSupport(r.kickTimes,r.bpm)
   var kickIndex=0
   for (event in (kickEvents+r.otherTransients).sortedBy { it.seconds }) {
    val e=SpectralKickEvidence.measure(source,event.seconds)
    fun f(x:Double)=String.format(Locale.US,"%.5f",x)
    val index=if(event.kind==TransientKind.KICK) kickIndex++ else -1
    val rhythmScore=if(index>=0) rhythm.candidateScores.getOrElse(index) { 0.0 } else 0.0
    val support=if(index>=0) consecutive.getOrElse(index) { 0.0 } else 0.0
    detailWriter.println("$offset,${f(event.seconds)},${f(offset+event.seconds)},${event.kind},${f(event.confidence)},${f(e.bassShare)},${f(e.midShare)},${f(e.score)},${f(rhythmScore)},${f(support)}")
   }
   val clustered=KickCandidateCluster.group(r.kickTimes.map { t ->
    KickCandidateCluster.Candidate(t,SpectralKickEvidence.measure(source,t).score)
   },r.bpm.takeIf { it>0.0 } ?: 120.0)
   val grid=KickGridContinuityAudit.inspect(clustered.retained.map { it.seconds },r.bpm.takeIf { it in 60.0..240.0 } ?: 120.0)
   val joint=KickSpectralGridAudit.inspect(clustered.retained,r.bpm.takeIf { it in 60.0..240.0 } ?: 120.0,grid.phaseSeconds)
   val holdout=KickHoldoutPhaseAudit.inspect(clustered.retained,r.bpm.takeIf { it in 60.0..240.0 } ?: 120.0)
   val tempoHoldout=KickTempoHoldoutAudit.inspect(clustered.retained.map { it.seconds },r.bpm.takeIf { it in 60.0..240.0 } ?: 120.0)
   val windowAudit=KickTempoWindowAudit.inspect(clustered.retained.map { it.seconds },r.bpm.takeIf { it in 60.0..240.0 } ?: 120.0,audio.durationSeconds)
   val intervalAudit=KickIntervalAudit.inspect(r.kickTimes,r.bpm.takeIf { it>0.0 } ?: 120.0)
   val intervals=r.kickTimes.zipWithNext().map { it.second-it.first }.sorted()
   val median=if(intervals.isEmpty()) Double.NaN else intervals[intervals.size/2]
   val nonIdentity=r.markers.any { kotlin.math.abs(it.sourceSeconds-it.targetSeconds)>1e-4 }
   fun fmt(x:Double)=String.format(Locale.US,"%.5f",x)
   csv.println("$offset,${fmt(audio.durationSeconds)},${fmt(r.bpm)},${fmt(r.confidence)},${r.kickTimes.size},${r.otherTransients.size},${fmt(r.kickTimes.firstOrNull()?:Double.NaN)},${fmt(median)},$nonIdentity,${intervalAudit.closePairs},${intervalAudit.pulsePairs},${intervalAudit.missedPulsePairs},${intervalAudit.irregularPairs},${intervalAudit.longestPulseRun},${clustered.retained.size},${clustered.suppressed},${grid.matchedCandidates},${grid.distinctMatchedBeats},${grid.missingInteriorBeats},${grid.longestConsecutiveRun},${fmt(grid.coverage)},${grid.reliable},${joint.gridMatched},${joint.spectralSupported},${joint.jointlySupported},${joint.distinctJointBeats},${joint.longestJointRun},${fmt(joint.jointCoverage)},${joint.reliable},${holdout.trainingCount},${holdout.validationCount},${holdout.validationMatched},${fmt(holdout.validationRatio)},${fmt(holdout.phaseErrorSeconds)},${holdout.sufficientEvidence},${tempoHoldout.earlyPairs},${tempoHoldout.latePairs},${fmt(tempoHoldout.earlyBpm)},${fmt(tempoHoldout.lateBpm)},${fmt(tempoHoldout.relativeDrift)},${tempoHoldout.sufficientEvidence},${tempoHoldout.consistent},${windowAudit.supportedWindows},${windowAudit.adjacentComparisons},${fmt(windowAudit.largestAdjacentDrift)},${windowAudit.stable}")
   println("offset=${offset}s bpm=${fmt(r.bpm)} confidence=${fmt(r.confidence)} kickCandidates=${r.kickTimes.size} other=${r.otherTransients.size} automaticWarp=$nonIdentity")
  }
 }
 detailWriter.close()
}
KT
mapfile -t SOURCES < <(find "$ROOT/app/src/main/java/com/danim/autowarp/analysis" -maxdepth 1 -name '*.kt' | sort)
kotlinc "${SOURCES[@]}" "$ROOT/app/src/main/java/com/danim/autowarp/audio/PcmWave.kt" "$ROOT/app/src/main/java/com/danim/autowarp/audio/StreamingWarpProcessor.kt" "$TMP/Adapter.kt" "$TMP/Audit.kt" -include-runtime -d "$TMP/audit.jar"
WAVS=()
for OFFSET in ${AUDIT_OFFSETS:-0 24 48 72 96 120 144 168 192 216 240 264 288 312 336 360 384 408 432 456 480 504}; do
 WAV="$TMP/${OFFSET}_clip.wav"
 ffmpeg -nostdin -v error -ss "$OFFSET" -i "$INPUT" -t "${AUDIT_DURATION:-24}" -ar 44100 -ac 2 -c:a pcm_s16le "$WAV" -y
 WAVS+=("$WAV")
done
java -jar "$TMP/audit.jar" "$OUT" "${WAVS[@]}"
echo "CSV: $OUT"
echo "Eventos: ${OUT%.csv}-events.csv"
