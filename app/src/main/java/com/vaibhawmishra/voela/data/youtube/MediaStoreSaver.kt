package com.vaibhawmishra.voela.data.youtube

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

// Saves a finished audio file into the public Music/Voela collection so it's
// visible to other apps. Uses scoped-storage MediaStore on Android 10+.
object MediaStoreSaver {

    private const val SUBFOLDER = "Voela"

    fun save(context: Context, source: File, displayName: String, mimeType: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(context, source, displayName, mimeType)
        } else {
            saveLegacy(context, source, displayName)
        }
    }

    private fun saveScoped(context: Context, source: File, displayName: String, mimeType: String): Boolean {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$SUBFOLDER")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return false
        resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out, 256 * 1024) } } ?: return false
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }

    // Pre-Q: write into the public Music dir (needs WRITE_EXTERNAL_STORAGE, declared maxSdk 28)
    private fun saveLegacy(context: Context, source: File, displayName: String): Boolean {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), SUBFOLDER).apply { mkdirs() }
        val dest = File(dir, displayName)
        source.copyTo(dest, overwrite = true)
        android.media.MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null, null)
        return true
    }
}
