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
import com.example.data.local.DownloadedVideoEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VeloTubeDao
import com.example.data.model.ActiveDownload
import com.example.data.model.ApiResult
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadJobStatus
import com.example.data.model.DownloadQualityResponse
import com.example.data.model.DownloadWithQualityBody
import com.example.data.model.RemoteAppConfig
import com.example.data.model.SelectedQualityInfo
import com.example.data.model.UrlRequestBody
import com.example.data.model.VideoItem
import com.example.data.remote.ApiClient
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
import org.json.JSONArray
import org.json.JSONObject
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
    val directUrl: String? = null
)

internal object DownloaderResponseParser {
    private val heightPRegex = Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE)
    private val wxhRegex = Regex("\\d{3,4}x(\\d{3,4})", RegexOption.IGNORE_CASE)

    fun parseAvailableResolutions(rawJson: String): Pair<List<ParsedResolutionOption>, String?> {
        val json = runCatching { JSONObject(rawJson) }.getOrNull()
            ?: return emptyList<ParsedResolutionOption>() to "Downloader API returned invalid JSON."

        val err = json.optString("error").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("message").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("detail").takeIf { it.isNotBlank() && it != "null" }

        val candidateArrays = listOfNotNull(
            json.optJSONArray("all"),
            json.optJSONArray("resolutions"),
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
                    if (trimmed.isNotEmpty()) {
                        val isAudio = isAudioLabel(trimmed)
                        val reqQuality = normalizeRequestQuality(trimmed, null)
                        out.add(
                            ParsedResolutionOption(
                                requestQuality = reqQuality,
                                displayLabel = trimmed,
                                ext = if (isAudio) "m4a" else "mp4",
                                isAudioOnly = isAudio
                            )
                        )
                    }
                }
                is Number -> {
                    val h = item.toInt()
                    if (h > 0) {
                        val q = "${h}p"
                        out.add(ParsedResolutionOption(requestQuality = q, displayLabel = q))
                    }
                }
                is JSONObject -> {
                    val heightVal = item.optInt("height", 0).takeIf { it > 0 }
                    val rawText = item.optString("quality").takeIf { it.isNotBlank() && it != "null" }
                        ?: item.optString("label").takeIf { it.isNotBlank() && it != "null" }
                        ?: item.optString("resolution").takeIf { it.isNotBlank() && it != "null" }
                        ?: item.optString("format_note").takeIf { it.isNotBlank() && it != "null" }
                        ?: heightVal?.let { "${it}p" }

                    if (!rawText.isNullOrBlank()) {
                        val ext = item.optString("ext").takeIf { it.isNotBlank() && it != "null" }
                        val size = item.optLong("filesize", 0L)
                            .takeIf { it > 0L }
                            ?: item.optLong("filesize_approx", 0L)
                                .takeIf { it > 0L }
                            ?: item.optLong("size", 0L)
                        val directUrl = item.optString("url").takeIf { it.startsWith("http") }
                            ?: item.optString("download_url").takeIf { it.startsWith("http") }
                        val isAudio = isAudioLabel(rawText) ||
                            item.optString("vcodec").equals("none", ignoreCase = true)
                        val reqQuality = normalizeRequestQuality(rawText, heightVal)
                        val display = if (heightVal != null && !rawText.contains("${heightVal}p", ignoreCase = true)) {
                            "${heightVal}p"
                        } else {
                            reqQuality
                        }
                        out.add(
                            ParsedResolutionOption(
                                requestQuality = reqQuality,
                                displayLabel = display,
                                sizeBytes = size.coerceAtLeast(0L),
                                ext = ext ?: if (isAudio) "m4a" else "mp4",
                                isAudioOnly = isAudio,
                                directUrl = directUrl
                            )
                        )
                    }
                }
            }
        }
    }

    private fun normalizeRequestQuality(rawText: String, height: Int?): String {
        if (height != null && height > 0) {
            return "${height}p"
        }
        heightPRegex.find(rawText)?.groupValues?.getOrNull(1)?.let { digits ->
            return "${digits}p"
        }
        wxhRegex.find(rawText)?.groupValues?.getOrNull(1)?.let { hDigits ->
            return "${hDigits}p"
        }
        return rawText.trim().replace("/", "-").trim()
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

    suspend fun getDownloadedVideo(videoId: String): DownloadedVideoEntity? {
        return dao.getDownloadedVideoById(videoId)
    }

    suspend fun syncRemoteConfig(configUrl: String): ApiResult<RemoteAppConfig> {
        val cleanUrl = configUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return ApiResult.Error("Enter a valid http(s) URL for the remote config JSON.")
        }
        return try {
            val response = downloaderApi.fetchRemoteConfig(cleanUrl)
            if (!response.isSuccessful) {
                return ApiResult.Error("Remote config returned HTTP ${response.code()}", response.code())
            }
            val cfg = response.body() ?: return ApiResult.Error("Empty remote config response.")
            preferencesRepository.setRemoteConfigUrl(cleanUrl)
            preferencesRepository.applyRemoteConfig(
                downloadApiUrl = cfg.downloadApiUrl,
                youtubeApiKey = cfg.youtubeApiKey
            )
            ApiResult.Success(cfg)
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to sync remote config")
        }
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
                DownloadFormat(
                    formatId = opt.requestQuality,
                    qualityLabel = opt.displayLabel,
                    mimeType = if (opt.isAudioOnly) "audio/mp4" else "video/${opt.ext}",
                    ext = opt.ext,
                    url = opt.directUrl ?: watchUrl,
                    contentLengthBytes = opt.sizeBytes,
                    hasAudio = true,
                    hasVideo = !opt.isAudioOnly
                )
            }

            ApiResult.Success(ResolvedDownloadPackage(resolvedVideo, formats))
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to Downloader API ($baseUrl)")
        }
    }

    suspend fun resolvePlayableStreamUrl(videoId: String): ApiResult<String> {
        val baseUrl = preferencesRepository.preferencesFlow.first().downloadApiBaseUrl
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        return try {
            val resResp = downloaderApi.getAvailableResolutions(
                "$baseUrl/available_resolutions",
                UrlRequestBody(watchUrl)
            )
            if (!resResp.isSuccessful) {
                return ApiResult.Error("Resolution lookup failed (${resResp.code()})")
            }
            val rawResJson = resResp.body()?.string().orEmpty()
            val (options, _) = DownloaderResponseParser.parseAvailableResolutions(rawResJson)
            val chosen = options.firstOrNull { it.requestQuality.contains("720") }
                ?: options.firstOrNull { it.requestQuality.contains("360") }
                ?: options.lastOrNull { !it.isAudioOnly }
                ?: return ApiResult.Error("No playable resolution available")

            if (!chosen.directUrl.isNullOrBlank()) {
                return ApiResult.Success(chosen.directUrl)
            }

            val encodedQuality = Uri.encode(chosen.requestQuality)
            var dlResp = downloaderApi.requestDownload(
                "$baseUrl/download/$encodedQuality",
                UrlRequestBody(watchUrl)
            )
            if (dlResp.code() == 404) {
                dlResp = downloaderApi.requestDownloadWithBodyQuality(
                    "$baseUrl/download",
                    DownloadWithQualityBody(url = watchUrl, quality = chosen.requestQuality)
                )
            }
            if (!dlResp.isSuccessful) {
                return ApiResult.Error("Stream lookup failed (${dlResp.code()})")
            }
            val bodyStr = dlResp.body()?.string().orEmpty()
            val parsed = DownloaderResponseParser.parseDownloadResponse(bodyStr)
                ?: return ApiResult.Error("Server returned binary stream instead of JSON stream URL")
            val rawUrl = parsed.downloadUrl
                ?: return ApiResult.Error(parsed.error ?: parsed.message ?: "No download_url returned")
            val resolved = resolveUrlAgainstBase(baseUrl, rawUrl)
            ApiResult.Success(resolved)
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to resolve HD stream")
        }
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
            totalBytes = format.contentLengthBytes,
            status = DownloadJobStatus.RESOLVING
        )
        _activeDownloads.update { it + (downloadId to initial) }

        val job = scope.launch {
            try {
                val baseUrl = preferencesRepository.preferencesFlow.first().downloadApiBaseUrl
                val safeTitle = video.title
                    .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    .take(48)
                    .trim('_')
                    .ifBlank { "velotube_${video.id}" }
                val safeFormat = format.formatId.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "video" }
                val fileName = "${safeTitle}_${video.id}_${safeFormat}.${format.ext}"

                val appMoviesDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
                    "VeloTube"
                ).apply { mkdirs() }
                val targetFile = File(appMoviesDir, fileName)

                val watchUrl = "https://www.youtube.com/watch?v=${video.id}"
                val encodedQuality = Uri.encode(format.formatId)
                val endpoint = "$baseUrl/download/$encodedQuality"

                var apiResp = downloaderApi.requestDownload(
                    fullUrl = endpoint,
                    body = UrlRequestBody(url = watchUrl)
                )

                // If path-based /download/{quality} returns 404, try fallback /download with body quality
                if (apiResp.code() == 404) {
                    val fallbackResp = downloaderApi.requestDownloadWithBodyQuality(
                        fullUrl = "$baseUrl/download",
                        body = DownloadWithQualityBody(url = watchUrl, quality = format.formatId)
                    )
                    if (fallbackResp.isSuccessful || fallbackResp.code() != 404) {
                        apiResp = fallbackResp
                    } else if (format.url != watchUrl && format.url.startsWith("http")) {
                        // Direct stream URL was included inside progressive object
                        _activeDownloads.update { map ->
                            val current = map[downloadId] ?: return@update map
                            map + (downloadId to current.copy(status = DownloadJobStatus.DOWNLOADING))
                        }
                        downloadUrlToFile(format.url, targetFile, downloadId)
                        finishSaveAndExport(video, format, targetFile, fileName, exportToGallery, downloadId)
                        return@launch
                    }
                }

                if (!apiResp.isSuccessful) {
                    val errText = parseErrorBody(apiResp.errorBody()?.string())
                        ?: "Download API failed (HTTP ${apiResp.code()})"
                    throw IllegalStateException("$errText (${format.formatId})")
                }

                val respBody = apiResp.body()
                    ?: throw IllegalStateException("Empty response from download API")

                val contentType = respBody.contentType()?.toString()?.lowercase().orEmpty()
                val isVideoBinary = contentType.startsWith("video/") ||
                    contentType.startsWith("audio/") ||
                    contentType.contains("application/octet-stream")

                if (isVideoBinary) {
                    val totalLen = respBody.contentLength()
                    _activeDownloads.update { map ->
                        val current = map[downloadId] ?: return@update map
                        map + (downloadId to current.copy(
                            status = DownloadJobStatus.DOWNLOADING,
                            totalBytes = totalLen.coerceAtLeast(0L)
                        ))
                    }
                    streamToFileWithProgress(
                        inputStream = respBody.byteStream(),
                        totalBytes = totalLen,
                        targetFile = targetFile,
                        downloadId = downloadId
                    )
                } else {
                    val rawText = respBody.string()
                    val parsed = DownloaderResponseParser.parseDownloadResponse(rawText)
                        ?: throw IllegalStateException("API did not return valid JSON download response")

                    if (!parsed.error.isNullOrBlank() ||
                        (!parsed.message.isNullOrBlank() && parsed.downloadUrl.isNullOrBlank())
                    ) {
                        throw IllegalStateException(parsed.error ?: parsed.message)
                    }

                    val expectedHeight = Regex("(\\d{3,4})p").find(format.formatId)
                        ?.groupValues?.getOrNull(1)?.toIntOrNull()
                    val returnedHeight = parsed.selected?.height
                    if (expectedHeight != null && returnedHeight != null && returnedHeight != expectedHeight) {
                        throw IllegalStateException(
                            "Requested ${format.formatId} but server returned ${returnedHeight}p. Aborted to prevent wrong quality."
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
                        downloadId = downloadId
                    )
                }

                finishSaveAndExport(video, format, targetFile, fileName, exportToGallery, downloadId)
            } catch (_: CancellationException) {
                _activeDownloads.update { it - downloadId }
            } catch (e: Exception) {
                _activeDownloads.update { map ->
                    val current = map[downloadId] ?: return@update map
                    map + (downloadId to current.copy(
                        status = DownloadJobStatus.FAILED,
                        errorMessage = e.localizedMessage ?: "Download failed"
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
        var galleryUri: String? = null
        if (exportToGallery && format.hasVideo) {
            _activeDownloads.update { map ->
                val current = map[downloadId] ?: return@update map
                map + (downloadId to current.copy(
                    progress = 1f,
                    status = DownloadJobStatus.EXPORTING_GALLERY
                ))
            }
            galleryUri = exportFileToGallery(
                sourceFile = targetFile,
                displayName = fileName,
                mimeType = format.mimeType
            )
        }

        val finalSize = targetFile.length()
        val entity = DownloadedVideoEntity(
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
                    "VeloTube"
                ).apply { mkdirs() }
                val targetFile = File(appMoviesDir, fileName)

                downloadUrlToFile(cleanUrl, targetFile, downloadId)

                val autoGallery = preferencesRepository.preferencesFlow.first().autoExportToGallery
                val galleryUri = if (autoGallery && ext != "m4a") {
                    _activeDownloads.update { map ->
                        val cur = map[downloadId] ?: return@update map
                        map + (downloadId to cur.copy(status = DownloadJobStatus.EXPORTING_GALLERY))
                    }
                    exportFileToGallery(targetFile, fileName, mime)
                } else null

                val finalSize = targetFile.length()
                dao.upsertDownloadedVideo(
                    DownloadedVideoEntity(
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
            } catch (e: Exception) {
                _activeDownloads.update { map ->
                    val cur = map[downloadId] ?: return@update map
                    map + (downloadId to cur.copy(
                        status = DownloadJobStatus.FAILED,
                        errorMessage = e.localizedMessage ?: "Direct download failed"
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
        downloadId: String
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
            streamToFileWithProgress(
                inputStream = body.byteStream(),
                totalBytes = body.contentLength(),
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
        inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(16 * 1024)
                var downloaded = 0L
                var lastUpdateMs = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateMs > 150L) {
                        lastUpdateMs = now
                        val fraction = if (totalBytes > 0) {
                            (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
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
                output.flush()
            }
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
        dao.deleteDownloadedVideo(entity.videoId)
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
        if (!sourceFile.exists()) return@withContext null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VeloTube")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val insertedUri = resolver.insert(collection, values) ?: return@withContext null

            return@withContext try {
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
            } catch (_: Exception) {
                runCatching { resolver.delete(insertedUri, null, null) }
                null
            }
        } else {
            val hasWritePerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasWritePerm) return@withContext null

            return@withContext try {
                @Suppress("DEPRECATION")
                val publicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "VeloTube"
                ).apply { mkdirs() }
                val destFile = File(publicDir, displayName)
                sourceFile.copyTo(destFile, overwrite = true)
                scanMediaFile(destFile.absolutePath, mimeType)
            } catch (_: Exception) {
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
