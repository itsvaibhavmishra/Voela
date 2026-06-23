package com.vaibhawmishra.voela.data.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.naman14.androidlame.AndroidLame
import com.naman14.androidlame.LameBuilder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder

// Encodes the local file to MP3 with the bundled LAME (Android has no native MP3
// encoder). Decodes to PCM via MediaCodec and feeds it straight to LAME — no re-download.
object Mp3Encoder {

    fun encode(input: File, output: File, bitrate: String?, onProgress: (Int) -> Unit = {}): Boolean {
        val kbps = bitrate?.removeSuffix("k")?.removeSuffix("K")?.toIntOrNull() ?: 320
        val extractor = MediaExtractor()
        var lame: AndroidLame? = null
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

            val mp3Buf = ByteArray(32 * 1024)
            var shorts = ShortArray(0) // reused across buffers, grown on demand
            var channels = 2
            var lastPercent = -1
            FileOutputStream(output).buffered(64 * 1024).use { out ->
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
                        if (lame == null) {
                            val out0 = codec.outputFormat
                            channels = out0.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            val sampleRate = out0.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            lame = LameBuilder()
                                .setInSampleRate(sampleRate)
                                .setOutSampleRate(sampleRate)
                                .setOutChannels(channels)
                                .setOutBitrate(kbps)
                                .setQuality(5)
                                .build()
                        }
                        if (info.size > 0) {
                            val buf = codec.getOutputBuffer(outIndex)!!
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            val needed = info.size / 2
                            if (shorts.size < needed) shorts = ShortArray(needed)
                            buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts, 0, needed)
                            val samplesPerChannel = needed / channels
                            val encoded = lame!!.encodeBufferInterLeaved(shorts, samplesPerChannel, mp3Buf)
                            if (encoded > 0) out.write(mp3Buf, 0, encoded)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
                lame?.flush(mp3Buf)?.let { if (it > 0) out.write(mp3Buf, 0, it) }
            }
            lame?.close()
            codec.stop()
            codec.release()
            onProgress(100)
            output.length() > 0
        } catch (t: Throwable) {
            Log.e("Mp3Encoder", "encode failed", t)
            false
        } finally {
            extractor.release()
        }
    }
}
