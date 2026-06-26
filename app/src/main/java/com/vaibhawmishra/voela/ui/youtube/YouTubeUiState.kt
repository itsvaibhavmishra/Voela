package com.vaibhawmishra.voela.ui.youtube

enum class ExtractionStatus { Idle, Processing, Done }

data class RecentLink(val title: String, val duration: String, val url: String)

data class ExtractedAudio(
    val title: String,
    val localPath: String,
    val waveform: List<Float>,
    val sourceUrl: String = "",
)

data class YouTubeUiState(
    val url: String = "",
    val status: ExtractionStatus = ExtractionStatus.Idle,
    val progress: Int = 0,
    val result: ExtractedAudio? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isSaving: Boolean = false,
    val saveProgress: Int = 0,
    val savedLabel: String? = null,
    val message: String? = null,
    val recentLinks: List<RecentLink> = emptyList(),
) {
    val playbackProgress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
