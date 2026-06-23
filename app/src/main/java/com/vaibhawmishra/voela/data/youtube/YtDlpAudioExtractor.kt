package com.vaibhawmishra.voela.data.youtube

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

// Real yt-dlp-backed extraction. Downloads the best audio-only stream to
// app-specific storage in a single pass, reading the title from the metadata
// it writes. If a run fails (e.g. YouTube changed), it refreshes yt-dlp once
// and retries — keeping the happy path fast while self-healing on breakage.
class YtDlpAudioExtractor(private val context: Context) : AudioExtractor {

    override suspend fun extract(url: String, onProgress: (Int) -> Unit): ExtractedAudioResult =
        withContext(Dispatchers.IO) {
            YoutubeDLInitializer.ensureInitialized(context)
            try {
                download(url, onProgress)
            } catch (first: Exception) {
                runCatching { YoutubeDL.getInstance().updateYoutubeDL(context) }
                download(url, onProgress)
            }
        }

    private fun download(url: String, onProgress: (Int) -> Unit): ExtractedAudioResult {
        val outputDir = File(context.getExternalFilesDir(null), "extracted").apply { mkdirs() }
        outputDir.listFiles()?.forEach { it.delete() } // one extraction at a time

        val request = YoutubeDLRequest(url).apply {
            addOption("-f", "bestaudio")
            addOption("--no-playlist")
            addOption("--write-info-json")
            addOption("-o", "${outputDir.absolutePath}/%(id)s.%(ext)s")
        }
        YoutubeDL.getInstance().execute(request, null) { progress, _, _ ->
            onProgress(progress.toInt().coerceIn(0, 100))
        }

        val files = outputDir.listFiles().orEmpty()
        val audio = files.firstOrNull { !it.name.endsWith(".info.json") }
        val title = files.firstOrNull { it.name.endsWith(".info.json") }
            ?.let { runCatching { JSONObject(it.readText()).optString("title") }.getOrNull() }
            ?.takeIf { it.isNotBlank() } ?: "Audio"
        return ExtractedAudioResult(title = title, localPath = audio?.absolutePath.orEmpty())
    }
}
