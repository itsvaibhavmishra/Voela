package com.vaibhawmishra.voela.data.library

// A completed, time-expensive action worth returning to: a YouTube extraction or a
// finished Vocal Split. Files live in app-private storage (files/library/{id}/), kept
// automatically and subject to expiry — separate from anything the user exports to Music.
enum class LibraryType { EXTRACTION, VOCAL_SPLIT }

data class LibraryItem(
    val id: String,
    val type: LibraryType,
    val title: String,
    val createdAt: Long,
    val lastOpenedAt: Long,
    val durationMs: Long,
    val elapsedMs: Long,      // processing time, for the "Completed in X" line on splits
    val mediaName: String,    // extraction: the audio file name (e.g. "audio.m4a"); split: ""
)
