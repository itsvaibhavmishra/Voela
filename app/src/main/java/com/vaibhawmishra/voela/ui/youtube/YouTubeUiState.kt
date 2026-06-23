package com.vaibhawmishra.voela.ui.youtube

enum class ExtractionStatus { Idle, Processing, Done }

data class RecentLink(val title: String, val duration: String, val url: String)

data class ExtractedAudio(
    val title: String,
    val localPath: String,
    val waveform: List<Float>,
    val sourceUrl: String = "",
)

// A downloadable format/quality choice; carries the ffmpeg codec/bitrate + MediaStore MIME
data class DownloadOption(
    val title: String,
    val subtitle: String,
    val codec: String,
    val bitrate: String?,
    val mimeType: String,
    val extension: String,
)

val downloadOptions = listOf(
    DownloadOption("M4A · AAC", "Compressed · compatible", "aac", "256k", "audio/mp4", "m4a"),
    DownloadOption("WAV · Lossless", "Uncompressed", "pcm_s16le", null, "audio/x-wav", "wav"),
    // MP3 options return once the LAME encoder is wired in (NDK build)
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
    val message: String? = null,
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
