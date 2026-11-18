package com.example.data.repository

import com.example.data.local.SearchHistoryEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VeloTubeDao
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.ApiResult
import com.example.data.model.ChannelInfo
import com.example.data.model.VideoComment
import com.example.data.model.VideoItem
import com.example.data.model.YouTubeCategory
import com.example.data.model.YouTubeVideoResource
import com.example.data.remote.ApiClient
import com.example.data.remote.FormatUtils
import com.example.data.remote.YouTubeApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class YouTubeRepository(
    private val api: YouTubeApiService,
    private val dao: VeloTubeDao,
    private val preferencesRepository: UserPreferencesRepository
) {
    val watchHistory: Flow<List<WatchHistoryEntity>> = dao.observeWatchHistory()
    val watchlist: Flow<List<WatchHistoryEntity>> = dao.observeWatchlist()
    val searchHistory: Flow<List<SearchHistoryEntity>> = dao.observeSearchHistory()

    private suspend fun resolveApiKey(): String? {
        val overrideKey = preferencesRepository.preferencesFlow.first().youtubeApiKeyOverride
        return ApiClient.getEffectiveYouTubeApiKey(overrideKey)
    }

    suspend fun getTrendingVideos(
        category: YouTubeCategory,
        regionCode: String = "US"
    ): ApiResult<List<VideoItem>> {
        val apiKey = resolveApiKey()
            ?: return ApiResult.MissingApiKey("YOUTUBE_API_KEY")

        return try {
            val response = api.getPopularVideos(
                regionCode = regionCode,
                videoCategoryId = category.categoryId,
                maxResults = 25,
                apiKey = apiKey
            )
            val videos = enrichWithChannelAvatars(response.items.map { it.toDomain() }, apiKey)
            ApiResult.Success(videos)
        } catch (e: HttpException) {
            ApiResult.Error(parseHttpError(e), e.code())
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to load videos")
        }
    }

    suspend fun searchVideos(query: String): ApiResult<List<VideoItem>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ApiResult.Success(emptyList())

        val apiKey = resolveApiKey()
            ?: return ApiResult.MissingApiKey("YOUTUBE_API_KEY")

        return try {
            dao.upsertSearchQuery(SearchHistoryEntity(query = trimmed))
            val directId = FormatUtils.extractYouTubeVideoId(trimmed)
            if (directId != null) {
                val detailResp = api.getVideosByIds(ids = directId, apiKey = apiKey)
                val items = enrichWithChannelAvatars(detailResp.items.map { it.toDomain() }, apiKey)
                return ApiResult.Success(items)
            }

            val searchResp = api.searchVideos(query = trimmed, maxResults = 20, apiKey = apiKey)
            val ids = searchResp.items.mapNotNull { it.id?.videoId }.distinct()
            if (ids.isEmpty()) return ApiResult.Success(emptyList())

            val detailsResp = api.getVideosByIds(ids = ids.joinToString(","), apiKey = apiKey)
            val videos = enrichWithChannelAvatars(detailsResp.items.map { it.toDomain() }, apiKey)
            ApiResult.Success(videos)
        } catch (e: HttpException) {
            ApiResult.Error(parseHttpError(e), e.code())
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Search failed")
        }
    }

    suspend fun getVideoDetails(videoId: String): ApiResult<VideoItem> {
        val apiKey = resolveApiKey()
            ?: return ApiResult.MissingApiKey("YOUTUBE_API_KEY")

        return try {
            val response = api.getVideosByIds(ids = videoId, apiKey = apiKey)
            val video = response.items.firstOrNull()?.toDomain()
                ?: return ApiResult.Error("Video not found", 404)
            val enriched = enrichWithChannelAvatars(listOf(video), apiKey).first()
            ApiResult.Success(enriched)
        } catch (e: HttpException) {
            ApiResult.Error(parseHttpError(e), e.code())
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to load video details")
        }
    }

    suspend fun getChannelInfo(channelId: String): ApiResult<ChannelInfo> {
        val apiKey = resolveApiKey()
            ?: return ApiResult.MissingApiKey("YOUTUBE_API_KEY")
        if (channelId.isBlank()) return ApiResult.Error("Missing channel ID")

        return try {
            val response = api.getChannelsByIds(ids = channelId, apiKey = apiKey)
            val item = response.items.firstOrNull()
                ?: return ApiResult.Error("Channel not found")
            ApiResult.Success(
                ChannelInfo(
                    id = item.id,
                    title = item.snippet?.title.orEmpty(),
                    avatarUrl = item.snippet?.thumbnails?.bestUrl().orEmpty(),
                    subscriberCount = item.statistics?.subscriberCount?.toLongOrNull() ?: 0L
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to load channel")
        }
    }

    suspend fun getRelatedVideos(video: VideoItem): ApiResult<List<VideoItem>> {
        val apiKey = resolveApiKey()
            ?: return ApiResult.MissingApiKey("YOUTUBE_API_KEY")

        return try {
            val queryTerms = video.title.split(" ")
                .filter { it.length > 3 }
                .take(4)
                .joinToString(" ")
                .ifBlank { video.channelTitle }
            val searchResp = api.searchVideos(query = queryTerms, maxResults = 12, apiKey = apiKey)
            val ids = searchResp.items
                .mapNotNull { it.id?.videoId }
                .filter { it != video.id }
                .distinct()
            if (ids.isEmpty()) return ApiResult.Success(emptyList())

            val detailsResp = api.getVideosByIds(ids = ids.joinToString(","), apiKey = apiKey)
            val related = enrichWithChannelAvatars(detailsResp.items.map { it.toDomain() }, apiKey)
            ApiResult.Success(related)
        } catch (e: HttpException) {
            ApiResult.Error(parseHttpError(e), e.code())
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to load related videos")
        }
    }

    suspend fun getVideoComments(videoId: String): ApiResult<List<VideoComment>> {
        val apiKey = resolveApiKey()
            ?: return ApiResult.MissingApiKey("YOUTUBE_API_KEY")

        return try {
            val resp = api.getVideoComments(videoId = videoId, apiKey = apiKey)
            val comments = resp.items.mapNotNull { thread ->
                val c = thread.snippet?.topLevelComment
                val s = c?.snippet ?: return@mapNotNull null
                VideoComment(
                    id = c.id ?: thread.id,
                    authorName = s.authorDisplayName ?: "Viewer",
                    authorAvatarUrl = s.authorProfileImageUrl.orEmpty(),
                    text = s.textOriginal ?: s.textDisplay.orEmpty(),
                    likeCount = s.likeCount ?: 0L,
                    publishedAt = s.publishedAt.orEmpty()
                )
            }
            ApiResult.Success(comments)
        } catch (e: HttpException) {
            ApiResult.Error(parseHttpError(e), e.code())
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Comments unavailable")
        }
    }

    suspend fun recordWatchHistory(video: VideoItem) {
        val existing = dao.getHistoryItem(video.id)
        dao.upsertWatchHistory(
            WatchHistoryEntity(
                videoId = video.id,
                title = video.title,
                channelId = video.channelId,
                channelTitle = video.channelTitle,
                thumbnailUrl = video.thumbnailUrl,
                durationText = video.durationText,
                viewCount = video.viewCount,
                publishedAt = video.publishedAt,
                watchedAt = System.currentTimeMillis(),
                isSavedToWatchlist = existing?.isSavedToWatchlist ?: false
            )
        )
    }

    suspend fun toggleWatchlist(video: VideoItem): Boolean {
        val existing = dao.getHistoryItem(video.id)
        val newSaved = !(existing?.isSavedToWatchlist ?: false)
        dao.upsertWatchHistory(
            WatchHistoryEntity(
                videoId = video.id,
                title = video.title,
                channelId = video.channelId,
                channelTitle = video.channelTitle,
                thumbnailUrl = video.thumbnailUrl,
                durationText = video.durationText,
                viewCount = video.viewCount,
                publishedAt = video.publishedAt,
                watchedAt = existing?.watchedAt ?: System.currentTimeMillis(),
                isSavedToWatchlist = newSaved
            )
        )
        return newSaved
    }

    suspend fun isVideoInWatchlist(videoId: String): Boolean {
        return dao.getHistoryItem(videoId)?.isSavedToWatchlist == true
    }

    suspend fun clearWatchHistory() = dao.clearNonSavedHistory()

    suspend fun deleteSearchQuery(query: String) = dao.deleteSearchQuery(query)

    suspend fun clearSearchHistory() = dao.clearSearchHistory()

    private suspend fun enrichWithChannelAvatars(
        videos: List<VideoItem>,
        apiKey: String
    ): List<VideoItem> {
        val channelIds = videos.map { it.channelId }.filter { it.isNotBlank() }.distinct().take(25)
        if (channelIds.isEmpty()) return videos
        return try {
            val channelsResp = api.getChannelsByIds(ids = channelIds.joinToString(","), apiKey = apiKey)
            val avatarMap = channelsResp.items.associate {
                it.id to it.snippet?.thumbnails?.bestUrl().orEmpty()
            }
            videos.map { video ->
                video.copy(channelAvatarUrl = avatarMap[video.channelId] ?: video.channelAvatarUrl)
            }
        } catch (_: Exception) {
            videos
        }
    }

    private fun YouTubeVideoResource.toDomain(): VideoItem {
        val s = snippet
        val stats = statistics
        return VideoItem(
            id = id,
            title = s?.title.orEmpty(),
            description = s?.description.orEmpty(),
            channelId = s?.channelId.orEmpty(),
            channelTitle = s?.channelTitle.orEmpty(),
            thumbnailUrl = s?.thumbnails?.bestUrl()
                ?.ifBlank { "https://i.ytimg.com/vi/$id/hqdefault.jpg" }
                ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            publishedAt = s?.publishedAt.orEmpty(),
            durationText = FormatUtils.formatIsoDuration(contentDetails?.duration),
            viewCount = stats?.viewCount?.toLongOrNull() ?: 0L,
            likeCount = stats?.likeCount?.toLongOrNull() ?: 0L,
            commentCount = stats?.commentCount?.toLongOrNull() ?: 0L
        )
    }

    private fun parseHttpError(e: HttpException): String {
        return when (e.code()) {
            400 -> "Invalid YouTube API request or key."
            403 -> "YouTube API quota exceeded or key restricted (403)."
            404 -> "Requested YouTube resource was not found."
            else -> "YouTube API error (${e.code()})"
        }
    }
}
