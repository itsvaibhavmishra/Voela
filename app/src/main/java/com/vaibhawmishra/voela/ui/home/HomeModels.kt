package com.vaibhawmishra.voela.ui.home

import com.vaibhawmishra.voela.data.library.LibraryItem
import com.vaibhawmishra.voela.data.library.LibraryType
import com.vaibhawmishra.voela.ui.formatClock
import com.vaibhawmishra.voela.ui.relativeTime

// What was produced from a source clip — drives badge label and accent colour
enum class ProcessType(val label: String) {
    VOCAL_REMOVAL("Vocal Split"),
    AUDIO_SPLIT("Audio Split"),
    EXTRACTION("YouTube Audio"),
}

// UI model for a Recents row; duration and timeAgo arrive pre-formatted for now
data class RecentAudio(
    val id: String,
    val title: String,
    val duration: String,
    val timeAgo: String,
    val type: ProcessType,
)

// Map a stored library item to the Recents/Library row model
fun LibraryItem.toRecentAudio(): RecentAudio = RecentAudio(
    id = id,
    title = title.ifBlank { "Audio" },
    duration = formatClock(durationMs),
    timeAgo = relativeTime(createdAt),
    type = if (type == LibraryType.EXTRACTION) ProcessType.EXTRACTION else ProcessType.VOCAL_REMOVAL,
)
