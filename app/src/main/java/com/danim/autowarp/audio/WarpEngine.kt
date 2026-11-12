package com.danim.autowarp.audio

import com.danim.autowarp.analysis.TransientEvent
import com.danim.autowarp.analysis.WarpMarker

enum class WarpEngine { WSOLA, PHASE_VOCODER }

object AudioWarpEngine {
    fun render(input: PcmAudio, markers: List<WarpMarker>, transients: List<TransientEvent>, engine: WarpEngine, quality: WarpQuality): PcmAudio =
        when(engine) {
            WarpEngine.WSOLA -> WsolaWarp.render(input, markers, transients, quality)
            WarpEngine.PHASE_VOCODER -> PhaseVocoderWarp.render(input, markers,
                if (quality == WarpQuality.HIGH) 4096 else if (quality == WarpQuality.FAST) 1024 else 2048,
                transients)
        }
}
