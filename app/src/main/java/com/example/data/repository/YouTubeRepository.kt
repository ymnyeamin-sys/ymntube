package com.example.data.repository

import com.example.data.local.DEFAULT_REGION_CODE
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    /**
     * Personalized Home feed analyzing overall user taste across watch history, watchlist,
     * and search history — blending topic clusters, favorite creators, fresh discovery,
     * and regional videos with strict anti-repetition & title deduplication.
     */
    suspend fun getHomeSuggestions(
        category: YouTubeCategory,
        regionCode: String = DEFAULT_REGION_CODE
    ): ApiResult<List<VideoItem>> = coroutineScope {
        val apiKey = resolveApiKey()
            ?: return@coroutineScope ApiResult.MissingApiKey("YOUTUBE_API_KEY")

        try {
            if (category == YouTubeCategory.ALL) {
                val recentSearches = runCatching { dao.observeSearchHistory().first() }.getOrDefault(emptyList())
                val recentWatches = runCatching { dao.observeWatchHistory().first() }.getOrDefault(emptyList())
                val savedVideos = runCatching { dao.observeWatchlist().first() }.getOrDefault(emptyList())

                val tasteQueries = HomeSuggestionEngine.buildDiverseTasteQueries(
                    searches = recentSearches,
                    watches = recentWatches,
                    saved = savedVideos,
                    regionCode = regionCode
                )

                val popularDeferred = async {
                    runCatching {
                        api.getPopularVideos(
                            regionCode = regionCode,
                            videoCategoryId = null,
                            maxResults = 20,
                            apiKey = apiKey
                        ).items.map { it.toDomain() }
                    }.getOrDefault(emptyList())
                }

                val personalizedDeferreds = tasteQueries.map { query ->
                    async {
                        runCatching {
                            val searchResp = api.searchVideos(
                                query = query,
                                maxResults = 8,
                                apiKey = apiKey
                            )
                            val ids = searchResp.items.mapNotNull { it.id?.videoId }.distinct()
                            if (ids.isEmpty()) emptyList()
                            else api.getVideosByIds(ids = ids.joinToString(","), apiKey = apiKey)
                                .items.map { it.toDomain() }
                        }.getOrDefault(emptyList())
                    }
                }

                val personalizedBuckets = personalizedDeferreds.map { it.await() }
                val regionalVideos = popularDeferred.await()

                val allBuckets = personalizedBuckets + listOf(regionalVideos)
                val watchedIds = recentWatches.map { it.videoId }.toSet()
                val preferredChannels = (savedVideos + recentWatches)
                    .map { it.channelTitle.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()

                val diverse = HomeSuggestionEngine.rankAndDiversify(
                    buckets = allBuckets,
                    watchedIds = watchedIds,
                    preferredChannels = preferredChannels,
                    limit = 30
                )

                if (diverse.isEmpty()) {
                    val fallbackResp = api.searchVideos(
                        query = if (regionCode == "BD") "Bangla music natok tech vlog" else "popular new videos",
                        maxResults = 20,
                        apiKey = apiKey
                    )
                    val ids = fallbackResp.items.mapNotNull { it.id?.videoId }.distinct()
                    if (ids.isEmpty()) return@coroutineScope ApiResult.Success(emptyList())
                    val details = api.getVideosByIds(ids = ids.joinToString(","), apiKey = apiKey)
                    val fallbackDiverse = HomeSuggestionEngine.rankAndDiversify(
                        buckets = listOf(details.items.map { it.toDomain() }),
                        watchedIds = watchedIds,
                        preferredChannels = preferredChannels,
                        limit = 24
                    )
                    val enriched = enrichWithChannelAvatars(fallbackDiverse, apiKey)
                    return@coroutineScope ApiResult.Success(enriched)
                }

                val enriched = enrichWithChannelAvatars(diverse, apiKey)
                return@coroutineScope ApiResult.Success(enriched)
            } else {
                val categoryVideos = runCatching {
                    api.getPopularVideos(
                        regionCode = regionCode,
                        videoCategoryId = category.categoryId,
                        maxResults = 24,
                        apiKey = apiKey
                    ).items.map { it.toDomain() }
                }.getOrNull().orEmpty()

                if (categoryVideos.isNotEmpty()) {
                    return@coroutineScope ApiResult.Success(enrichWithChannelAvatars(categoryVideos, apiKey))
                }

                val searchResp = api.searchVideos(
                    query = "${category.label} ${if (regionCode == "BD") "Bangladesh" else ""}".trim(),
                    maxResults = 24,
                    apiKey = apiKey
                )
                val ids = searchResp.items.mapNotNull { it.id?.videoId }.distinct()
                if (ids.isEmpty()) return@coroutineScope ApiResult.Success(emptyList())
                val details = api.getVideosByIds(ids = ids.joinToString(","), apiKey = apiKey)
                val videos = enrichWithChannelAvatars(details.items.map { it.toDomain() }, apiKey)
                return@coroutineScope ApiResult.Success(videos)
            }
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

internal object HomeSuggestionEngine {
    private val noiseWords = setOf(
        "official", "video", "videos", "music", "audio", "song", "songs", "full",
        "lyric", "lyrics", "lyrical", "new", "latest", "status", "short", "shorts",
        "reel", "reels", "remix", "cover", "version", "live", "hd", "4k", "1080p",
        "720p", "mp3", "mp4", "part", "episode", "ep", "teaser", "trailer", "viral",
        "trending", "best", "top", "super", "hit", "hits", "feat", "ft", "vs",
        "with", "from", "this", "that", "your", "you", "are", "for", "and", "the",
        "2023", "2024", "2025", "2026", "watch", "play"
    )

    private data class TasteCategoryRule(
        val id: String,
        val triggers: Set<String>,
        val bdQuery: String,
        val globalQuery: String
    )

    private val categoryRules = listOf(
        TasteCategoryRule(
            id = "bangla_music",
            triggers = setOf("bangla", "বাংলা", "গান", "gaan", "gan", "habib", "tahsan", "imran", "minar", "asif", "arnob", "artcell", "warfed", "lrb", "james", "ayub", "konal", "readoy", "pora"),
            bdQuery = "new Bangla romantic sad music mix",
            globalQuery = "Bangla new music hits playlist"
        ),
        TasteCategoryRule(
            id = "music",
            triggers = setOf("song", "songs", "music", "lofi", "melody", "singer", "band", "arijit", "atif", "shreya", "arjit", "jubin", "qawwali", "ghazal", "gazal", "rap", "hiphop", "rock", "pop", "nasheed"),
            bdQuery = "new Hindi Bangla trending songs playlist",
            globalQuery = "new trending music mix playlist"
        ),
        TasteCategoryRule(
            id = "natok_drama",
            triggers = setOf("natok", "নাটক", "drama", "telefilm", "movie", "film", "cinema", "webseries", "series", "comedy", "apurba", "afran", "nisho", "mehazabien", "tawsif", "mosharraf"),
            bdQuery = "new Bangla natok drama 2026",
            globalQuery = "new comedy drama short film"
        ),
        TasteCategoryRule(
            id = "tech",
            triggers = setOf("tech", "phone", "mobile", "smartphone", "review", "unboxing", "android", "iphone", "samsung", "xiaomi", "laptop", "pc", "coding", "programming", "kotlin", "ai", "gadget", "camera"),
            bdQuery = "Bangla tech smartphone review latest",
            globalQuery = "latest tech smartphone gadgets review"
        ),
        TasteCategoryRule(
            id = "gaming",
            triggers = setOf("game", "gaming", "gameplay", "pubg", "freefire", "valorant", "minecraft", "gta", "fifa", "efootball", "esports", "walkthrough"),
            bdQuery = "gaming highlights live stream Bangladesh",
            globalQuery = "gaming highlights walkthrough new"
        ),
        TasteCategoryRule(
            id = "sports",
            triggers = setOf("cricket", "football", "ipl", "bpl", "match", "highlights", "goal", "goals", "worldcup", "messi", "ronaldo", "shakib", "tamim", "tigers"),
            bdQuery = "cricket football match highlights",
            globalQuery = "football cricket sports highlights"
        ),
        TasteCategoryRule(
            id = "knowledge",
            triggers = setOf("news", "podcast", "interview", "documentary", "history", "science", "space", "mystery", "facts", "fact", "explained", "story", "analysis"),
            bdQuery = "Bangla documentary podcast science story",
            globalQuery = "interesting documentary science podcast"
        ),
        TasteCategoryRule(
            id = "vlog_food",
            triggers = setOf("vlog", "travel", "tour", "food", "streetfood", "recipe", "cooking", "kitchen", "biryani", "village"),
            bdQuery = "Bangladeshi travel street food vlog",
            globalQuery = "travel food vlog adventure"
        ),
        TasteCategoryRule(
            id = "islamic",
            triggers = setOf("waz", "islamic", "quran", "tilawat", "azan", "ahmadullah", "mizanur", "azhari", "tariq", "lecture", "motivation"),
            bdQuery = "new Bangla islamic waz lecture",
            globalQuery = "islamic reminder motivation lecture"
        )
    )

    fun extractCoreTokens(text: String): List<String> {
        val withoutBrackets = text
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^\\]]*\\]"), " ")
        return withoutBrackets
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .lowercase()
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { token ->
                token.length >= 2 &&
                    token !in noiseWords &&
                    !token.all { it.isDigit() }
            }
            .distinct()
    }

    fun areTitlesSimilar(titleA: String, titleB: String): Boolean {
        val tokensA = extractCoreTokens(titleA)
        val tokensB = extractCoreTokens(titleB)
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return titleA.trim().equals(titleB.trim(), ignoreCase = true)
        }
        val setA = tokensA.toSet()
        val setB = tokensB.toSet()
        val intersection = setA.intersect(setB).size
        if (intersection == 0) return false
        val union = setA.union(setB).size
        val jaccard = intersection.toFloat() / union.toFloat()
        if (jaccard >= 0.48f) return true

        val prefixLen = minOf(3, minOf(tokensA.size, tokensB.size))
        if (prefixLen >= 2 && tokensA.take(prefixLen) == tokensB.take(prefixLen)) {
            return true
        }
        return false
    }

    fun buildDiverseTasteQueries(
        searches: List<SearchHistoryEntity>,
        watches: List<WatchHistoryEntity>,
        saved: List<WatchHistoryEntity>,
        regionCode: String
    ): List<String> {
        val queries = mutableListOf<String>()
        val isBd = regionCode.equals("BD", ignoreCase = true)

        // Aggregate overall weighted tokens and title-clusters to avoid repeating a single video
        val topicScores = mutableMapOf<String, Int>()
        val seenClusters = mutableListOf<String>()
        val channelCounts = mutableMapOf<String, Int>()
        val categoryScores = mutableMapOf<String, Int>()

        fun scoreText(text: String, weight: Int, dedupeCluster: Boolean) {
            if (dedupeCluster && seenClusters.any { areTitlesSimilar(it, text) }) {
                return
            }
            if (dedupeCluster) seenClusters.add(text)

            val rawTokens = text
                .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.length >= 2 }

            for (rule in categoryRules) {
                if (rawTokens.any { it in rule.triggers }) {
                    categoryScores[rule.id] = (categoryScores[rule.id] ?: 0) + weight
                }
            }

            extractCoreTokens(text).forEach { tok ->
                if (tok.length >= 3) {
                    topicScores[tok] = (topicScores[tok] ?: 0) + weight
                }
            }
        }

        saved.take(20).forEach { entry ->
            scoreText(entry.title, weight = 3, dedupeCluster = true)
            if (entry.channelTitle.isNotBlank()) {
                channelCounts[entry.channelTitle.trim()] =
                    (channelCounts[entry.channelTitle.trim()] ?: 0) + 3
            }
        }
        watches.take(35).forEach { entry ->
            scoreText(entry.title, weight = 2, dedupeCluster = true)
            if (entry.channelTitle.isNotBlank()) {
                channelCounts[entry.channelTitle.trim()] =
                    (channelCounts[entry.channelTitle.trim()] ?: 0) + 2
            }
        }
        searches.take(12).forEach { search ->
            scoreText(search.query, weight = 2, dedupeCluster = true)
        }

        // 1. Top inferred broad interest categories (gives NEW videos in user's taste!)
        val topCategories = categoryScores.entries
            .sortedByDescending { it.value }
            .mapNotNull { entry -> categoryRules.firstOrNull { it.id == entry.key } }
            .take(2)
        topCategories.forEach { rule ->
            queries.add(if (isBd) rule.bdQuery else rule.globalQuery)
        }

        // 2. Favorite creator's fresh/other uploads
        val topCreator = channelCounts.entries
            .sortedByDescending { it.value }
            .firstOrNull()
            ?.key
            ?.trim()
        if (!topCreator.isNullOrBlank()) {
            queries.add("$topCreator new video")
        }

        // 3. Cross-topic blend from distinct keywords across different videos (never copying one title)
        val topKeywords = topicScores.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(4)
        if (topKeywords.isNotEmpty()) {
            val blend = topKeywords.take(2).joinToString(" ")
            queries.add("$blend new mix")
        } else if (searches.isNotEmpty()) {
            val latestSearch = searches.first().query.trim()
            if (latestSearch.length >= 2) queries.add("$latestSearch mix")
        }

        // 4. Adjacent discovery query so home always introduces fresh variety
        val discoveryPool = if (isBd) {
            listOf(
                "new Bangla song playlist",
                "Bangla tech vlog science new",
                "new Bangla natok comedy",
                "Bangladesh travel documentary"
            )
        } else {
            listOf(
                "new trending music mix",
                "latest tech science documentary",
                "comedy gaming highlights new"
            )
        }
        val unusedDiscovery = discoveryPool.firstOrNull { cand ->
            queries.none { q -> q.equals(cand, ignoreCase = true) }
        }
        if (unusedDiscovery != null && queries.size < 4) {
            queries.add(unusedDiscovery)
        }

        return queries.distinct().take(4)
    }

    fun rankAndDiversify(
        buckets: List<List<VideoItem>>,
        watchedIds: Set<String>,
        preferredChannels: Set<String>,
        limit: Int = 30
    ): List<VideoItem> {
        // Round-robin interleave buckets so no single query dominates the top of the feed
        val interleaved = mutableListOf<VideoItem>()
        val maxSize = buckets.maxOfOrNull { it.size } ?: 0
        for (i in 0 until maxSize) {
            for (bucket in buckets) {
                if (i < bucket.size) {
                    interleaved.add(bucket[i])
                }
            }
        }

        val uniqueById = interleaved.distinctBy { it.id }
        val (freshCandidates, seenCandidates) = uniqueById.partition { it.id !in watchedIds }
        val orderedCandidates = freshCandidates + seenCandidates

        val accepted = mutableListOf<VideoItem>()
        val acceptedPerChannel = mutableMapOf<String, Int>()

        for (candidate in orderedCandidates) {
            if (accepted.size >= limit) break

            val channelKey = candidate.channelTitle.trim().lowercase()
            val channelCount = if (channelKey.isNotBlank()) acceptedPerChannel[channelKey] ?: 0 else 0
            val channelCap = if (channelKey in preferredChannels) 3 else 2
            if (channelCount >= channelCap) continue

            // Drop near-duplicate titles of videos already selected in the feed
            if (accepted.any { existing -> areTitlesSimilar(existing.title, candidate.title) }) {
                continue
            }

            accepted.add(candidate)
            if (channelKey.isNotBlank()) {
                acceptedPerChannel[channelKey] = channelCount + 1
            }
        }

        // Only pad with remaining non-duplicate IDs if strict diversity left fewer than 10 items
        if (accepted.size < 10) {
            val acceptedIds = accepted.map { it.id }.toSet()
            for (candidate in orderedCandidates) {
                if (accepted.size >= limit) break
                if (candidate.id !in acceptedIds) {
                    accepted.add(candidate)
                }
            }
        }

        return accepted.take(limit)
    }
}
