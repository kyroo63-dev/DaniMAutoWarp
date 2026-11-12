package com.danim.autowarp.analysis

import java.util.Locale

/** Portable, documented CSV beatgrid export; times are seconds from file start. */
object BeatgridCsv {
    fun encode(result: AnalysisResult, events: List<TransientEvent> = emptyList()): String = buildString {
        appendLine("# DaniM Auto Warp beatgrid v2")
        appendLine("# time_unit=seconds; bpm_unit=beats_per_minute; bar_meter=4/4")
        appendLine("record_type,index,time_seconds,bpm,bar,beat,kind,confidence")
        result.beatTimes.forEachIndexed { index, time ->
            val bpm = if (index < result.beatTimes.lastIndex) {
                val delta = result.beatTimes[index + 1] - time
                if (delta > 0.0) 60.0 / delta else result.bpm
            } else result.bpm
            val beatInBar = (index % 4) + 1
            val bar = (index / 4) + 1
            appendLine(String.format(Locale.US, "beat,%d,%.6f,%.4f,%d,%d,,", index + 1, time, bpm, bar, beatInBar))
        }
        events.sortedBy { it.seconds }.forEachIndexed { index, event ->
            appendLine(String.format(Locale.US, "transient,%d,%.6f,,,,'%s',%.4f", index + 1, event.seconds, event.kind.name, event.confidence.coerceIn(0.0, 1.0)))
        }
    }
}
