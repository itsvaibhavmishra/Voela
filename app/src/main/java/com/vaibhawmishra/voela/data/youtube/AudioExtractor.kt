package com.vaibhawmishra.voela.data.youtube

// Extracts audio from a media URL; emits coarse progress (0..100) while working.
// The real yt-dlp-backed implementation is loaded lazily by the YouTube feature only.
interface AudioExtractor {
    suspend fun extract(url: String, onProgress: (Int) -> Unit): ExtractedAudioResult
}

data class ExtractedAudioResult(
    val title: String,
    val localPath: String,
)
