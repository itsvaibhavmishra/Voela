package com.vaibhawmishra.voela.data.youtube

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL

// Lazy, one-time init of the yt-dlp engine. Only ever called from the extraction
// worker, so the Python/yt-dlp runtime loads exclusively for the YouTube feature —
// never at app startup or on other screens. ffmpeg is intentionally not bundled (we
// download a single pre-muxed audio stream, no muxing/transcoding). Self-update is
// kept OUT of this path (it blocks extraction); the extractor refreshes on failure.
object YoutubeDLInitializer {

    @Volatile
    private var initialized = false

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return
        YoutubeDL.getInstance().init(context)
        initialized = true
    }
}
