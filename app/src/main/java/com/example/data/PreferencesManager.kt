package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.msnDataStore: DataStore<Preferences> by preferencesDataStore(name = "msn_prefs")

class PreferencesManager(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("language_code")
    private val keySound = booleanPreferencesKey("sound_enabled")
    private val keyVibrate = booleanPreferencesKey("vibration_enabled")

    private val defaultLanguage: String = run {
        val lang = Locale.getDefault().language
        if (lang in setOf("hu", "en", "de", "fr", "es", "it", "nl", "pl", "cs", "sk", "ro", "tr", "ru", "sv", "no", "da", "fi", "el", "ar", "he", "ja", "ko")) {
            lang
        } else {
            "hu"
        }
    }

    val languageCode: Flow<String> = context.msnDataStore.data.map { prefs ->
        prefs[keyLanguage] ?: defaultLanguage
    }

    val soundEnabled: Flow<Boolean> = context.msnDataStore.data.map { prefs ->
        prefs[keySound] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.msnDataStore.data.map { prefs ->
        prefs[keyVibrate] ?: true
    }

    suspend fun setLanguageCode(code: String) {
        context.msnDataStore.edit { it[keyLanguage] = code }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.msnDataStore.edit { it[keySound] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.msnDataStore.edit { it[keyVibrate] = enabled }
    }
}
