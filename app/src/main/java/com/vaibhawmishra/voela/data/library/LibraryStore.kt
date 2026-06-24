package com.vaibhawmishra.voela.data.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.libraryDataStore by preferencesDataStore(name = "library")

// Tracks kept actions (newest first) as a small JSON blob, with their files in
// app-private storage. The list is reconciled against disk on read so an item whose
// files were removed (manually or by expiry) simply disappears.
class LibraryStore(private val context: Context) {

    private val key = stringPreferencesKey("items")

    private val root: File get() = File(context.getExternalFilesDir(null), "library")
    fun dir(id: String): File = File(root, id)

    val items: Flow<List<LibraryItem>> =
        context.libraryDataStore.data.map { decode(it[key]).filter { item -> dir(item.id).exists() } }

    // Files for an item
    fun extractionFile(item: LibraryItem): File = File(dir(item.id), item.mediaName)
    fun vocalsFile(id: String): File = File(dir(id), "vocals.wav")
    fun instrumentalFile(id: String): File = File(dir(id), "instrumental.wav")

    // Copy a freshly-extracted audio file into the library and record it.
    suspend fun addExtraction(source: File, title: String, durationMs: Long): String =
        withContext(Dispatchers.IO) {
            val id = newId()
            val ext = source.extension.ifBlank { "m4a" }
            val media = "audio.$ext"
            dir(id).mkdirs()
            source.copyTo(File(dir(id), media), overwrite = true)
            val now = System.currentTimeMillis()
            prepend(LibraryItem(id, LibraryType.EXTRACTION, title, now, now, durationMs, 0, media))
            id
        }

    // Allocate a fresh library folder for a split; the separation worker writes its stems
    // straight here, then calls recordSplit() so nothing is copied and the record only
    // appears once the files exist.
    fun allocateDir(): String = newId().also { dir(it).mkdirs() }

    suspend fun recordSplit(id: String, title: String, durationMs: Long, elapsedMs: Long) {
        val now = System.currentTimeMillis()
        prepend(LibraryItem(id, LibraryType.VOCAL_SPLIT, title, now, now, durationMs, elapsedMs, ""))
    }

    // Total bytes a kept item occupies on disk.
    fun folderSize(id: String): Long =
        dir(id).listFiles()?.sumOf { it.length() } ?: 0L

    // Remove one item: drop its record and delete its files.
    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) { dir(id).deleteRecursively() }
        context.libraryDataStore.edit { prefs ->
            prefs[key] = encode(decode(prefs[key]).filterNot { it.id == id })
        }
    }

    // Remove everything.
    suspend fun clear() {
        withContext(Dispatchers.IO) { root.deleteRecursively() }
        context.libraryDataStore.edit { it.remove(key) }
    }

    private suspend fun prepend(item: LibraryItem) {
        context.libraryDataStore.edit { prefs ->
            prefs[key] = encode(listOf(item) + decode(prefs[key]))
        }
    }

    private fun newId(): String = System.currentTimeMillis().toString()

    private fun encode(items: List<LibraryItem>): String =
        JSONArray().apply {
            items.forEach {
                put(
                    JSONObject()
                        .put("id", it.id).put("ty", it.type.name).put("t", it.title)
                        .put("c", it.createdAt).put("o", it.lastOpenedAt)
                        .put("d", it.durationMs).put("e", it.elapsedMs).put("m", it.mediaName),
                )
            }
        }.toString()

    private fun decode(json: String?): List<LibraryItem> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                LibraryItem(
                    id = o.getString("id"),
                    type = runCatching { LibraryType.valueOf(o.getString("ty")) }.getOrDefault(LibraryType.VOCAL_SPLIT),
                    title = o.optString("t"),
                    createdAt = o.optLong("c"),
                    lastOpenedAt = o.optLong("o"),
                    durationMs = o.optLong("d"),
                    elapsedMs = o.optLong("e"),
                    mediaName = o.optString("m"),
                )
            }
        }.getOrDefault(emptyList())
    }
}
