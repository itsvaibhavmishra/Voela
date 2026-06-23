package com.vaibhawmishra.voela.ui.trim

// Which feature this trim run feeds into — decides the action label and next destination
enum class TrimFeature(val key: String) {
    VOCALS("vocals"),
    AUDIO("audio");

    companion object {
        fun from(key: String?): TrimFeature = entries.firstOrNull { it.key == key } ?: VOCALS
    }
}

// Separation engine: FAST runs now; BEST (higher quality) is not wired up yet
enum class SeparationEngine { FAST, BEST }

data class TrimAudioUiState(
    val feature: TrimFeature = TrimFeature.VOCALS,
    val title: String = "",
    val format: String = "",
    val totalDuration: String = "",
    val sampleRate: String = "",
    val waveform: List<Float> = emptyList(),
    val durationMs: Long = 0,
    val startMs: Long = 0,
    val endMs: Long = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val engine: SeparationEngine = SeparationEngine.FAST,
) {
    val startFraction: Float get() = if (durationMs > 0) startMs.toFloat() / durationMs else 0f
    val endFraction: Float get() = if (durationMs > 0) endMs.toFloat() / durationMs else 1f
    val progressFraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
