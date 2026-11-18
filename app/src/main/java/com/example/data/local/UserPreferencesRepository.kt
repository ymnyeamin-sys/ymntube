package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "velotube_prefs")

const val DEFAULT_DOWNLOAD_API_BASE_URL = "https://ytdlp-production-4d018ba3.up.railway.app"

data class UserPreferences(
    val autoExportToGallery: Boolean = true,
    val preferredQuality: String = "360p",
    val regionCode: String = "US",
    val downloadApiBaseUrl: String = DEFAULT_DOWNLOAD_API_BASE_URL,
    val remoteConfigUrl: String = "",
    val youtubeApiKeyOverride: String = ""
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val AUTO_EXPORT_GALLERY = booleanPreferencesKey("auto_export_gallery")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val REGION_CODE = stringPreferencesKey("region_code")
        val DOWNLOAD_API_BASE_URL = stringPreferencesKey("download_api_base_url")
        val REMOTE_CONFIG_URL = stringPreferencesKey("remote_config_url")
        val YOUTUBE_API_KEY_OVERRIDE = stringPreferencesKey("youtube_api_key_override")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            autoExportToGallery = prefs[Keys.AUTO_EXPORT_GALLERY] ?: true,
            preferredQuality = prefs[Keys.PREFERRED_QUALITY] ?: "360p",
            regionCode = prefs[Keys.REGION_CODE] ?: "US",
            downloadApiBaseUrl = (prefs[Keys.DOWNLOAD_API_BASE_URL] ?: DEFAULT_DOWNLOAD_API_BASE_URL)
                .trim()
                .trimEnd('/')
                .ifBlank { DEFAULT_DOWNLOAD_API_BASE_URL },
            remoteConfigUrl = prefs[Keys.REMOTE_CONFIG_URL].orEmpty().trim(),
            youtubeApiKeyOverride = prefs[Keys.YOUTUBE_API_KEY_OVERRIDE].orEmpty().trim()
        )
    }

    suspend fun setAutoExportToGallery(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_EXPORT_GALLERY] = enabled }
    }

    suspend fun setPreferredQuality(quality: String) {
        context.dataStore.edit { it[Keys.PREFERRED_QUALITY] = quality }
    }

    suspend fun setRegionCode(region: String) {
        context.dataStore.edit { it[Keys.REGION_CODE] = region }
    }

    suspend fun setDownloadApiBaseUrl(url: String) {
        val normalized = url.trim().trimEnd('/').ifBlank { DEFAULT_DOWNLOAD_API_BASE_URL }
        context.dataStore.edit { it[Keys.DOWNLOAD_API_BASE_URL] = normalized }
    }

    suspend fun setRemoteConfigUrl(url: String) {
        context.dataStore.edit { it[Keys.REMOTE_CONFIG_URL] = url.trim() }
    }

    suspend fun applyRemoteConfig(
        downloadApiUrl: String?,
        youtubeApiKey: String?
    ) {
        context.dataStore.edit { prefs ->
            if (!downloadApiUrl.isNullOrBlank()) {
                prefs[Keys.DOWNLOAD_API_BASE_URL] = downloadApiUrl.trim().trimEnd('/')
            }
            if (!youtubeApiKey.isNullOrBlank()) {
                prefs[Keys.YOUTUBE_API_KEY_OVERRIDE] = youtubeApiKey.trim()
            }
        }
    }
}
