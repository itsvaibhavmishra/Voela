package com.vaibhawmishra.voela.data.audio

// On-device folder layout, all rooted under Music/Voela so saved files are
// organised and user-visible without any storage permission. Paths are relative
// to Environment.DIRECTORY_MUSIC (MediaStore RELATIVE_PATH).
object VoelaStorage {

    private const val ROOT = "Voela"

    val youtubeDownloads: String = "$ROOT/YouTube Downloads"

    fun audioSplits(name: String, stamp: String): String = "$ROOT/Audio Splits/${folder(name, stamp)}"

    fun vocalSplits(name: String, stamp: String): String = "$ROOT/Vocal Splits/${folder(name, stamp)}"

    private fun folder(name: String, stamp: String): String = "${sanitize(name)}_$stamp"

    // Strip characters illegal in file/folder names; cap length
    fun sanitize(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().take(120).ifBlank { "audio" }
}
