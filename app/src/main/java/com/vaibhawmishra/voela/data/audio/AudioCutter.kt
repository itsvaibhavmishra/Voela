package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

// Cuts a [startMs, endMs] range of audio into its own m4a file. Tries a lossless
// stream-copy first (instant, no re-encode — works for AAC sources like our YouTube
// extractions); falls back to Media3 re-encoding for formats that can't be muxed.
@androidx.annotation.OptIn(UnstableApi::class)
object AudioCutter {

    suspend fun cut(context: Context, source: String, startMs: Long, endMs: Long, output: File): Boolean {
        if (streamCopy(context, source, startMs * 1000, endMs * 1000, output)) return true
        output.delete()
        return transcodeClip(context, source, startMs, endMs, output)
    }

    private fun streamCopy(context: Context, source: String, startUs: Long, endUs: Long, output: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            if (source.startsWith("content://")) extractor.setDataSource(context, Uri.parse(source), null)
            else extractor.setDataSource(source)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return false
            val format = extractor.getTrackFormat(track)
            // MediaMuxer (mp4) only accepts a few codecs (AAC). Bail for others -> fallback.
            if (format.getString(MediaFormat.KEY_MIME) != MimeTypes.AUDIO_AAC) return false
            extractor.selectTrack(track)

            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer.addTrack(format)
            muxer.start()

            val maxIn = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 1 shl 20
            val buffer = ByteBuffer.allocate(maxIn.coerceAtLeast(64 * 1024))
            val info = MediaCodec.BufferInfo()
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            var firstPtsUs = -1L
            var wrote = false
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break
                if (firstPtsUs < 0) firstPtsUs = sampleTime
                info.set(0, size, (sampleTime - firstPtsUs).coerceAtLeast(0), extractor.sampleFlags)
                muxer.writeSampleData(outTrack, buffer, info)
                wrote = true
                if (!extractor.advance()) break
            }
            muxer.stop()
            wrote
        } catch (t: Throwable) {
            false
        } finally {
            runCatching { muxer?.release() }
            extractor.release()
        }
    }

    private suspend fun transcodeClip(context: Context, source: String, startMs: Long, endMs: Long, output: File): Boolean =
        suspendCancellableCoroutine { cont ->
            val thread = HandlerThread("voela-cutter").apply { start() }
            Handler(thread.looper).post {
                var resumed = false
                fun finish(ok: Boolean) {
                    thread.quitSafely()
                    if (!resumed && cont.isActive) { resumed = true; cont.resume(ok) }
                }
                try {
                    val uri = if (source.startsWith("content://")) Uri.parse(source) else Uri.fromFile(File(source))
                    val item = MediaItem.Builder()
                        .setUri(uri)
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(startMs)
                                .setEndPositionMs(endMs)
                                .build(),
                        )
                        .build()
                    val edited = EditedMediaItem.Builder(item).setRemoveVideo(true).build()
                    Transformer.Builder(context)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, result: ExportResult) = finish(true)
                            override fun onError(composition: Composition, result: ExportResult, exception: ExportException) = finish(false)
                        })
                        .build()
                        .start(edited, output.absolutePath)
                } catch (t: Throwable) {
                    finish(false)
                }
            }
            cont.invokeOnCancellation { thread.quitSafely() }
        }
}
