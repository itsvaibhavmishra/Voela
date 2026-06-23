package com.vaibhawmishra.voela.data.audio

// Shared keys for the audio save/transcode WorkManager job. Any feature
// (YouTube, splits, vocals) can enqueue AudioSaveWorker with these.
object AudioSave {
    const val WORK_NAME = "audio_save"
    const val KEY_INPUT_PATH = "input_path"
    const val KEY_BITRATE = "bitrate"
    const val KEY_MIME = "mime"
    const val KEY_EXTENSION = "extension"
    const val KEY_TITLE = "title"
    const val KEY_SUBPATH = "subpath"
    const val KEY_PROGRESS = "progress"
    const val KEY_SAVED_NAME = "saved_name"
    const val KEY_ERROR = "error"
}
