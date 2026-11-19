package com.example.data.repository

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.BuildConfig
import com.example.data.local.APP_UPDATE_GIST_API_URL
import com.example.data.local.DownloadedVideoEntity
import com.example.data.local.FIXED_APP_UPDATE_URL
import com.example.data.local.FIXED_REMOTE_CONFIG_URL
import com.example.data.local.PINNED_APP_UPDATE_URL
import com.example.data.local.PINNED_REMOTE_CONFIG_URL
import com.example.data.local.REMOTE_CONFIG_GIST_API_URL
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VeloTubeDao
import com.example.data.model.ActiveDownload
import com.example.data.model.ApiResult
import com.example.data.model.AppUpdateInfo
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadJobStatus
import com.example.data.model.DownloadQualityResponse
import com.example.data.model.DownloadWithQualityBody
import com.example.data.model.RemoteAppConfig
import com.example.data.model.SelectedQualityInfo
import com.example.data.model.UrlRequestBody
import com.example.data.model.VideoItem
import com.example.data.remote.DownloaderApiService
import com.example.data.remote.FormatUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.util.UUID
import kotlin.coroutines.resume

data class ResolvedDownloadPackage(
    val video: VideoItem,
    val formats: List<DownloadFormat>
)

sealed interface AppUpdateCheckResult {
    data class Available(val info: AppUpdateInfo) : AppUpdateCheckResult
    data class Disabled(val version: String) : AppUpdateCheckResult
    data class UpToDate(val installedVersion: String, val remoteVersion: String) : AppUpdateCheckResult
    data class Error(val message: String) : AppUpdateCheckResult
}

internal data class ParsedVideoInfo(
    val title: String? = null,
    val author: String? = null,
    val durationSeconds: Long? = null,
    val viewCount: Long? = null,
    val thumbnailUrl: String? = null
)

internal data class ParsedResolutionOption(
    val requestQuality: String,
    val displayLabel: String,
    val sizeBytes: Long = 0L,
    val ext: String = "mp4",
    val isAudioOnly: Boolean = false,
    val directUrl: String? = null,
    val rawFormatId: String? = null
)

internal object DownloaderResponseParser {
    private val pureDigitsRegex = Regex("^(\\d+)$")
    private val heightPRegex = Regex("(\\d{2,4})p", RegexOption.IGNORE_CASE)
    private val wxhRegex = Regex("\\d{2,4}x(\\d{2,4})", RegexOption.IGNORE_CASE)

    fun extractHeight(quality: String): Int? {
        val trimmed = quality.trim()
        pureDigitsRegex.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        heightPRegex.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        wxhRegex.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        return null
    }

    fun formatDisplayQuality(raw: String): String {
        val trimmed = raw.trim()
        return if (pureDigitsRegex.matches(trimmed)) "${trimmed}p" else trimmed
    }

    fun buildQualityCandidates(quality: String, rawFormatId: String? = null): List<String> {
        val trimmed = quality.trim()
        val height = extractHeight(trimmed)
        return buildList {
            if (trimmed.isNotEmpty()) add(trimmed)
            if (height != null) {
                add(height.toString())
                add("${height}p")
            }
            if (!rawFormatId.isNullOrBlank()) {
                add(rawFormatId.trim())
            }
        }.distinct()
    }

    fun parseAvailableResolutions(rawJson: String): Pair<List<ParsedResolutionOption>, String?> {
        val trimmedJson = rawJson.trim()
        if (trimmedJson.startsWith("[")) {
            val arr = runCatching { JSONArray(trimmedJson) }.getOrNull()
                ?: return emptyList<ParsedResolutionOption>() to "Downloader API returned invalid JSON array."
            val collected = mutableListOf<ParsedResolutionOption>()
            extractOptionsFromArray(arr, collected)
            return collected.distinctBy { it.requestQuality.lowercase() } to null
        }

        val json = runCatching { JSONObject(trimmedJson) }.getOrNull()
            ?: return emptyList<ParsedResolutionOption>() to "Downloader API returned invalid JSON."

        val err = json.optString("error").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("message").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("detail").takeIf { it.isNotBlank() && it != "null" }

        val candidateArrays = listOfNotNull(
            json.optJSONArray("resolutions"),
            json.optJSONArray("all"),
            json.optJSONArray("progressive"),
            json.optJSONArray("formats")
        )

        val collected = mutableListOf<ParsedResolutionOption>()
        for (arr in candidateArrays) {
            extractOptionsFromArray(arr, collected)
            if (collected.isNotEmpty()) break
        }

        val deduped = collected.distinctBy { it.requestQuality.lowercase() }
        return deduped to err
    }

    private fun extractOptionsFromArray(arr: JSONArray, out: MutableList<ParsedResolutionOption>) {
        for (i in 0 until arr.length()) {
            val item = arr.opt(i) ?: continue
            when (item) {
                is String -> {
                    val trimmed = item.trim()
                    if (trimmed.isNotEmpty() && !trimmed.equals("highest", ignoreCase = true)) {
                        val isAudio = isAudioLabel(trimmed)
                        val reqQuality = normalizeRequestQuality(trimmed, null)
                        out.add(
                            ParsedResolutionOption(
                                requestQuality = reqQuality,
                                displayLabel = formatDisplayQuality(trimmed),
                                ext = if (isAudio) "m4a" else "mp4",
                                isAudioOnly = isAudio
                            )
                        )
                    }
                }
                is Number -> {
                    val h = item.toInt()
                    if (h > 0) {
                        val raw = h.toString()
                        out.add(
                            ParsedResolutionOption(
                                requestQuality = raw,
                                displayLabel = "${h}p"
                            )
                        )
                    }
                }
                is JSONObject -> {
                    val heightVal = item.optInt("height", 0).takeIf { it > 0 }
                    val rawText = item.optString("quality").takeIf { it.isNotBlank() && it != "null" }
                        ?: item.optString("label").takeIf { it.isNotBlank() && it != "null" }
                        ?: item.optString("resolution").takeIf { it.isNotBlank() && it != "null" }
                        ?: item.optString("format_note").takeIf { it.isNotBlank() && it != "null" }
                        ?: heightVal?.toString()

                    if (!rawText.isNullOrBlank()) {
                        val ext = item.optString("ext").takeIf { it.isNotBlank() && it != "null" }
                        val size = item.optLong("filesize", 0L)
                            .takeIf { it > 0L }
                            ?: item.optLong("filesize_approx", 0L)
                                .takeIf { it > 0L }
                            ?: item.optLong("size", 0L)
                        val directUrl = listOf(
                            "url",
                            "download_url",
                            "stream_url",
                            "video_url",
                            "download_endpoint",
                            "endpoint",
                            "path",
                            "href"
                        ).firstNotNullOfOrNull { key ->
                            item.optString(key).trim().takeIf {
                                it.isNotBlank() && it != "null" &&
                                    (it.startsWith("http") || it.startsWith("/"))
                            }
                        }
                        val rawFmtId = item.optString("format_id").takeIf { it.isNotBlank() && it != "null" }
                            ?: item.optString("itag").takeIf { it.isNotBlank() && it != "null" }
                            ?: item.optString("id").takeIf { it.isNotBlank() && it != "null" }
                        val isAudio = isAudioLabel(rawText) ||
                            item.optString("vcodec").equals("none", ignoreCase = true)
                        val reqQuality = normalizeRequestQuality(rawText, heightVal)
                        val resolvedHeight = heightVal ?: extractHeight(rawText)
                        val display = if (resolvedHeight != null && !isAudio) {
                            "${resolvedHeight}p"
                        } else {
                            formatDisplayQuality(rawText)
                        }
                        out.add(
                            ParsedResolutionOption(
                                requestQuality = reqQuality,
                                displayLabel = display,
                                sizeBytes = size.coerceAtLeast(0L),
                                ext = ext ?: if (isAudio) "m4a" else "mp4",
                                isAudioOnly = isAudio,
                                directUrl = directUrl,
                                rawFormatId = rawFmtId
                            )
                        )
                    }
                }
            }
        }
    }

    private fun normalizeRequestQuality(rawText: String, height: Int?): String {
        val trimmed = rawText.trim()
        if (pureDigitsRegex.matches(trimmed)) {
            return trimmed
        }
        if (height != null && height > 0) {
            return height.toString()
        }
        heightPRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { digits ->
            // Preserve raw if it already ends cleanly with p (e.g. "720p"), else extract digits
            return if (trimmed.equals("${digits}p", ignoreCase = true)) trimmed else digits
        }
        wxhRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { hDigits ->
            return hDigits
        }
        return trimmed.replace("/", "-").trim()
    }

    private fun isAudioLabel(label: String): Boolean {
        return label.contains("audio", ignoreCase = true) ||
            label.contains("mp3", ignoreCase = true) ||
            label.contains("m4a", ignoreCase = true)
    }

    fun parseVideoInfo(rawJson: String): ParsedVideoInfo? {
        val json = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null
        val title = json.optString("title").takeIf { it.isNotBlank() && it != "null" }
        val author = json.optString("author").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("uploader").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("channel").takeIf { it.isNotBlank() && it != "null" }

        val durationSeconds = parseFlexibleLong(json.opt("length"))
            ?: parseFlexibleLong(json.opt("duration"))
        val viewCount = parseFlexibleLong(json.opt("views"))
            ?: parseFlexibleLong(json.opt("view_count"))

        val thumbRaw = json.opt("thumbnail")
        val thumbUrl = when (thumbRaw) {
            is String -> thumbRaw.takeIf { it.isNotBlank() && it != "null" }
            is JSONObject -> thumbRaw.optString("url").takeIf { it.isNotBlank() && it != "null" }
            is JSONArray -> (0 until thumbRaw.length()).reversed().firstNotNullOfOrNull { idx ->
                when (val el = thumbRaw.opt(idx)) {
                    is String -> el.takeIf { it.isNotBlank() }
                    is JSONObject -> el.optString("url").takeIf { it.isNotBlank() }
                    else -> null
                }
            }
            else -> null
        }

        return ParsedVideoInfo(
            title = title,
            author = author,
            durationSeconds = durationSeconds,
            viewCount = viewCount,
            thumbnailUrl = thumbUrl
        )
    }

    fun parseDownloadResponse(rawJson: String): DownloadQualityResponse? {
        val json = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null
        val dlUrl = json.optString("download_url").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("url").takeIf { it.isNotBlank() && it != "null" }
        val err = json.optString("error").takeIf { it.isNotBlank() && it != "null" }
        val msg = json.optString("message").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("detail").takeIf { it.isNotBlank() && it != "null" }
        val selObj = json.optJSONObject("selected")
        val selHeight = selObj?.let { parseFlexibleLong(it.opt("height"))?.toInt() }
        return DownloadQualityResponse(
            downloadUrl = dlUrl,
            selected = selHeight?.let { SelectedQualityInfo(height = it) },
            error = err,
            message = msg
        )
    }

    fun parseAppUpdateInfo(rawJson: String): AppUpdateInfo? {
        val trimmed = rawJson.trim().removePrefix("\uFEFF")
        val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null

        val enabled = parseFlexibleBoolean(
            json,
            "update_enabled",
            "updateEnabled",
            "update_on",
            "updateOn",
            "is_update_available",
            "isUpdateAvailable",
            "is_update",
            "enabled",
            "update",
            "active",
            "status"
        )

        val versionCode = listOf("version_code", "versionCode", "build", "build_number")
            .firstNotNullOfOrNull { key ->
                if (json.has(key)) parseFlexibleLong(json.opt(key))?.toInt() else null
            } ?: 0

        val rawVersion = listOf("version", "latest_version", "latestVersion", "version_name", "versionName", "app_version")
            .firstNotNullOfOrNull { key ->
                json.optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
            }
            ?: if (versionCode > 0) "v$versionCode" else ""

        val rawUpdateUrl = listOf(
            "update_url",
            "updateUrl",
            "download_url",
            "downloadUrl",
            "apk_url",
            "apkUrl",
            "app_url",
            "appUrl",
            "url",
            "link"
        ).firstNotNullOfOrNull { key ->
            json.optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
        }.orEmpty()

        val normalizedUrl = when {
            rawUpdateUrl.startsWith("http://", ignoreCase = true) ||
                rawUpdateUrl.startsWith("https://", ignoreCase = true) -> rawUpdateUrl
            rawUpdateUrl.startsWith("www.", ignoreCase = true) -> "https://$rawUpdateUrl"
            else -> rawUpdateUrl
        }

        val title = listOf("title", "update_title", "heading")
            .firstNotNullOfOrNull { key ->
                json.optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
            } ?: "Update Available"

        val changelog = listOf("changelog", "message", "notes", "release_notes", "description")
            .firstNotNullOfOrNull { key ->
                json.optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
            } ?: ""

        return AppUpdateInfo(
            updateEnabled = enabled,
            version = rawVersion,
            versionCode = versionCode,
            updateUrl = normalizedUrl,
            title = title,
            changelog = changelog
        )
    }

    fun parseRemoteAppConfig(rawJson: String): RemoteAppConfig? {
        val trimmed = rawJson.trim().removePrefix("\uFEFF")
        val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
        val ytKey = listOf("youtube_api_key", "youtubeApiKey", "yt_api_key")
            .firstNotNullOfOrNull { key ->
                json.optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
            }
        val dlUrl = listOf("download_api_url", "downloadApiUrl", "api_url")
            .firstNotNullOfOrNull { key ->
                json.optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
            }
        val updatedAt = json.optString("updated_at").trim().takeIf { it.isNotBlank() && it != "null" }
        if (ytKey == null && dlUrl == null) return null
        return RemoteAppConfig(
            youtubeApiKey = ytKey,
            downloadApiUrl = dlUrl,
            updatedAt = updatedAt
        )
    }

    fun extractGistFileContent(gistApiJson: String, preferredFileName: String): String? {
        val root = runCatching { JSONObject(gistApiJson) }.getOrNull() ?: return null
        val files = root.optJSONObject("files") ?: return null
        val exact = files.optJSONObject(preferredFileName)
            ?.optString("content")
            ?.takeIf { it.isNotBlank() }
        if (exact != null) return exact

        val keys = files.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val content = files.optJSONObject(k)?.optString("content")?.takeIf { it.isNotBlank() }
            if (content != null) return content
        }
        return null
    }

    fun normalizeVersionString(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }

    /**
     * Compares two version strings numerically segment-by-segment (e.g. "v1.0.1" > "1.0", "1.2" > "1.0.5").
     * Returns >0 if candidate > current, 0 if equal, <0 if candidate < current.
     */
    fun compareVersions(candidate: String, current: String): Int {
        val normA = normalizeVersionString(candidate)
        val normB = normalizeVersionString(current)
        if (normA.isBlank() && normB.isBlank()) return 0
        if (normA.isBlank()) return -1
        if (normB.isBlank()) return 1

        val partsA = normA.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        val partsB = normB.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }

        if (partsA.isEmpty() || partsB.isEmpty()) {
            return normA.compareTo(normB, ignoreCase = true)
        }

        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val a = partsA.getOrElse(i) { 0 }
            val b = partsB.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    fun isVersionNewer(
        remoteVersion: String,
        remoteVersionCode: Int,
        installedVersion: String,
        installedVersionCode: Int
    ): Boolean {
        if (remoteVersionCode > installedVersionCode) return true
        if (remoteVersion.isNotBlank() && compareVersions(remoteVersion, installedVersion) > 0) return true
        return false
    }

    private fun parseFlexibleBoolean(json: JSONObject, vararg keys: String): Boolean {
        for (key in keys) {
            if (!json.has(key) || json.isNull(key)) continue
            when (val raw = json.opt(key)) {
                is Boolean -> return raw
                is Number -> return raw.toInt() != 0
                is String -> {
                    val s = raw.trim().lowercase()
                    return when (s) {
                        "true", "on", "yes", "1", "enabled", "active" -> true
                        "false", "off", "no", "0", "disabled", "inactive" -> false
                        else -> false
                    }
                }
            }
        }
        return false
    }

    private fun parseFlexibleLong(value: Any?): Long? {
        return when (value) {
            is Number -> value.toLong()
            is String -> value.trim().toDoubleOrNull()?.toLong()
            else -> null
        }
    }
}

class DownloadRepository(
    private val context: Context,
    private val downloaderApi: DownloaderApiService,
    private val okHttpClient: OkHttpClient,
    private val dao: VeloTubeDao,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningJobs = mutableMapOf<String, Job>()

    private val _activeDownloads = MutableStateFlow<Map<String, ActiveDownload>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, ActiveDownload>> = _activeDownloads.asStateFlow()

    val downloadedVideos: Flow<List<DownloadedVideoEntity>> = dao.observeDownloadedVideos()
    val downloadedVideoIds: Flow<List<String>> = dao.observeDownloadedIds()

    suspend fun getDownloadedVideoByKey(downloadId: String): DownloadedVideoEntity? {
        return dao.getDownloadedVideoByKey(downloadId)
    }

    suspend fun getDownloadedVideosForVideo(videoId: String): List<DownloadedVideoEntity> {
        return dao.getDownloadedVideosForVideo(videoId)
    }

    private fun withCacheBuster(url: String): String {
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}cb=${System.currentTimeMillis()}"
    }

    private suspend fun fetchFreshGistJson(
        gistApiUrl: String,
        unpinnedRawUrl: String,
        pinnedRawUrl: String,
        preferredFileName: String
    ): Pair<String?, String?> {
        var lastError: String? = null

        // 1. GitHub Gist API returns latest commit content immediately without CDN raw lag
        runCatching {
            val apiResp = downloaderApi.fetchRawJson(withCacheBuster(gistApiUrl))
            if (apiResp.isSuccessful) {
                val apiBody = apiResp.body()?.string().orEmpty()
                val content = DownloaderResponseParser.extractGistFileContent(apiBody, preferredFileName)
                if (!content.isNullOrBlank()) return content to null
            } else {
                lastError = "HTTP ${apiResp.code()}"
            }
        }.onFailure { lastError = it.localizedMessage }

        // 2. Unpinned Gist raw URL with cache-buster always tracks latest Gist revision
        runCatching {
            val rawResp = downloaderApi.fetchRawJson(withCacheBuster(unpinnedRawUrl))
            if (rawResp.isSuccessful) {
                val rawBody = rawResp.body()?.string().orEmpty()
                if (rawBody.isNotBlank()) return rawBody to null
            } else {
                lastError = "HTTP ${rawResp.code()}"
            }
        }.onFailure { lastError = it.localizedMessage }

        // 3. Original pinned fallback URL
        runCatching {
            val pinnedResp = downloaderApi.fetchRawJson(withCacheBuster(pinnedRawUrl))
            if (pinnedResp.isSuccessful) {
                val pinnedBody = pinnedResp.body()?.string().orEmpty()
                if (pinnedBody.isNotBlank()) return pinnedBody to null
            } else {
                lastError = "HTTP ${pinnedResp.code()}"
            }
        }.onFailure { lastError = it.localizedMessage }

        return null to (lastError ?: "Unable to reach GitHub Gist")
    }

    suspend fun syncRemoteConfig(configUrl: String = FIXED_REMOTE_CONFIG_URL): ApiResult<RemoteAppConfig> {
        val cleanUrl = configUrl.trim().ifBlank { FIXED_REMOTE_CONFIG_URL }
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return ApiResult.Error("Invalid remote config URL.")
        }
        return try {
            val (rawJson, err) = fetchFreshGistJson(
                gistApiUrl = REMOTE_CONFIG_GIST_API_URL,
                unpinnedRawUrl = cleanUrl,
                pinnedRawUrl = PINNED_REMOTE_CONFIG_URL,
                preferredFileName = "velotube-config.json"
            )
            if (rawJson.isNullOrBlank()) {
                return ApiResult.Error(err ?: "Empty remote config response.")
            }
            val cfg = DownloaderResponseParser.parseRemoteAppConfig(rawJson)
                ?: return ApiResult.Error("Invalid remote config JSON.")
            preferencesRepository.applyRemoteConfig(
                downloadApiUrl = cfg.downloadApiUrl,
                youtubeApiKey = cfg.youtubeApiKey
            )
            ApiResult.Success(cfg)
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to sync remote config")
        }
    }

    suspend fun checkForAppUpdate(ignoreAcknowledged: Boolean = false): AppUpdateCheckResult =
        withContext(Dispatchers.IO) {
            val (rawJson, fetchErr) = fetchFreshGistJson(
                gistApiUrl = APP_UPDATE_GIST_API_URL,
                unpinnedRawUrl = FIXED_APP_UPDATE_URL,
                pinnedRawUrl = PINNED_APP_UPDATE_URL,
                preferredFileName = "update.json"
            )
            if (rawJson.isNullOrBlank()) {
                return@withContext AppUpdateCheckResult.Error(fetchErr ?: "Empty update response")
            }

            val info = DownloaderResponseParser.parseAppUpdateInfo(rawJson)
                ?: return@withContext AppUpdateCheckResult.Error("Invalid update.json format")

            if (!info.updateEnabled) {
                return@withContext AppUpdateCheckResult.Disabled(info.version)
            }
            if (info.updateUrl.isBlank() ||
                (!info.updateUrl.startsWith("http://", ignoreCase = true) &&
                    !info.updateUrl.startsWith("https://", ignoreCase = true))
            ) {
                return@withContext AppUpdateCheckResult.Error("Missing or invalid update_url in update.json")
            }

            val isNewer = DownloaderResponseParser.isVersionNewer(
                remoteVersion = info.version,
                remoteVersionCode = info.versionCode,
                installedVersion = BuildConfig.VERSION_NAME,
                installedVersionCode = BuildConfig.VERSION_CODE
            )
            if (!isNewer) {
                return@withContext AppUpdateCheckResult.UpToDate(
                    installedVersion = BuildConfig.VERSION_NAME,
                    remoteVersion = info.version.ifBlank { "v${info.versionCode}" }
                )
            }

            val acknowledged = preferencesRepository.preferencesFlow.first().acknowledgedUpdateVersion
            val normRemote = DownloaderResponseParser.normalizeVersionString(info.version)
            val normAck = DownloaderResponseParser.normalizeVersionString(acknowledged)
            if (!ignoreAcknowledged && normAck.isNotBlank() &&
                normAck.equals(normRemote, ignoreCase = true)
            ) {
                return@withContext AppUpdateCheckResult.UpToDate(
                    installedVersion = BuildConfig.VERSION_NAME,
                    remoteVersion = info.version
                )
            }

            AppUpdateCheckResult.Available(info)
        }

    suspend fun acknowledgeUpdateVersion(version: String) {
        preferencesRepository.setAcknowledgedUpdateVersion(version)
    }

    suspend fun resolveFormatsForVideo(
        videoId: String,
        fallbackVideo: VideoItem? = null
    ): ApiResult<ResolvedDownloadPackage> = coroutineScope {
        val baseUrl = preferencesRepository.preferencesFlow.first().downloadApiBaseUrl
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val body = UrlRequestBody(url = watchUrl)

        try {
            val infoDeferred = async {
                runCatching { downloaderApi.getVideoInfo("$baseUrl/video_info", body) }.getOrNull()
            }
            val resolutionsDeferred = async {
                downloaderApi.getAvailableResolutions("$baseUrl/available_resolutions", body)
            }

            val resResponse = resolutionsDeferred.await()
            if (!resResponse.isSuccessful) {
                val errMsg = parseErrorBody(resResponse.errorBody()?.string())
                    ?: "Downloader API returned HTTP ${resResponse.code()}"
                return@coroutineScope ApiResult.Error(errMsg, resResponse.code())
            }

            val rawResJson = resResponse.body()?.string().orEmpty()
            val (parsedOptions, apiError) = DownloaderResponseParser.parseAvailableResolutions(rawResJson)
            if (!apiError.isNullOrBlank() && parsedOptions.isEmpty()) {
                return@coroutineScope ApiResult.Error(apiError)
            }
            if (parsedOptions.isEmpty()) {
                return@coroutineScope ApiResult.Error("No downloadable resolutions are available for this video.")
            }

            val infoResp = infoDeferred.await()
            val infoBody = if (infoResp?.isSuccessful == true) {
                infoResp.body()?.string()?.let { DownloaderResponseParser.parseVideoInfo(it) }
            } else null

            val resolvedVideo = VideoItem(
                id = videoId,
                title = infoBody?.title
                    ?: fallbackVideo?.title
                    ?: "YouTube Video $videoId",
                description = fallbackVideo?.description.orEmpty(),
                channelId = fallbackVideo?.channelId.orEmpty(),
                channelTitle = infoBody?.author
                    ?: fallbackVideo?.channelTitle
                    ?: "YouTube Creator",
                channelAvatarUrl = fallbackVideo?.channelAvatarUrl,
                thumbnailUrl = infoBody?.thumbnailUrl
                    ?: fallbackVideo?.thumbnailUrl
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                publishedAt = fallbackVideo?.publishedAt.orEmpty(),
                durationText = FormatUtils.formatSecondsDuration(infoBody?.durationSeconds?.toString())
                    .ifBlank { fallbackVideo?.durationText.orEmpty() },
                viewCount = infoBody?.viewCount ?: fallbackVideo?.viewCount ?: 0L,
                likeCount = fallbackVideo?.likeCount ?: 0L,
                commentCount = fallbackVideo?.commentCount ?: 0L
            )

            val formats = parsedOptions.map { opt ->
                val resolvedDirect = opt.directUrl?.let { resolveUrlAgainstBase(baseUrl, it) }
                DownloadFormat(
                    formatId = opt.requestQuality,
                    qualityLabel = opt.displayLabel,
                    mimeType = if (opt.isAudioOnly) "audio/mp4" else "video/${opt.ext}",
                    ext = opt.ext,
                    url = resolvedDirect ?: watchUrl,
                    contentLengthBytes = opt.sizeBytes,
                    hasAudio = true,
                    hasVideo = !opt.isAudioOnly,
                    rawFormatId = opt.rawFormatId
                )
            }

            ApiResult.Success(ResolvedDownloadPackage(resolvedVideo, formats))
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to Downloader API ($baseUrl)")
        }
    }

    private suspend fun executeDownloadApiRequest(
        baseUrl: String,
        watchUrl: String,
        primaryQuality: String,
        rawFormatId: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody> {
        val candidates = DownloaderResponseParser.buildQualityCandidates(primaryQuality, rawFormatId)
        val body = UrlRequestBody(url = watchUrl)
        var lastResp: retrofit2.Response<okhttp3.ResponseBody>? = null

        for (candidate in candidates) {
            val encoded = Uri.encode(candidate)
            val resp = downloaderApi.requestDownload("$baseUrl/download/$encoded", body)
            lastResp = resp
            if (resp.code() != 404) return resp
        }

        for (candidate in candidates) {
            val resp = runCatching {
                downloaderApi.requestDownloadWithBodyQuality(
                    fullUrl = "$baseUrl/download",
                    body = DownloadWithQualityBody(url = watchUrl, quality = candidate)
                )
            }.getOrNull()
            if (resp != null) {
                lastResp = resp
                if (resp.code() != 404) return resp
            }
        }

        val snakeRoutes = listOf("$baseUrl/download_video", "$baseUrl/download_mp4")
        for (route in snakeRoutes) {
            val resp = runCatching {
                downloaderApi.requestDownloadWithBodyQuality(
                    fullUrl = route,
                    body = DownloadWithQualityBody(url = watchUrl, quality = primaryQuality)
                )
            }.getOrNull()
            if (resp != null) {
                lastResp = resp
                if (resp.code() != 404) return resp
            }
        }

        for (candidate in candidates) {
            val encoded = Uri.encode(candidate)
            val resp = runCatching {
                downloaderApi.requestDownloadGet(
                    "$baseUrl/download/$encoded?url=${Uri.encode(watchUrl)}"
                )
            }.getOrNull()
            if (resp != null) {
                lastResp = resp
                if (resp.code() != 404) return resp
            }
        }

        return lastResp ?: throw IllegalStateException("Download server unreachable ($baseUrl)")
    }

    suspend fun resolvePlayableStreamUrl(videoId: String): ApiResult<String> = withContext(Dispatchers.IO) {
        val baseUrl = preferencesRepository.preferencesFlow.first().downloadApiBaseUrl
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        return@withContext try {
            val resResp = downloaderApi.getAvailableResolutions(
                "$baseUrl/available_resolutions",
                UrlRequestBody(watchUrl)
            )
            if (!resResp.isSuccessful) {
                return@withContext ApiResult.Error("Resolution lookup failed (${resResp.code()})")
            }
            val rawResJson = resResp.body()?.string().orEmpty()
            val (options, _) = DownloaderResponseParser.parseAvailableResolutions(rawResJson)
            val videoOptions = options.filter { !it.isAudioOnly }
            val chosen = videoOptions.firstOrNull {
                DownloaderResponseParser.extractHeight(it.requestQuality) == 720
            } ?: videoOptions.firstOrNull {
                DownloaderResponseParser.extractHeight(it.requestQuality) == 480
            } ?: videoOptions.firstOrNull {
                DownloaderResponseParser.extractHeight(it.requestQuality) == 360
            } ?: videoOptions.firstOrNull {
                DownloaderResponseParser.extractHeight(it.requestQuality) == 1080
            } ?: videoOptions.lastOrNull()
                ?: options.lastOrNull()
                ?: return@withContext ApiResult.Error("No playable resolution available")

            if (!chosen.directUrl.isNullOrBlank()) {
                return@withContext ApiResult.Success(resolveUrlAgainstBase(baseUrl, chosen.directUrl))
            }

            val dlResp = executeDownloadApiRequest(
                baseUrl = baseUrl,
                watchUrl = watchUrl,
                primaryQuality = chosen.requestQuality,
                rawFormatId = chosen.rawFormatId
            )
            if (!dlResp.isSuccessful) {
                val errText = parseErrorBody(dlResp.errorBody()?.string())
                return@withContext ApiResult.Error(
                    errText ?: "Stream lookup failed (HTTP ${dlResp.code()})"
                )
            }

            val respBody = dlResp.body()
                ?: return@withContext ApiResult.Error("Empty stream response")
            val contentType = respBody.contentType()?.toString()?.lowercase().orEmpty()
            val isExplicitMedia = !contentType.contains("json") && (
                contentType.startsWith("video/") ||
                    contentType.startsWith("audio/") ||
                    contentType.contains("octet-stream")
                )

            if (isExplicitMedia) {
                val streamFile = cacheBinaryStreamForPlayback(videoId, respBody.byteStream())
                return@withContext ApiResult.Success(streamFile.absolutePath)
            }

            val inspection = inspectResponseBodySafely(respBody)
            if (inspection.jsonText != null) {
                val parsed = DownloaderResponseParser.parseDownloadResponse(inspection.jsonText)
                    ?: return@withContext ApiResult.Error("Invalid response from stream endpoint")
                val rawUrl = parsed.downloadUrl
                    ?: return@withContext ApiResult.Error(parsed.error ?: parsed.message ?: "No download_url returned")
                val resolved = resolveUrlAgainstBase(baseUrl, rawUrl)
                return@withContext ApiResult.Success(resolved)
            } else {
                val streamFile = cachePrefixedStreamForPlayback(
                    videoId = videoId,
                    prefixBytes = inspection.prefixBytes,
                    remainingStream = inspection.remainingStream
                )
                return@withContext ApiResult.Success(streamFile.absolutePath)
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            ApiResult.Error(t.localizedMessage ?: "Failed to resolve HD stream")
        }
    }

    private data class BodyInspection(
        val jsonText: String? = null,
        val prefixBytes: ByteArray = ByteArray(0),
        val remainingStream: InputStream? = null
    )

    private fun inspectResponseBodySafely(
        respBody: ResponseBody,
        maxJsonBytes: Int = 512 * 1024
    ): BodyInspection {
        val stream = respBody.byteStream()
        val headBuf = ByteArray(8 * 1024)
        val firstRead = stream.read(headBuf)
        if (firstRead <= 0) {
            stream.close()
            return BodyInspection(jsonText = "")
        }

        val firstNonWs = (0 until firstRead)
            .map { headBuf[it].toInt().toChar() }
            .firstOrNull { !it.isWhitespace() }

        val contentType = respBody.contentType()?.toString()?.lowercase().orEmpty()
        val looksLikeJson = contentType.contains("json") || firstNonWs == '{' || firstNonWs == '['

        if (!looksLikeJson) {
            return BodyInspection(
                jsonText = null,
                prefixBytes = headBuf.copyOf(firstRead),
                remainingStream = stream
            )
        }

        val jsonBuffer = ByteArrayOutputStream(firstRead)
        jsonBuffer.write(headBuf, 0, firstRead)
        val extra = ByteArray(8 * 1024)
        while (jsonBuffer.size() < maxJsonBytes) {
            val n = stream.read(extra)
            if (n == -1) break
            val allowed = minOf(n, maxJsonBytes - jsonBuffer.size())
            jsonBuffer.write(extra, 0, allowed)
        }
        stream.close()
        return BodyInspection(jsonText = jsonBuffer.toString(Charsets.UTF_8.name()))
    }

    private fun cacheBinaryStreamForPlayback(videoId: String, inputStream: InputStream): File {
        return cachePrefixedStreamForPlayback(videoId, ByteArray(0), inputStream)
    }

    private fun cachePrefixedStreamForPlayback(
        videoId: String,
        prefixBytes: ByteArray,
        remainingStream: InputStream?
    ): File {
        val cutoff = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("velotube_stream_") && file.lastModified() < cutoff) {
                file.delete()
            }
        }
        val streamFile = File(context.cacheDir, "velotube_stream_${videoId}.mp4")
        FileOutputStream(streamFile).use { output ->
            if (prefixBytes.isNotEmpty()) {
                output.write(prefixBytes)
            }
            remainingStream?.use { input ->
                input.copyTo(output)
            }
            output.flush()
        }
        return streamFile
    }

    fun startDownload(
        video: VideoItem,
        format: DownloadFormat,
        exportToGallery: Boolean = true
    ): String {
        val downloadId = "${video.id}_${format.formatId}"
        runningJobs[downloadId]?.cancel()

        val initial = ActiveDownload(
            downloadId = downloadId,
            videoId = video.id,
            title = video.title,
            channelTitle = video.channelTitle,
            thumbnailUrl = video.thumbnailUrl,
            qualityLabel = format.qualityLabel,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = format.contentLengthBytes,
            status = DownloadJobStatus.DOWNLOADING
        )
        _activeDownloads.update { it + (downloadId to initial) }

        val job = scope.launch {
            try {
                val baseUrl = preferencesRepository.preferencesFlow.first().downloadApiBaseUrl
                val safeTitle = video.title
                    .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    .take(48)
                    .trim('_')
                    .ifBlank { "ymntube_${video.id}" }
                val safeFormat = format.formatId.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "video" }
                val fileName = "${safeTitle}_${video.id}_${safeFormat}.${format.ext}"

                val appMoviesDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
                    "YmnTube"
                ).apply { mkdirs() }
                val targetFile = File(appMoviesDir, fileName)

                val watchUrl = "https://www.youtube.com/watch?v=${video.id}"
                val apiResp = executeDownloadApiRequest(
                    baseUrl = baseUrl,
                    watchUrl = watchUrl,
                    primaryQuality = format.formatId,
                    rawFormatId = format.rawFormatId
                )

                if (apiResp.code() == 404 && format.url != watchUrl && format.url.startsWith("http")) {
                    // Direct stream URL was included inside progressive/resolutions object
                    _activeDownloads.update { map ->
                        val current = map[downloadId] ?: return@update map
                        map + (downloadId to current.copy(status = DownloadJobStatus.DOWNLOADING))
                    }
                    downloadUrlToFile(
                        downloadUrl = format.url,
                        targetFile = targetFile,
                        downloadId = downloadId,
                        fallbackTotalBytes = format.contentLengthBytes
                    )
                    finishSaveAndExport(video, format, targetFile, fileName, exportToGallery, downloadId)
                    return@launch
                }

                if (!apiResp.isSuccessful) {
                    val errText = parseErrorBody(apiResp.errorBody()?.string())
                        ?: "Download API failed (HTTP ${apiResp.code()})"
                    if (apiResp.code() == 404) {
                        throw IllegalStateException(
                            "$errText at $baseUrl/download/${format.formatId}"
                        )
                    }
                    throw IllegalStateException("$errText (${format.qualityLabel})")
                }

                val respBody = apiResp.body()
                    ?: throw IllegalStateException("Empty response from download API")

                val contentType = respBody.contentType()?.toString()?.lowercase().orEmpty()
                val headerLen = respBody.contentLength()
                val effectiveLen = if (headerLen > 0L) headerLen else format.contentLengthBytes
                val isExplicitVideoBinary = !contentType.contains("json") && (
                    contentType.startsWith("video/") ||
                        contentType.startsWith("audio/") ||
                        contentType.contains("octet-stream")
                    )

                if (isExplicitVideoBinary) {
                    _activeDownloads.update { map ->
                        val current = map[downloadId] ?: return@update map
                        map + (downloadId to current.copy(
                            status = DownloadJobStatus.DOWNLOADING,
                            totalBytes = effectiveLen.coerceAtLeast(0L)
                        ))
                    }
                    streamToFileWithProgress(
                        inputStream = respBody.byteStream(),
                        totalBytes = effectiveLen,
                        targetFile = targetFile,
                        downloadId = downloadId
                    )
                } else {
                    val inspection = inspectResponseBodySafely(respBody)
                    if (inspection.jsonText == null) {
                        _activeDownloads.update { map ->
                            val current = map[downloadId] ?: return@update map
                            map + (downloadId to current.copy(
                                status = DownloadJobStatus.DOWNLOADING,
                                totalBytes = effectiveLen.coerceAtLeast(0L)
                            ))
                        }
                        streamPrefixedToFileWithProgress(
                            prefixBytes = inspection.prefixBytes,
                            remainingStream = inspection.remainingStream,
                            totalBytes = effectiveLen,
                            targetFile = targetFile,
                            downloadId = downloadId
                        )
                    } else {
                        val parsed = DownloaderResponseParser.parseDownloadResponse(inspection.jsonText)
                            ?: throw IllegalStateException("API did not return valid JSON download response")

                        if (!parsed.error.isNullOrBlank() ||
                            (!parsed.message.isNullOrBlank() && parsed.downloadUrl.isNullOrBlank())
                        ) {
                            throw IllegalStateException(parsed.error ?: parsed.message)
                        }

                        val expectedHeight = DownloaderResponseParser.extractHeight(format.formatId)
                        val returnedHeight = parsed.selected?.height
                        if (expectedHeight != null && returnedHeight != null && returnedHeight != expectedHeight) {
                            throw IllegalStateException(
                                "Requested ${format.qualityLabel} but server returned ${returnedHeight}p. Aborted to prevent wrong quality."
                            )
                        }

                        val rawDownloadUrl = parsed.downloadUrl
                            ?.takeIf { it.isNotBlank() }
                            ?: throw IllegalStateException("Missing download_url in API response")

                        val resolvedDownloadUrl = resolveUrlAgainstBase(baseUrl, rawDownloadUrl)

                        _activeDownloads.update { map ->
                            val current = map[downloadId] ?: return@update map
                            map + (downloadId to current.copy(status = DownloadJobStatus.DOWNLOADING))
                        }

                        downloadUrlToFile(
                            downloadUrl = resolvedDownloadUrl,
                            targetFile = targetFile,
                            downloadId = downloadId,
                            fallbackTotalBytes = format.contentLengthBytes
                        )
                    }
                }

                finishSaveAndExport(video, format, targetFile, fileName, exportToGallery, downloadId)
            } catch (_: CancellationException) {
                _activeDownloads.update { it - downloadId }
            } catch (t: Throwable) {
                _activeDownloads.update { map ->
                    val current = map[downloadId] ?: return@update map
                    map + (downloadId to current.copy(
                        status = DownloadJobStatus.FAILED,
                        errorMessage = t.localizedMessage ?: "Download failed"
                    ))
                }
            } finally {
                runningJobs.remove(downloadId)
            }
        }

        runningJobs[downloadId] = job
        return downloadId
    }

    private suspend fun finishSaveAndExport(
        video: VideoItem,
        format: DownloadFormat,
        targetFile: File,
        fileName: String,
        exportToGallery: Boolean,
        downloadId: String
    ) {
        if (!targetFile.exists() || targetFile.length() <= 0L) {
            throw IllegalStateException("Downloaded file is empty or missing")
        }

        var galleryUri: String? = null
        if (exportToGallery && format.hasVideo) {
            _activeDownloads.update { map ->
                val current = map[downloadId] ?: return@update map
                map + (downloadId to current.copy(
                    progress = 1f,
                    status = DownloadJobStatus.EXPORTING_GALLERY
                ))
            }
            galleryUri = runCatching {
                exportFileToGallery(
                    sourceFile = targetFile,
                    displayName = fileName,
                    mimeType = format.mimeType
                )
            }.getOrNull()
        }

        val finalSize = targetFile.length()
        val entity = DownloadedVideoEntity(
            downloadId = downloadId,
            videoId = video.id,
            title = video.title,
            channelTitle = video.channelTitle,
            thumbnailUrl = video.thumbnailUrl,
            durationText = video.durationText,
            qualityLabel = format.qualityLabel,
            mimeType = format.mimeType,
            fileSizeBytes = finalSize,
            localFilePath = targetFile.absolutePath,
            mediaStoreUri = galleryUri,
            isSavedToGallery = galleryUri != null,
            downloadedAt = System.currentTimeMillis()
        )
        dao.upsertDownloadedVideo(entity)

        _activeDownloads.update { map ->
            val current = map[downloadId] ?: return@update map
            map + (downloadId to current.copy(
                progress = 1f,
                bytesDownloaded = finalSize,
                totalBytes = finalSize,
                status = DownloadJobStatus.COMPLETED
            ))
        }
    }

    fun startDirectUrlDownload(
        directUrl: String,
        customTitle: String = ""
    ): String {
        val cleanUrl = directUrl.trim()
        val id = "direct_${UUID.randomUUID().toString().take(8)}"
        val title = customTitle.ifBlank {
            cleanUrl.toUri().lastPathSegment?.substringBeforeLast(".") ?: "Direct Video"
        }
        val ext = when {
            cleanUrl.contains(".webm", ignoreCase = true) -> "webm"
            cleanUrl.contains(".m4a", ignoreCase = true) -> "m4a"
            else -> "mp4"
        }
        val mime = when (ext) {
            "webm" -> "video/webm"
            "m4a" -> "audio/mp4"
            else -> "video/mp4"
        }
        val downloadId = "${id}_direct"
        runningJobs[downloadId]?.cancel()

        _activeDownloads.update {
            it + (downloadId to ActiveDownload(
                downloadId = downloadId,
                videoId = id,
                title = title,
                channelTitle = "Direct URL",
                thumbnailUrl = "",
                qualityLabel = ext.uppercase(),
                status = DownloadJobStatus.DOWNLOADING
            ))
        }

        val job = scope.launch {
            try {
                val safeTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(48).ifBlank { id }
                val fileName = "${safeTitle}.$ext"
                val appMoviesDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
                    "YmnTube"
                ).apply { mkdirs() }
                val targetFile = File(appMoviesDir, fileName)

                downloadUrlToFile(cleanUrl, targetFile, downloadId, 0L)
                if (!targetFile.exists() || targetFile.length() <= 0L) {
                    throw IllegalStateException("Downloaded file is empty")
                }

                val autoGallery = preferencesRepository.preferencesFlow.first().autoExportToGallery
                val galleryUri = if (autoGallery && ext != "m4a") {
                    _activeDownloads.update { map ->
                        val cur = map[downloadId] ?: return@update map
                        map + (downloadId to cur.copy(progress = 1f, status = DownloadJobStatus.EXPORTING_GALLERY))
                    }
                    runCatching { exportFileToGallery(targetFile, fileName, mime) }.getOrNull()
                } else null

                val finalSize = targetFile.length()
                dao.upsertDownloadedVideo(
                    DownloadedVideoEntity(
                        downloadId = downloadId,
                        videoId = id,
                        title = title,
                        channelTitle = "Direct URL",
                        thumbnailUrl = "",
                        durationText = "",
                        qualityLabel = ext.uppercase(),
                        mimeType = mime,
                        fileSizeBytes = finalSize,
                        localFilePath = targetFile.absolutePath,
                        mediaStoreUri = galleryUri,
                        isSavedToGallery = galleryUri != null,
                        downloadedAt = System.currentTimeMillis()
                    )
                )
                _activeDownloads.update { map ->
                    val cur = map[downloadId] ?: return@update map
                    map + (downloadId to cur.copy(
                        progress = 1f,
                        bytesDownloaded = finalSize,
                        totalBytes = finalSize,
                        status = DownloadJobStatus.COMPLETED
                    ))
                }
            } catch (_: CancellationException) {
                _activeDownloads.update { it - downloadId }
            } catch (t: Throwable) {
                _activeDownloads.update { map ->
                    val cur = map[downloadId] ?: return@update map
                    map + (downloadId to cur.copy(
                        status = DownloadJobStatus.FAILED,
                        errorMessage = t.localizedMessage ?: "Direct download failed"
                    ))
                }
            } finally {
                runningJobs.remove(downloadId)
            }
        }
        runningJobs[downloadId] = job
        return downloadId
    }

    private fun downloadUrlToFile(
        downloadUrl: String,
        targetFile: File,
        downloadId: String,
        fallbackTotalBytes: Long = 0L
    ) {
        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download host returned HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Empty download body")
            val headerLen = body.contentLength()
            val effectiveTotal = if (headerLen > 0L) headerLen else fallbackTotalBytes
            streamToFileWithProgress(
                inputStream = body.byteStream(),
                totalBytes = effectiveTotal,
                targetFile = targetFile,
                downloadId = downloadId
            )
        }
    }

    private fun streamToFileWithProgress(
        inputStream: InputStream,
        totalBytes: Long,
        targetFile: File,
        downloadId: String
    ) {
        streamPrefixedToFileWithProgress(
            prefixBytes = ByteArray(0),
            remainingStream = inputStream,
            totalBytes = totalBytes,
            targetFile = targetFile,
            downloadId = downloadId
        )
    }

    private fun streamPrefixedToFileWithProgress(
        prefixBytes: ByteArray,
        remainingStream: InputStream?,
        totalBytes: Long,
        targetFile: File,
        downloadId: String
    ) {
        FileOutputStream(targetFile).use { output ->
            var downloaded = 0L
            if (prefixBytes.isNotEmpty()) {
                output.write(prefixBytes)
                downloaded += prefixBytes.size
            }
            if (remainingStream != null) {
                remainingStream.use { input ->
                    val buffer = ByteArray(16 * 1024)
                    var lastUpdateMs = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateMs > 100L) {
                            lastUpdateMs = now
                            val rawFrac = if (totalBytes > 0L) downloaded.toFloat() / totalBytes.toFloat() else 0f
                            val fraction = if (rawFrac.isFinite()) rawFrac.coerceIn(0f, 1f) else 0f
                            _activeDownloads.update { map ->
                                val current = map[downloadId] ?: return@update map
                                map + (downloadId to current.copy(
                                    progress = fraction,
                                    bytesDownloaded = downloaded,
                                    totalBytes = totalBytes.coerceAtLeast(0L),
                                    status = DownloadJobStatus.DOWNLOADING
                                ))
                            }
                        }
                    }
                }
            }
            output.flush()
        }
    }

    private fun resolveUrlAgainstBase(baseUrl: String, rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return runCatching {
            URI(baseUrl.trimEnd('/') + "/").resolve(trimmed).toString()
        }.getOrDefault(trimmed)
    }

    private fun parseErrorBody(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            json.optString("error").takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("detail").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun cancelDownload(downloadId: String) {
        runningJobs[downloadId]?.cancel()
        runningJobs.remove(downloadId)
        _activeDownloads.update { it - downloadId }
    }

    fun dismissCompletedOrFailed(downloadId: String) {
        _activeDownloads.update { it - downloadId }
    }

    suspend fun exportExistingToGallery(entity: DownloadedVideoEntity): Boolean {
        val source = File(entity.localFilePath)
        if (!source.exists()) return false
        val uri = exportFileToGallery(
            sourceFile = source,
            displayName = source.name,
            mimeType = entity.mimeType
        ) ?: return false
        dao.upsertDownloadedVideo(
            entity.copy(mediaStoreUri = uri, isSavedToGallery = true)
        )
        return true
    }

    suspend fun deleteDownloadedVideo(entity: DownloadedVideoEntity) = withContext(Dispatchers.IO) {
        runCatching { File(entity.localFilePath).delete() }
        entity.mediaStoreUri?.let { uriStr ->
            runCatching {
                context.contentResolver.delete(uriStr.toUri(), null, null)
            }
        }
        dao.deleteDownloadedVideoByKey(entity.downloadId)
    }

    fun openInGallery(entity: DownloadedVideoEntity): Boolean {
        val uri = entity.mediaStoreUri?.toUri()
            ?: MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (entity.mediaStoreUri != null) {
                setDataAndType(uri, entity.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                data = uri
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun shareDownloadedVideo(entity: DownloadedVideoEntity): Boolean {
        val uriString = entity.mediaStoreUri ?: return false
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = entity.mimeType
            putExtra(Intent.EXTRA_STREAM, uriString.toUri())
            putExtra(Intent.EXTRA_SUBJECT, entity.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Video").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    private suspend fun exportFileToGallery(
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) return@withContext null

        val safeMime = when {
            mimeType.startsWith("video/") -> mimeType
            displayName.endsWith(".webm", ignoreCase = true) -> "video/webm"
            displayName.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            else -> "video/mp4"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            var insertedUri: Uri? = null
            return@withContext try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, safeMime)
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/YmnTube")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                insertedUri = resolver.insert(collection, values) ?: return@withContext null

                resolver.openOutputStream(insertedUri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Cannot open MediaStore OutputStream")
                val publishValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(insertedUri, publishValues, null, null)
                insertedUri.toString()
            } catch (_: Throwable) {
                insertedUri?.let { uri -> runCatching { resolver.delete(uri, null, null) } }
                null
            }
        } else {
            return@withContext try {
                val hasWritePerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasWritePerm) return@withContext null

                @Suppress("DEPRECATION")
                val publicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "YmnTube"
                ).apply { mkdirs() }
                val destFile = File(publicDir, displayName)
                sourceFile.copyTo(destFile, overwrite = true)
                scanMediaFile(destFile.absolutePath, safeMime)
            } catch (_: Throwable) {
                null
            }
        }
    }

    private suspend fun scanMediaFile(path: String, mimeType: String): String? =
        suspendCancellableCoroutine { cont ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                arrayOf(mimeType)
            ) { _, uri ->
                if (cont.isActive) {
                    cont.resume(uri?.toString())
                }
            }
        }
}
