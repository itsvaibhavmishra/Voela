package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Transcodes a local audio file to the chosen format/quality (Media3 for AAC,
// LAME for MP3, MediaCodec for WAV) and saves it under Music/<subPath>. Runs as a
// foreground job so it survives the app closing. Reusable by any feature.
class AudioSaveWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val notifications = ProcessingNotifications(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo = notifications.savingForegroundInfo(0)

    override suspend fun doWork(): Result {
        val inputPath = inputData.getString(AudioSave.KEY_INPUT_PATH) ?: return Result.failure()
        val bitrate = inputData.getString(AudioSave.KEY_BITRATE)
        val mime = inputData.getString(AudioSave.KEY_MIME) ?: "audio/*"
        val extension = inputData.getString(AudioSave.KEY_EXTENSION) ?: "m4a"
        val title = inputData.getString(AudioSave.KEY_TITLE).orEmpty().ifBlank { "audio" }
        val subPath = inputData.getString(AudioSave.KEY_SUBPATH) ?: VoelaStorage.youtubeDownloads
        val savedLabel = inputData.getString(AudioSave.KEY_SAVED_LABEL)

        setForeground(notifications.savingForegroundInfo(0))
        return try {
            withContext(Dispatchers.IO) {
                val input = File(inputPath)
                if (!input.exists()) error("Source audio missing")

                val displayName = "${VoelaStorage.sanitize(title)}.$extension"
                val alreadyAac = input.extension.equals("m4a", ignoreCase = true) ||
                    input.extension.equals("mp4", ignoreCase = true)

                if (extension == "m4a" && alreadyAac) {
                    // Source is already AAC — save it as-is, no re-encoding (instant)
                    if (!MediaStoreSaver.save(applicationContext, input, displayName, mime, subPath)) error("Save failed")
                } else {
                    val tmpDir = File(applicationContext.cacheDir, "save").apply {
                        mkdirs()
                        listFiles()?.forEach { it.delete() }
                    }
                    val output = File(tmpDir, "audio.$extension")
                    var lastPercent = -1
                    val onProgress = { percent: Int ->
                        if (percent != lastPercent) {
                            lastPercent = percent
                            setProgressAsync(workDataOf(AudioSave.KEY_PROGRESS to percent))
                            notifications.updateSaving(percent)
                        }
                    }
                    val transcoded = when (extension) {
                        "m4a" -> AudioTranscoder.toAac(applicationContext, input, output)
                        "wav" -> AudioTranscoder.toWav(input, output, onProgress)
                        "mp3" -> Mp3Encoder.encode(input, output, bitrate, onProgress)
                        else -> false
                    }
                    if (!transcoded) error("Transcode failed")
                    if (!MediaStoreSaver.save(applicationContext, output, displayName, mime, subPath)) error("Save failed")
                    output.delete()
                }

                notifications.showSaved(displayName, savedLabel)
                Result.success(workDataOf(AudioSave.KEY_SAVED_NAME to displayName))
            }
        } catch (t: Throwable) {
            Log.e("AudioSaveWorker", "Save failed", t)
            Result.failure(workDataOf(AudioSave.KEY_ERROR to (t.message ?: "Save failed")))
        }
    }
}
