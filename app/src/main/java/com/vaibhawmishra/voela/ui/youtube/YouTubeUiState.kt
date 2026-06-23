package com.vaibhawmishra.voela.ui.youtube

enum class ExtractionStatus { Idle, Processing, Done }

data class RecentLink(val title: String, val duration: String, val url: String)

data class ExtractedAudio(
    val title: String,
    val localPath: String,
    val waveform: List<Float>,
)

data class YouTubeUiState(
    val url: String = "",
    val status: ExtractionStatus = ExtractionStatus.Idle,
    val progress: Int = 0,
    val result: ExtractedAudio? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val recentLinks: List<RecentLink> = emptyList(),
) {
    val playbackProgress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

// Seed shown until recents are persisted (DataStore) in a later phase
internal val initialRecentLinks = listOf(
    RecentLink("Lofi study mix — 1 hour", "1:02:14", "https://youtu.be/aaaa1111"),
    RecentLink("Acoustic guitar session", "3:24", "https://youtu.be/bbbb2222"),
    RecentLink("City ambience for focus", "4:20", "https://youtu.be/cccc3333"),
)
