package com.vaibhawmishra.voela.data.settings

// Output format the separated stems are stored in. Drives both library size and the
// quality of what's kept (and re-exported). bitrate is null for formats without a choice.
enum class StemFormat(
    val key: String,
    val label: String,
    val sublabel: String,
    val extension: String,
    val bitrate: String?,
) {
    M4A("m4a", "M4A · AAC", "Compressed · smallest", "m4a", null),
    MP3_320("mp3_320", "MP3 · 320 kbps", "High quality", "mp3", "320k"),
    MP3_192("mp3_192", "MP3 · 192 kbps", "Standard", "mp3", "192k"),
    WAV("wav", "WAV · Lossless", "Largest · pristine", "wav", null);

    companion object {
        fun from(key: String?): StemFormat = entries.firstOrNull { it.key == key } ?: M4A
    }
}
