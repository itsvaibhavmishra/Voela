package com.vaibhawmishra.voela.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

// App preferences. Currently the output format for Vocal Split stems. (Audio Split
// picks its format per-save via a dialog, so it isn't a stored preference.)
class SettingsStore(private val context: Context) {

    private val vocalKey = stringPreferencesKey("vocal_format")
    private val accentKey = stringPreferencesKey("accent_color")

    val vocalFormat: Flow<StemFormat> = context.settingsDataStore.data.map { StemFormat.from(it[vocalKey]) }

    // Stored as the accent key; resolved to an Accent (Purple default) by the reader.
    val accentColor: Flow<String?> = context.settingsDataStore.data.map { it[accentKey] }

    suspend fun setVocalFormat(format: StemFormat) {
        context.settingsDataStore.edit { it[vocalKey] = format.key }
    }

    suspend fun setAccentColor(key: String) {
        context.settingsDataStore.edit { it[accentKey] = key }
    }
}
