package com.vaibhawmishra.voela.data.audio

// Keys for the Audio Split WorkManager job (cuts a selection into equal clips
// and saves them under Music/Voela/Audio Splits/{name}_{stamp}/).
object AudioSplit {
    const val WORK_NAME = "audio_split"
    const val KEY_SOURCE = "source"
    const val KEY_START_MS = "start_ms"
    const val KEY_END_MS = "end_ms"
    const val KEY_SEGMENT_MS = "segment_ms"
    const val KEY_TITLE = "title"
    const val KEY_EXTENSION = "extension"
    const val KEY_BITRATE = "bitrate"
    const val KEY_MIME = "mime"
    const val KEY_PROGRESS = "progress"
    const val KEY_COUNT = "count"
    const val KEY_ERROR = "error"
}
