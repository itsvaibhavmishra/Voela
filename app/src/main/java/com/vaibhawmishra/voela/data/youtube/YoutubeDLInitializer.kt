package com.vaibhawmishra.voela.data.youtube

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

// Lazy, one-time init of the yt-dlp engine. Only ever called from the extraction
// worker, so the Python/yt-dlp/FFmpeg runtime loads exclusively for the YouTube
// feature — never at app startup or on other screens. Self-update is kept OUT of
// this path (it blocks extraction); the extractor refreshes yt-dlp only on failure.
object YoutubeDLInitializer {

    @Volatile
    private var initialized = false

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
        initialized = true
    }
}
