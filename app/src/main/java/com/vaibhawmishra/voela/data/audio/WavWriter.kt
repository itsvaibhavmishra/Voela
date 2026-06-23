package com.vaibhawmishra.voela.data.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Streams interleaved float PCM to a 16-bit WAV, appending chunk by chunk so a long
// track never needs to be held in memory all at once. The header is patched on close.
class WavWriter(file: File, private val channels: Int, private val rate: Int) {

    private val raf = RandomAccessFile(file, "rw")
    private var pcmBytes = 0L
    private var buffer = ByteArray(0)

    init {
        raf.setLength(0)
        raf.write(ByteArray(44)) // header placeholder
    }

    // Writes count frames starting at frame `offset` from an interleaved float array.
    fun append(interleaved: FloatArray, offset: Int, count: Int) {
        val samples = count * channels
        if (buffer.size < samples * 2) buffer = ByteArray(samples * 2)
        var b = 0
        val start = offset * channels
        for (i in 0 until samples) {
            val v = interleaved[start + i].coerceIn(-1f, 1f)
            val s = (v * 32767f).toInt()
            buffer[b++] = (s and 0xFF).toByte()
            buffer[b++] = ((s shr 8) and 0xFF).toByte()
        }
        raf.write(buffer, 0, b)
        pcmBytes += b
    }

    fun close() {
        val byteRate = rate * channels * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt((36 + pcmBytes).toInt())
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(channels.toShort())
        header.putInt(rate)
        header.putInt(byteRate)
        header.putShort((channels * 2).toShort())
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(pcmBytes.toInt())
        raf.seek(0)
        raf.write(header.array())
        raf.close()
    }
}
