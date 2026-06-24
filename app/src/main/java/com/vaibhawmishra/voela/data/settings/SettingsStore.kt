package com.vaibhawmishra.voela.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

// App preferences. Vocal Split and Audio Split each keep their own output format.
class SettingsStore(private val context: Context) {

    private val vocalKey = stringPreferencesKey("vocal_format")
    private val audioKey = stringPreferencesKey("audio_format")

    val vocalFormat: Flow<StemFormat> = context.settingsDataStore.data.map { StemFormat.from(it[vocalKey]) }
    val audioFormat: Flow<StemFormat> = context.settingsDataStore.data.map { StemFormat.from(it[audioKey]) }

    suspend fun setVocalFormat(format: StemFormat) {
        context.settingsDataStore.edit { it[vocalKey] = format.key }
    }

    suspend fun setAudioFormat(format: StemFormat) {
        context.settingsDataStore.edit { it[audioKey] = format.key }
    }
}
