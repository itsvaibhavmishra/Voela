package com.vaibhawmishra.voela.data.youtube

// Shared keys/identifiers for the extraction WorkManager job
object Extraction {
    const val WORK_NAME = "youtube_extraction"
    const val SAVE_WORK_NAME = "youtube_save"
    const val KEY_URL = "url"
    const val KEY_PROGRESS = "progress"
    const val KEY_OUTPUT_PATH = "output_path"
    const val KEY_TITLE = "title"
    const val KEY_SOURCE_URL = "source_url"
    const val KEY_ERROR = "error"

    // Download/save job (transcode a local file with the bundled ffmpeg)
    const val KEY_INPUT_PATH = "input_path"
    const val KEY_CODEC = "codec"
    const val KEY_BITRATE = "bitrate"
    const val KEY_MIME = "mime"
    const val KEY_EXTENSION = "extension"
    const val KEY_SAVED_NAME = "saved_name"
}
