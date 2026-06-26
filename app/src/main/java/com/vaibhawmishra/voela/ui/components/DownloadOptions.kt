package com.vaibhawmishra.voela.ui.components

// A downloadable format/quality choice; carries the bitrate + MediaStore MIME/extension
data class DownloadOption(
    val title: String,
    val subtitle: String,
    val bitrate: String?,
    val mimeType: String,
    val extension: String,
)

val downloadOptions = listOf(
    DownloadOption("MP3 · 320 kbps", "High quality", "320k", "audio/mpeg", "mp3"),
    DownloadOption("MP3 · 192 kbps", "Standard", "192k", "audio/mpeg", "mp3"),
    DownloadOption("M4A · AAC", "Compressed · compatible", "256k", "audio/mp4", "m4a"),
    DownloadOption("WAV · Lossless", "Uncompressed", null, "audio/x-wav", "wav"),
)
