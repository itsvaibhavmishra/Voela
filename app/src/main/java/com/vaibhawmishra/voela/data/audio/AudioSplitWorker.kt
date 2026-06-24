package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Foreground job: cut the [start, end] selection into equal clips and save each to
// Music/Voela/Audio Splits/{name}_{stamp}/Clip N. Cutting is a fast stream-copy.
class AudioSplitWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val notifications = ProcessingNotifications(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo = notifications.savingForegroundInfo(0)

    override suspend fun doWork(): Result {
        val source = inputData.getString(AudioSplit.KEY_SOURCE) ?: return Result.failure()
        val startMs = inputData.getLong(AudioSplit.KEY_START_MS, 0)
        var endMs = inputData.getLong(AudioSplit.KEY_END_MS, 0)
        val segmentMs = inputData.getLong(AudioSplit.KEY_SEGMENT_MS, 0)
        val title = inputData.getString(AudioSplit.KEY_TITLE).orEmpty().ifBlank { "Audio" }
        val ext = inputData.getString(AudioSplit.KEY_EXTENSION) ?: "m4a"
        val bitrate = inputData.getString(AudioSplit.KEY_BITRATE)
        val mime = inputData.getString(AudioSplit.KEY_MIME) ?: "audio/mp4"

        setForeground(notifications.savingForegroundInfo(0))
        return try {
            withContext(Dispatchers.IO) {
                if (endMs <= startMs) endMs = AudioMetadataReader.read(applicationContext, source).durationMs
                val clips = AudioSplitPlan.parts(endMs - startMs, segmentMs)
                if (clips.isEmpty()) error("Nothing to split")

                val stamp = SimpleDateFormat("HHmmss", Locale.US).format(Date())
                // Drop a trailing audio extension so the folder reads as a clean song name
                val cleanName = title.replace(Regex("\\.(m4a|mp3|wav|aac|m4b|ogg|opus|flac|webm)$", RegexOption.IGNORE_CASE), "")
                val subPath = VoelaStorage.audioSplits(cleanName, stamp)
                val tmpDir = File(applicationContext.cacheDir, "audiosplit").apply {
                    mkdirs(); listFiles()?.forEach { it.delete() }
                }

                clips.forEachIndexed { i, clip ->
                    // Cut the range losslessly to m4a, then convert to the chosen format if needed.
                    val cut = File(tmpDir, "cut_${i + 1}.m4a")
                    if (!AudioCutter.cut(applicationContext, source, startMs + clip.startMs, startMs + clip.endMs, cut) || !cut.exists()) {
                        error("Could not cut clip ${i + 1}")
                    }
                    val displayName = "Clip ${i + 1}.$ext"
                    val toSave = when (ext) {
                        "m4a" -> cut
                        "mp3" -> File(tmpDir, displayName).also { if (!Mp3Encoder.encode(cut, it, bitrate)) error("Encode failed (clip ${i + 1})") }
                        "wav" -> File(tmpDir, displayName).also { if (!AudioTranscoder.toWav(cut, it)) error("Encode failed (clip ${i + 1})") }
                        else -> cut
                    }
                    if (!MediaStoreSaver.save(applicationContext, toSave, displayName, mime, subPath)) {
                        error("Could not save clip ${i + 1}")
                    }
                    cut.delete()
                    if (toSave != cut) toSave.delete()
                    report(((i + 1) * 100 / clips.size).coerceIn(1, 100))
                }
                notifications.showSaved(applicationContext.getString(com.vaibhawmishra.voela.R.string.audiosplit_saved, clips.size))
                Result.success(workDataOf(AudioSplit.KEY_COUNT to clips.size))
            }
        } catch (t: Throwable) {
            Log.e("AudioSplit", "split failed", t)
            Result.failure(workDataOf(AudioSplit.KEY_ERROR to (t.message ?: "Split failed")))
        }
    }

    private fun report(percent: Int) {
        setProgressAsync(workDataOf(AudioSplit.KEY_PROGRESS to percent))
        notifications.updateSaving(percent)
    }
}
