package com.vaibhawmishra.voela.data.youtube

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf

// Foreground (dataSync) worker: runs the extraction off the UI, survives app
// close/swipe/process death, and reports progress via WorkInfo + notification.
class AudioExtractionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val notifications = ExtractionNotifications(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo = notifications.foregroundInfo(0)

    override suspend fun doWork(): Result {
        val url = inputData.getString(Extraction.KEY_URL) ?: return Result.failure()
        setForeground(notifications.foregroundInfo(0))
        return try {
            val result = YtDlpAudioExtractor(applicationContext).extract(url) { percent ->
                setProgressAsync(workDataOf(Extraction.KEY_PROGRESS to percent))
                notifications.updateProgress(percent)
            }
            notifications.showComplete(result.title)
            Result.success(
                workDataOf(
                    Extraction.KEY_OUTPUT_PATH to result.localPath,
                    Extraction.KEY_TITLE to result.title,
                    Extraction.KEY_SOURCE_URL to url,
                    Extraction.KEY_DURATION to result.durationSeconds,
                ),
            )
        } catch (t: Throwable) {
            Log.e("AudioExtractionWorker", "Extraction failed", t)
            Result.failure(workDataOf(Extraction.KEY_ERROR to (t.message ?: "Extraction failed")))
        }
    }
}
