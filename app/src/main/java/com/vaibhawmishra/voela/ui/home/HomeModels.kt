package com.vaibhawmishra.voela.ui.home

// What was produced from a source clip — drives badge label and accent colour
enum class ProcessType(val label: String) {
    VOCAL_REMOVAL("Vocal Removal"),
    AUDIO_SPLIT("Audio Split"),
}

// UI model for a Recents row; duration and timeAgo arrive pre-formatted for now
data class RecentAudio(
    val id: String,
    val title: String,
    val duration: String,
    val timeAgo: String,
    val type: ProcessType,
)
