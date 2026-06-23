package com.vaibhawmishra.voela.data.youtube

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Transcodes the already-extracted local audio to the chosen format/quality with the
// bundled ffmpeg, then copies it into the public Music/Voela folder. Runs as a
// foreground job so it survives the app closing.
class SaveAudioWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val notifications = ExtractionNotifications(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo = notifications.savingForegroundInfo(0)

    override suspend fun doWork(): Result {
        val inputPath = inputData.getString(Extraction.KEY_INPUT_PATH) ?: return Result.failure()
        val bitrate = inputData.getString(Extraction.KEY_BITRATE)
        val mime = inputData.getString(Extraction.KEY_MIME) ?: "audio/*"
        val extension = inputData.getString(Extraction.KEY_EXTENSION) ?: "m4a"
        val title = inputData.getString(Extraction.KEY_TITLE).orEmpty().ifBlank { "audio" }

        setForeground(notifications.savingForegroundInfo(0))
        return try {
            withContext(Dispatchers.IO) {
                val input = File(inputPath)
                if (!input.exists()) error("Source audio missing")

                val tmpDir = File(applicationContext.cacheDir, "save").apply {
                    mkdirs()
                    listFiles()?.forEach { it.delete() }
                }
                val output = File(tmpDir, "audio.$extension")
                val transcoded = when (extension) {
                    "m4a" -> AudioTranscoder.toAac(applicationContext, input, output)
                    "wav" -> AudioTranscoder.toWav(input, output)
                    "mp3" -> Mp3Encoder.encode(input, output, bitrate)
                    else -> false
                }
                if (!transcoded) error("Transcode failed")

                val displayName = "${sanitize(title)}.$extension"
                if (!MediaStoreSaver.save(applicationContext, output, displayName, mime)) error("Save failed")
                output.delete()

                notifications.showSaved(displayName)
                Result.success(workDataOf(Extraction.KEY_SAVED_NAME to displayName))
            }
        } catch (t: Throwable) {
            Log.e("SaveAudioWorker", "Save failed", t)
            Result.failure(workDataOf(Extraction.KEY_ERROR to (t.message ?: "Save failed")))
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().take(120).ifBlank { "audio" }
}
