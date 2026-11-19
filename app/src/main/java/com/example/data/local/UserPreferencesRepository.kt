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
const val FIXED_REMOTE_CONFIG_URL =
    "https://gist.githubusercontent.com/ymnyeamin-sys/798b5138b7a6d2bb994657ac18a29abd/raw/velotube-config.json"
const val PINNED_REMOTE_CONFIG_URL =
    "https://gist.githubusercontent.com/ymnyeamin-sys/798b5138b7a6d2bb994657ac18a29abd/raw/80c6068597c44a85faca73bd03e525d73a812f0a/velotube-config.json"
const val REMOTE_CONFIG_GIST_API_URL =
    "https://api.github.com/gists/798b5138b7a6d2bb994657ac18a29abd"

const val FIXED_APP_UPDATE_URL =
    "https://gist.githubusercontent.com/ymnyeamin-sys/88048ef1878947e0dd82de124cb3625d/raw/update.json"
const val PINNED_APP_UPDATE_URL =
    "https://gist.githubusercontent.com/ymnyeamin-sys/88048ef1878947e0dd82de124cb3625d/raw/e480f790228650d93e8e010d8551bed985a53a11/update.json"
const val APP_UPDATE_GIST_API_URL =
    "https://api.github.com/gists/88048ef1878947e0dd82de124cb3625d"

const val DEFAULT_REGION_CODE = "BD"

data class UserPreferences(
    val autoExportToGallery: Boolean = true,
    val backgroundPlayEnabled: Boolean = true,
    val preferredQuality: String = "360",
    val regionCode: String = DEFAULT_REGION_CODE,
    val downloadApiBaseUrl: String = DEFAULT_DOWNLOAD_API_BASE_URL,
    val remoteConfigUrl: String = FIXED_REMOTE_CONFIG_URL,
    val youtubeApiKeyOverride: String = "",
    val acknowledgedUpdateVersion: String = ""
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val AUTO_EXPORT_GALLERY = booleanPreferencesKey("auto_export_gallery")
        val BACKGROUND_PLAY_ENABLED = booleanPreferencesKey("background_play_enabled")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val REGION_CODE = stringPreferencesKey("region_code")
        val DOWNLOAD_API_BASE_URL = stringPreferencesKey("download_api_base_url")
        val YOUTUBE_API_KEY_OVERRIDE = stringPreferencesKey("youtube_api_key_override")
        val ACKNOWLEDGED_UPDATE_VERSION = stringPreferencesKey("acknowledged_update_version")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            autoExportToGallery = prefs[Keys.AUTO_EXPORT_GALLERY] ?: true,
            backgroundPlayEnabled = prefs[Keys.BACKGROUND_PLAY_ENABLED] ?: true,
            preferredQuality = prefs[Keys.PREFERRED_QUALITY] ?: "360",
            regionCode = prefs[Keys.REGION_CODE] ?: DEFAULT_REGION_CODE,
            downloadApiBaseUrl = (prefs[Keys.DOWNLOAD_API_BASE_URL] ?: DEFAULT_DOWNLOAD_API_BASE_URL)
                .trim()
                .trimEnd('/')
                .ifBlank { DEFAULT_DOWNLOAD_API_BASE_URL },
            remoteConfigUrl = FIXED_REMOTE_CONFIG_URL,
            youtubeApiKeyOverride = prefs[Keys.YOUTUBE_API_KEY_OVERRIDE].orEmpty().trim(),
            acknowledgedUpdateVersion = prefs[Keys.ACKNOWLEDGED_UPDATE_VERSION].orEmpty().trim()
        )
    }

    suspend fun setAutoExportToGallery(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_EXPORT_GALLERY] = enabled }
    }

    suspend fun setBackgroundPlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_PLAY_ENABLED] = enabled }
    }

    suspend fun setRegionCode(region: String) {
        context.dataStore.edit { it[Keys.REGION_CODE] = region }
    }

    suspend fun setAcknowledgedUpdateVersion(version: String) {
        val normalized = version.trim().removePrefix("v").removePrefix("V")
        context.dataStore.edit { it[Keys.ACKNOWLEDGED_UPDATE_VERSION] = normalized }
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
