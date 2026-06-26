package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume

// Transcodes the local extracted file (no re-download). M4A/AAC goes through Media3
// Transformer (the OS encoder — safe on 16KB devices); WAV is raw PCM we write
// ourselves. MP3 is handled separately by the LAME encoder.
@androidx.annotation.OptIn(UnstableApi::class)
object AudioTranscoder {

    suspend fun toAac(context: Context, input: File, output: File): Boolean =
        suspendCancellableCoroutine { cont ->
            val thread = HandlerThread("voela-transformer").apply { start() }
            Handler(thread.looper).post {
                var resumed = false
                fun finish(success: Boolean) {
                    thread.quitSafely()
                    if (!resumed && cont.isActive) { resumed = true; cont.resume(success) }
                }
                try {
                    val transformer = Transformer.Builder(context)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, result: ExportResult) = finish(true)
                            override fun onError(composition: Composition, result: ExportResult, exception: ExportException) = finish(false)
                        })
                        .build()
                    val edited = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(input)))
                        .setRemoveVideo(true)
                        .build()
                    transformer.start(edited, output.absolutePath)
                } catch (t: Throwable) {
                    finish(false)
                }
            }
            cont.invokeOnCancellation { thread.quitSafely() }
        }

    // Decode to 16-bit PCM and wrap it in a WAV container (no encoder needed)
    fun toWav(input: File, output: File, onProgress: (Int) -> Unit = {}): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(input.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return false
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()

            RandomAccessFile(output, "rw").use { raf ->
                raf.setLength(0)
                raf.write(ByteArray(44)) // header placeholder, patched at the end
                var pcmBytes = 0L
                var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                var buffer = ByteArray(0) // reused across buffers, grown on demand
                var lastPercent = -1

                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                while (!outputDone) {
                    if (!inputDone) {
                        val inIndex = codec.dequeueInputBuffer(10_000)
                        if (inIndex >= 0) {
                            val size = extractor.readSampleData(codec.getInputBuffer(inIndex)!!, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                if (durationUs > 0) {
                                    val percent = (extractor.sampleTime * 100 / durationUs).toInt().coerceIn(0, 99)
                                    if (percent != lastPercent) { lastPercent = percent; onProgress(percent) }
                                }
                                extractor.advance()
                            }
                        }
                    }
                    val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                    if (outIndex >= 0) {
                        if (info.size > 0) {
                            val buf = codec.getOutputBuffer(outIndex)!!
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            if (buffer.size < info.size) buffer = ByteArray(info.size)
                            buf.get(buffer, 0, info.size)
                            raf.write(buffer, 0, info.size)
                            pcmBytes += info.size
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val out = codec.outputFormat
                        sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
                codec.stop()
                codec.release()
                writeWavHeader(raf, pcmBytes, sampleRate, channels)
            }
            onProgress(100)
            output.length() > 44
        } catch (t: Throwable) {
            false
        } finally {
            extractor.release()
        }
    }

    private fun writeWavHeader(raf: RandomAccessFile, pcmBytes: Long, sampleRate: Int, channels: Int) {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt((36 + pcmBytes).toInt())
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * bitsPerSample / 8).toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(pcmBytes.toInt())
        raf.seek(0)
        raf.write(header.array())
    }
}
