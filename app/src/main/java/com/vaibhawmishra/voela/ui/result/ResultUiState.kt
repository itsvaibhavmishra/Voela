package com.vaibhawmishra.voela.ui.result

data class StemUi(
    val label: String,
    val path: String,
    val waveform: List<Float> = emptyList(),
    val durationMs: Long = 0,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

data class ResultUiState(
    val title: String = "",
    val elapsedMs: Long = 0,
    val sampleRate: Int = 0,
    val stems: List<StemUi> = emptyList(),
    val playingIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val message: String? = null,
) {
    val durationMs: Long get() = stems.firstOrNull()?.durationMs ?: 0
    fun positionFor(index: Int): Long = if (index == playingIndex) positionMs else 0

    fun progressFor(index: Int): Float {
        if (index != playingIndex) return 0f
        val d = stems.getOrNull(index)?.durationMs ?: 0
        return if (d > 0) (positionMs.toFloat() / d).coerceIn(0f, 1f) else 0f
    }
}
