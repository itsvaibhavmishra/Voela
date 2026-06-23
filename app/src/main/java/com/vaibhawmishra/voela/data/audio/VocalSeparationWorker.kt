package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Foreground job: separate the trimmed [start, end] region into vocals.wav +
// instrumental.wav. Processed in chunks (with a little context each side) so a long
// track stays within memory, streaming each chunk's stems straight to disk.
class VocalSeparationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val notifications = ProcessingNotifications(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo = notifications.separatingForegroundInfo(0)

    override suspend fun doWork(): Result {
        val source = inputData.getString(VocalSeparation.KEY_SOURCE) ?: return Result.failure()
        val startMs = inputData.getLong(VocalSeparation.KEY_START_MS, 0)
        var endMs = inputData.getLong(VocalSeparation.KEY_END_MS, 0)

        setForeground(notifications.separatingForegroundInfo(0))
        return try {
            withContext(Dispatchers.IO) {
                ensureModels()
                if (endMs <= startMs) endMs = AudioMetadataReader.read(applicationContext, source).durationMs
                if (endMs <= startMs) error("Empty selection")
                separate(source, startMs, endMs)
                report(100)
                Result.success()
            }
        } catch (t: Throwable) {
            Log.e("VocalSeparation", "separation failed", t)
            Result.failure(workDataOf(VocalSeparation.KEY_ERROR to (t.message ?: "Separation failed")))
        }
    }

    private fun separate(source: String, startMs: Long, endMs: Long) {
        val outDir = VocalSeparation.outputDir(applicationContext).apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val handle = SourceSeparator.nativeCreate(
            VocalSeparation.vocalsModel(applicationContext).absolutePath,
            VocalSeparation.accompModel(applicationContext).absolutePath,
            "",
        )
        if (handle == 0L) error("Engine init failed")

        var vocals: WavWriter? = null
        var instrumental: WavWriter? = null
        try {
            val chunks = ((endMs - startMs + CHUNK_MS - 1) / CHUNK_MS).toInt().coerceAtLeast(1)
            for (c in 0 until chunks) {
                val wStart = startMs + c * CHUNK_MS
                val wEnd = min(wStart + CHUNK_MS, endMs)
                val dStart = (wStart - CTX_MS).coerceAtLeast(startMs)
                val dEnd = min(wEnd + CTX_MS, endMs)

                val pcm = AudioDecoder.decodeRange(applicationContext, source, dStart, dEnd)
                if (pcm.frames == 0) continue
                val stems = SourceSeparator.nativeProcess(handle, pcm.samples, pcm.channels, pcm.frames, pcm.rate)
                    ?: error("Separation failed")

                if (vocals == null) {
                    vocals = WavWriter(File(outDir, "vocals.wav"), pcm.channels, pcm.rate)
                    instrumental = WavWriter(File(outDir, "instrumental.wav"), pcm.channels, pcm.rate)
                }
                val outFrames = stems[0].size / pcm.channels
                val skip = ((wStart - dStart) * pcm.rate / 1000).toInt().coerceIn(0, outFrames)
                val writeFrames = ((wEnd - wStart) * pcm.rate / 1000).toInt().coerceAtMost(outFrames - skip)
                vocals!!.append(stems[0], skip, writeFrames)
                instrumental!!.append(stems[1], skip, writeFrames)

                report(((c + 1) * 95 / chunks).coerceIn(1, 95))
            }
        } finally {
            vocals?.close()
            instrumental?.close()
            SourceSeparator.nativeDestroy(handle)
        }
    }

    private fun report(percent: Int) {
        setProgressAsync(workDataOf(VocalSeparation.KEY_PROGRESS to percent))
        notifications.updateSeparating(percent)
    }

    private fun ensureModels() {
        download(VOCALS_URL, VocalSeparation.vocalsModel(applicationContext))
        download(ACCOMP_URL, VocalSeparation.accompModel(applicationContext))
    }

    private fun download(url: String, dest: File) {
        if (dest.exists() && dest.length() > 0) return
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "${dest.name}.part")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 30_000
        }
        try {
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it, 256 * 1024) } }
            if (!tmp.renameTo(dest)) error("Could not save model")
        } finally {
            conn.disconnect()
            tmp.delete()
        }
    }

    companion object {
        private const val CHUNK_MS = 30_000L
        private const val CTX_MS = 1_000L
        private const val BASE = "https://huggingface.co/csukuangfj/sherpa-onnx-spleeter-2stems-fp16/resolve/main"
        private const val VOCALS_URL = "$BASE/vocals.fp16.onnx"
        private const val ACCOMP_URL = "$BASE/accompaniment.fp16.onnx"
    }
}
