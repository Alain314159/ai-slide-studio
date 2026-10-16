package com.aislide.studio.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.string
import androidx.datastore.preferences.preferencesDataStore
import com.aislide.studio.data.remote.LlmProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent settings storage.
 *
 * SECURITY NOTE (Rule 7 of android skill): Nothing in the APK is a secret.
 * The API key is stored in DataStore (plain shared preferences — NOT in BuildConfig,
 * NOT in strings.xml, NOT in NDK). For v0.1 personal use, this is acceptable
 * because the user provides their own key.
 *
 * For a production release, this should be encrypted with Jetpack Security:
 * `EncryptedSharedPreferences` or `MasterKey` + `Tink` encrypted DataStore.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_slide_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val apiKey: Flow<String> = context.dataStore.data.map { it[KEY_API_KEY] ?: "" }
    val providerId: Flow<String> = context.dataStore.data.map { it[KEY_PROVIDER_ID] ?: LlmProvider.GLM.id }
    val endpoint: Flow<String> = context.dataStore.data.map { it[KEY_ENDPOINT] ?: LlmProvider.GLM.defaultEndpoint }
    val model: Flow<String> = context.dataStore.data.map { it[KEY_MODEL] ?: LlmProvider.GLM.defaultModel }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[KEY_API_KEY] = value }
    }
    suspend fun setProviderId(value: String) {
        context.dataStore.edit { it[KEY_PROVIDER_ID] = value }
    }
    suspend fun setEndpoint(value: String) {
        context.dataStore.edit { it[KEY_ENDPOINT] = value }
    }
    suspend fun setModel(value: String) {
        context.dataStore.edit { it[KEY_MODEL] = value }
    }

    companion object {
        private val KEY_API_KEY = string("api_key")
        private val KEY_PROVIDER_ID = string("provider_id")
        private val KEY_ENDPOINT = string("endpoint")
        private val KEY_MODEL = string("model")
    }
}
