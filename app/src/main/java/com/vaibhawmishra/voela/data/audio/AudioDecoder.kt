package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder

// Decodes a time range [startMs, endMs] of a local path or content Uri into
// interleaved float PCM in [-1, 1]. Capacity is pre-sized from the duration so we
// don't keep a second copy of a long track in memory.
object AudioDecoder {

    class Pcm(val samples: FloatArray, val frames: Int, val channels: Int, val rate: Int)

    fun decodeRange(context: Context, source: String, startMs: Long, endMs: Long): Pcm {
        val extractor = MediaExtractor()
        if (source.startsWith("content://")) extractor.setDataSource(context, Uri.parse(source), null)
        else extractor.setDataSource(source)
        try {
            val track = (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val endUs = endMs * 1000
            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val estSamples = (((endMs - startMs) / 1000.0 + 1) * rate).toInt().coerceAtLeast(rate) * channels
            var out = FloatArray(estSamples)
            var count = 0

            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val size = extractor.readSampleData(codec.getInputBuffer(inIdx)!!, 0)
                        if (size < 0 || extractor.sampleTime > endUs) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    if (info.size > 0) {
                        val buf = codec.getOutputBuffer(outIdx)!!
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        val shorts = buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val n = shorts.remaining()
                        if (count + n > out.size) out = out.copyOf((out.size * 2).coerceAtLeast(count + n))
                        var i = count
                        while (shorts.hasRemaining()) out[i++] = shorts.get() / 32768f
                        count += n
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
            codec.stop()
            codec.release()
            return Pcm(out, count / channels, channels, rate)
        } finally {
            extractor.release()
        }
    }
}
