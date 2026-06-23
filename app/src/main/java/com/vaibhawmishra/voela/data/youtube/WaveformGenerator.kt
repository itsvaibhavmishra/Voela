package com.vaibhawmishra.voela.data.youtube

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.abs

// Builds a fixed number of normalized amplitude bars (0..1) for the waveform UI.
// Rather than decoding the whole file (slow, esp. on software codecs), it seeks to
// evenly-spaced points and decodes one short window at each — fast, and still
// represents the entire track.
object WaveformGenerator {

    suspend fun generate(path: String, bars: Int = 56): List<Float> = withContext(Dispatchers.Default) {
        if (path.isBlank()) return@withContext flat(bars)
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext flat(bars)

            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            if (durationUs <= 0) return@withContext flat(bars)

            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val raw = FloatArray(bars)
            for (i in 0 until bars) {
                extractor.seekTo(durationUs * i / bars, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                codec.flush()
                raw[i] = decodePeak(extractor, codec, info)
            }
            codec.stop()
            codec.release()
            normalize(raw)
        } catch (t: Throwable) {
            flat(bars)
        } finally {
            extractor.release()
        }
    }

    // Decode just enough at the current position to read one PCM window's peak
    private fun decodePeak(extractor: MediaExtractor, codec: MediaCodec, info: MediaCodec.BufferInfo): Float {
        var inputDone = false
        repeat(40) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(5_000)
                if (inIndex >= 0) {
                    val inBuf = codec.getInputBuffer(inIndex)!!
                    val size = extractor.readSampleData(inBuf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 5_000)
            if (outIndex >= 0) {
                var peak = 0f
                if (info.size > 0) {
                    val outBuf = codec.getOutputBuffer(outIndex)!!
                    outBuf.position(info.offset)
                    outBuf.limit(info.offset + info.size)
                    val samples = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    var p = 0
                    while (samples.hasRemaining()) {
                        val v = abs(samples.get().toInt())
                        if (v > p) p = v
                    }
                    peak = p / 32768f
                }
                codec.releaseOutputBuffer(outIndex, false)
                if (peak > 0f || info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return peak
            }
        }
        return 0f
    }

    private fun normalize(raw: FloatArray): List<Float> {
        val maxV = raw.maxOrNull()?.coerceAtLeast(0.0001f) ?: 1f
        return raw.map { (it / maxV).coerceIn(0.06f, 1f) }
    }

    private fun flat(bars: Int) = List(bars) { 0.25f }
}
