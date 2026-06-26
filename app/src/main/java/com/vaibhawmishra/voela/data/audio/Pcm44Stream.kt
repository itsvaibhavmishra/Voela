package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.Closeable
import java.nio.ByteOrder

// Forward-streaming decoder that yields interleaved stereo float PCM at 44.1 kHz,
// resampling (linear) and up-mixing mono on the fly. Holds only the codec's current
// output buffer in memory, so a long track never needs a full-length array.
//
// Used to feed DTTNet its fixed-size windows without loading the whole selection.
class Pcm44Stream(context: Context, source: String, private val startMs: Long, endMs: Long) : Closeable {

    private val extractor = MediaExtractor()
    private val codec: MediaCodec
    private val info = MediaCodec.BufferInfo()
    private var srcRate: Int
    private var srcCh: Int
    private val endUs = endMs * 1000

    private var inputDone = false
    private var outputDone = false

    // Current decoded source buffer (interleaved source-rate samples) and read cursor.
    private var sb = FloatArray(0)
    private var sbPos = 0   // sample index into sb
    private var sbLen = 0

    // Two most recent source frames for interpolation (L/R), and the absolute index of `cur`.
    private var prevL = 0f; private var prevR = 0f
    private var curL = 0f; private var curR = 0f
    private var curIndex = -1L      // source-frame index currently held in cur (-1 before first)
    private var srcExhausted = false

    private var ratio = 1.0         // 44100 / srcRate
    private var outFrame = 0L       // next output frame to produce

    init {
        if (source.startsWith("content://")) extractor.setDataSource(context, Uri.parse(source), null)
        else extractor.setDataSource(source)
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        srcRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        srcCh = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        ratio = 44100.0 / srcRate
        extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()
    }

    // Writes up to `maxFrames` interleaved-stereo frames into `out`, starting at frame
    // `dstFrameOffset`. Returns the number of frames written (0 at end of stream).
    fun readInto(out: FloatArray, dstFrameOffset: Int, maxFrames: Int): Int {
        var produced = 0
        while (produced < maxFrames) {
            val p = outFrame / ratio          // source-frame position for this output frame
            val fi = p.toLong()
            // Ensure cur holds source frame fi+1 (prev = fi) so we can interpolate.
            while (curIndex < fi + 1) {
                if (!advanceSource()) break
            }
            if (srcExhausted && curIndex < fi + 1) break
            val frac = (p - fi).toFloat()
            val d = (dstFrameOffset + produced) * 2
            out[d] = prevL + (curL - prevL) * frac
            out[d + 1] = prevR + (curR - prevR) * frac
            outFrame++
            produced++
        }
        return produced
    }

    // Advance prev<-cur and read the next source frame into cur. Returns false at EOF.
    private fun advanceSource(): Boolean {
        if (srcExhausted) return false
        if (sbPos >= sbLen && !fillSourceBuffer()) { srcExhausted = true; return false }
        prevL = curL; prevR = curR
        curL = sb[sbPos]
        curR = if (srcCh >= 2) sb[sbPos + 1] else curL
        sbPos += srcCh
        curIndex++
        return true
    }

    // Pull the next non-empty decoded buffer from the codec into sb. Returns false at EOS.
    private fun fillSourceBuffer(): Boolean {
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
                var got = false
                if (info.size > 0) {
                    val buf = codec.getOutputBuffer(outIdx)!!
                    buf.position(info.offset)
                    buf.limit(info.offset + info.size)
                    val shorts = buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val n = shorts.remaining()
                    if (sb.size < n) sb = FloatArray(n)
                    var i = 0
                    while (shorts.hasRemaining()) sb[i++] = shorts.get() / 32768f
                    sbLen = n; sbPos = 0
                    got = true
                }
                val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(outIdx, false)
                if (eos) outputDone = true
                if (got) return true
            } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val of = codec.outputFormat
                srcRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                srcCh = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                ratio = 44100.0 / srcRate
            }
        }
        return false
    }

    override fun close() {
        try { codec.stop() } catch (_: Throwable) {}
        try { codec.release() } catch (_: Throwable) {}
        extractor.release()
    }
}
