package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vaibhawmishra.voela.data.library.LibraryStore
import com.vaibhawmishra.voela.data.settings.SettingsStore
import com.vaibhawmishra.voela.data.settings.StemFormat
import java.io.File
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Foreground job: separate the trimmed [start, end] region into vocals.wav +
// instrumental.wav.
//   Fast  -> Spleeter (sherpa-onnx), chunked with a little context each side.
//   Best  -> DTTNet, run at its fixed 44.1 kHz / 261120-sample window with
//            overlap-discard between windows; instrumental = mix - vocals.
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
        val best = inputData.getString(VocalSeparation.KEY_ENGINE) == VocalSeparation.ENGINE_BEST
        val title = inputData.getString(VocalSeparation.KEY_TITLE).orEmpty()
        // Vocal splits are auto-kept in the library; "audio" splits are throwaway (separation dir).
        val keep = inputData.getString(VocalSeparation.KEY_FEATURE) != "audio"

        setForeground(notifications.separatingForegroundInfo(0))
        var allocatedDir: File? = null
        return try {
            withContext(Dispatchers.IO) {
                if (endMs <= startMs) endMs = AudioMetadataReader.read(applicationContext, source).durationMs
                if (endMs <= startMs) error("Empty selection")

                val library = LibraryStore(applicationContext)
                val libId = if (keep) library.allocateDir() else ""
                val outDir = if (keep) library.dir(libId) else VocalSeparation.outputDir(applicationContext)
                if (keep) allocatedDir = outDir
                prepDir(outDir)

                // Actual end-to-end time: decode + separation + (kept-stem) encoding.
                val started = System.currentTimeMillis()
                if (best) separateDtt(source, startMs, endMs, outDir)
                else separateFast(source, startMs, endMs, outDir)
                if (keep) {
                    val format = SettingsStore(applicationContext).vocalFormat.first()
                    encodeStems(outDir, format)
                }
                val elapsed = System.currentTimeMillis() - started
                if (keep) library.recordSplit(libId, title, endMs - startMs, elapsed)
                report(100)
                Result.success(
                    workDataOf(VocalSeparation.KEY_ELAPSED_MS to elapsed, VocalSeparation.KEY_LIBRARY_ID to libId),
                )
            }
        } catch (t: Throwable) {
            Log.e("VocalSeparation", "separation failed", t)
            allocatedDir?.deleteRecursively() // don't leave an empty kept-split folder behind
            Result.failure(workDataOf(VocalSeparation.KEY_ERROR to (t.message ?: "Separation failed")))
        }
    }

    // --- Best: DTTNet -------------------------------------------------------

    private fun separateDtt(source: String, startMs: Long, endMs: Long, outDir: File): Long {
        val model = ensureDttModel()

        // Estimate window count for progress (DTTNet runs at 44.1 kHz).
        val approxFrames = (endMs - startMs) * 44100 / 1000
        val chunk = DttSeparator.CHUNK
        val step = chunk - 2 * DTT_TRIM
        val totalChunks = ((approxFrames + step - 1) / step).toInt().coerceAtLeast(1)

        val handle = DttSeparator.nativeCreate(model.absolutePath)
        if (handle == 0L) error("Engine init failed")

        // Stream the selection at 44.1 kHz stereo through a sliding window so memory stays
        // at ~one window (≈2 MB) regardless of track length. Each window's edges (DTT_TRIM
        // frames) are discarded and recomputed by the neighbouring window (overlap-discard).
        val stream = Pcm44Stream(applicationContext, source, startMs, endMs)
        val window = FloatArray(chunk * 2)
        var vocals: WavWriter? = null
        var instrumental: WavWriter? = null
        val t0 = System.currentTimeMillis()
        try {
            var first = true
            var c = 0
            var valid = stream.readInto(window, 0, chunk)
            while (valid > 0) {
                if (valid < chunk) java.util.Arrays.fill(window, valid * 2, chunk * 2, 0f)
                val voc = DttSeparator.nativeProcess(handle, window) ?: error("Separation failed")
                val last = valid < chunk

                if (vocals == null) {
                    vocals = WavWriter(File(outDir, "vocals.wav"), 2, 44100)
                    instrumental = WavWriter(File(outDir, "instrumental.wav"), 2, 44100)
                }
                val writeStart = if (first) 0 else DTT_TRIM
                val writeEnd = if (last) valid else chunk - DTT_TRIM
                val writeCount = writeEnd - writeStart
                if (writeCount > 0) {
                    val inst = FloatArray(writeCount * 2)
                    val base = writeStart * 2
                    for (i in 0 until writeCount * 2) inst[i] = window[base + i] - voc[base + i]
                    vocals!!.append(voc, writeStart, writeCount)
                    instrumental!!.append(inst, 0, writeCount)
                }

                report(((c + 1) * 95 / totalChunks).coerceIn(1, 95))
                if (last) break
                // Slide: keep the last 2*DTT_TRIM frames as the next window's left overlap.
                System.arraycopy(window, step * 2, window, 0, 2 * DTT_TRIM * 2)
                val got = stream.readInto(window, 2 * DTT_TRIM, step)
                valid = 2 * DTT_TRIM + got
                first = false
                c++
            }
        } finally {
            vocals?.close()
            instrumental?.close()
            stream.close()
            DttSeparator.nativeDestroy(handle)
        }
        return System.currentTimeMillis() - t0
    }

    // Copy the bundled DTTNet ONNX out of assets to a stable path (once). The model
    // ships inside the APK, so there's no network fallback.
    private fun ensureDttModel(): File {
        val model = VocalSeparation.dttModel(applicationContext)
        if (model.exists() && model.length() > 0) return model
        model.parentFile?.mkdirs()
        val tmp = File(model.parentFile, "${model.name}.part")
        try {
            applicationContext.assets.open(DTT_ASSET).use { input ->
                tmp.outputStream().use { input.copyTo(it, 256 * 1024) }
            }
            if (!tmp.renameTo(model)) error("Could not unpack model")
        } catch (t: Throwable) {
            tmp.delete()
            throw t
        }
        return model
    }

    // --- Fast: Spleeter -----------------------------------------------------

    private fun separateFast(source: String, startMs: Long, endMs: Long, outDir: File): Long {
        download(VOCALS_URL, VocalSeparation.vocalsModel(applicationContext))
        download(ACCOMP_URL, VocalSeparation.accompModel(applicationContext))
        val handle = SourceSeparator.nativeCreate(
            VocalSeparation.vocalsModel(applicationContext).absolutePath,
            VocalSeparation.accompModel(applicationContext).absolutePath,
            "",
            "cpu",
            2,
        )
        if (handle == 0L) error("Engine init failed")

        var vocals: WavWriter? = null
        var instrumental: WavWriter? = null
        val t0 = System.currentTimeMillis()
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
                val vocalsArr = stems[0]
                val instArr = if (stems.size >= 2) stems[1] else FloatArray(vocalsArr.size) {
                    (pcm.samples[it] - vocalsArr[it]).coerceIn(-1f, 1f)
                }

                if (vocals == null) {
                    vocals = WavWriter(File(outDir, "vocals.wav"), pcm.channels, pcm.rate)
                    instrumental = WavWriter(File(outDir, "instrumental.wav"), pcm.channels, pcm.rate)
                }
                val outFrames = vocalsArr.size / pcm.channels
                val skip = ((wStart - dStart) * pcm.rate / 1000).toInt().coerceIn(0, outFrames)
                val writeFrames = ((wEnd - wStart) * pcm.rate / 1000).toInt().coerceAtMost(outFrames - skip)
                vocals!!.append(vocalsArr, skip, writeFrames)
                instrumental!!.append(instArr, skip, writeFrames)

                report(((c + 1) * 95 / chunks).coerceIn(1, 95))
            }
        } finally {
            vocals?.close()
            instrumental?.close()
            SourceSeparator.nativeDestroy(handle)
        }
        return System.currentTimeMillis() - t0
    }

    // --- shared -------------------------------------------------------------

    private fun prepDir(dir: File) {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
    }

    // Stems are written as WAV (uncompressed ~10 MB/min/stem). Re-encode kept ones to the
    // user's chosen format to control library size vs quality. WAV keeps them as-is.
    private suspend fun encodeStems(dir: File, format: StemFormat) {
        if (format.extension == "wav") return
        for (name in listOf("vocals", "instrumental")) {
            val wav = File(dir, "$name.wav")
            if (!wav.exists()) continue
            val out = File(dir, "$name.${format.extension}")
            val ok = when (format.extension) {
                "m4a" -> AudioTranscoder.toAac(applicationContext, wav, out)
                "mp3" -> Mp3Encoder.encode(wav, out, format.bitrate)
                else -> false
            }
            if (ok && out.length() > 0) wav.delete() else out.delete()
        }
    }

    private fun report(percent: Int) {
        setProgressAsync(workDataOf(VocalSeparation.KEY_PROGRESS to percent))
        notifications.updateSeparating(percent)
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
        private const val CHUNK_MS = 30_000L // Spleeter
        private const val CTX_MS = 1_000L
        private const val DTT_TRIM = 4_096 // frames discarded at each window edge (overlap-discard)
        private const val BASE = "https://huggingface.co/csukuangfj/sherpa-onnx-spleeter-2stems-fp16/resolve/main"
        private const val VOCALS_URL = "$BASE/vocals.fp16.onnx"
        private const val ACCOMP_URL = "$BASE/accompaniment.fp16.onnx"
        private const val DTT_ASSET = "dttnet_vocals.onnx" // bundled in app/src/main/assets
    }
}
