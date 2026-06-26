package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

data class AudioMetadata(val format: String, val durationMs: Long, val sizeBytes: Long, val sampleRateHz: Int)

// Reads display metadata (format, duration, size, sample rate) from a local path or content Uri
object AudioMetadataReader {

    fun read(context: Context, source: String): AudioMetadata {
        val uri = if (source.startsWith("content://")) Uri.parse(source) else null
        val retriever = MediaMetadataRetriever()
        val durationMs = try {
            if (uri != null) retriever.setDataSource(context, uri) else retriever.setDataSource(source)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (t: Throwable) {
            0L
        } finally {
            retriever.release()
        }
        return AudioMetadata(formatOf(context, source, uri), durationMs, sizeOf(context, source, uri), sampleRateOf(context, source, uri))
    }

    private fun sampleRateOf(context: Context, source: String, uri: Uri?): Int {
        val extractor = MediaExtractor()
        return try {
            if (uri != null) extractor.setDataSource(context, uri, null) else extractor.setDataSource(source)
            (0 until extractor.trackCount)
                .map { extractor.getTrackFormat(it) }
                .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?.takeIf { it.containsKey(MediaFormat.KEY_SAMPLE_RATE) }
                ?.getInteger(MediaFormat.KEY_SAMPLE_RATE) ?: 0
        } catch (t: Throwable) {
            0
        } finally {
            extractor.release()
        }
    }

    private fun formatOf(context: Context, source: String, uri: Uri?): String {
        val ext = if (uri == null) {
            File(source).extension
        } else {
            query(context, uri, OpenableColumns.DISPLAY_NAME)?.substringAfterLast('.', "")
                ?: context.contentResolver.getType(uri)?.substringAfterLast('/').orEmpty()
        }
        return ext.uppercase()
    }

    private fun sizeOf(context: Context, source: String, uri: Uri?): Long =
        if (uri == null) File(source).length() else query(context, uri, OpenableColumns.SIZE)?.toLongOrNull() ?: 0L

    private fun query(context: Context, uri: Uri, column: String): String? =
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use {
            if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
        }
}
