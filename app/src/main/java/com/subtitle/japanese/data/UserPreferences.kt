package com.subtitle.japanese.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_BAIDU_APP_ID = stringPreferencesKey("baidu_app_id")
        val KEY_BAIDU_SECRET_KEY = stringPreferencesKey("baidu_secret_key")
        val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        val KEY_OVERLAY_POSITION = stringPreferencesKey("overlay_position")
    }

    val baiduAppId: Flow<String> = context.dataStore.data.map { it[KEY_BAIDU_APP_ID] ?: "" }
    val baiduSecretKey: Flow<String> = context.dataStore.data.map { it[KEY_BAIDU_SECRET_KEY] ?: "" }
    val modelName: Flow<String> = context.dataStore.data.map { it[KEY_MODEL_NAME] ?: "ggml-tiny.bin" }
    val fontSize: Flow<String> = context.dataStore.data.map { it[KEY_FONT_SIZE] ?: "18" }
    val overlayPosition: Flow<String> = context.dataStore.data.map { it[KEY_OVERLAY_POSITION] ?: "bottom" }

    suspend fun saveBaiduCredentials(appId: String, secretKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BAIDU_APP_ID] = appId
            prefs[KEY_BAIDU_SECRET_KEY] = secretKey
        }
    }

    suspend fun saveModelName(name: String) {
        context.dataStore.edit { it[KEY_MODEL_NAME] = name }
    }

    suspend fun saveFontSize(size: String) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun saveOverlayPosition(position: String) {
        context.dataStore.edit { it[KEY_OVERLAY_POSITION] = position }
    }
}
