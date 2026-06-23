package com.vaibhawmishra.voela.data.youtube

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vaibhawmishra.voela.ui.youtube.RecentLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.recentLinksDataStore by preferencesDataStore(name = "recent_links")

// Persists the most recent extracted links (newest first, deduped by url) so they
// survive restarts. Stored as a small JSON blob — no extra serialization dependency.
class RecentLinksStore(private val context: Context) {

    private val key = stringPreferencesKey("links")

    val recents: Flow<List<RecentLink>> =
        context.recentLinksDataStore.data.map { decode(it[key]) }

    suspend fun add(link: RecentLink) {
        context.recentLinksDataStore.edit { prefs ->
            val others = decode(prefs[key]).filterNot { it.url == link.url }
            prefs[key] = encode((listOf(link) + others).take(MAX))
        }
    }

    suspend fun clear() {
        context.recentLinksDataStore.edit { it.remove(key) }
    }

    private fun encode(links: List<RecentLink>): String =
        JSONArray().apply {
            links.forEach { put(JSONObject().put("t", it.title).put("d", it.duration).put("u", it.url)) }
        }.toString()

    private fun decode(json: String?): List<RecentLink> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                RecentLink(o.optString("t"), o.optString("d"), o.optString("u"))
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val MAX = 10
    }
}
