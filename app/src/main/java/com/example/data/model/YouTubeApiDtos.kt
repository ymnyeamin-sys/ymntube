package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    val items: List<YouTubeVideoResource> = emptyList(),
    val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoResource(
    val id: String,
    val snippet: YouTubeSnippet? = null,
    val contentDetails: YouTubeContentDetails? = null,
    val statistics: YouTubeStatistics? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchListResponse(
    val items: List<YouTubeSearchResource> = emptyList(),
    val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResource(
    val id: YouTubeSearchId? = null,
    val snippet: YouTubeSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchId(
    val kind: String? = null,
    val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    val publishedAt: String? = null,
    val channelId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val thumbnails: YouTubeThumbnails? = null,
    val channelTitle: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    val default: YouTubeThumbnail? = null,
    val medium: YouTubeThumbnail? = null,
    val high: YouTubeThumbnail? = null,
    val standard: YouTubeThumbnail? = null,
    val maxres: YouTubeThumbnail? = null
) {
    fun bestUrl(): String {
        return maxres?.url
            ?: standard?.url
            ?: high?.url
            ?: medium?.url
            ?: default?.url
            ?: ""
    }
}

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeContentDetails(
    val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null,
    val commentCount: String? = null,
    val subscriberCount: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelListResponse(
    val items: List<YouTubeChannelResource> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelResource(
    val id: String,
    val snippet: YouTubeSnippet? = null,
    val statistics: YouTubeStatistics? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentThreadsResponse(
    val items: List<YouTubeCommentThreadResource> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentThreadResource(
    val id: String,
    val snippet: YouTubeCommentThreadSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentThreadSnippet(
    val topLevelComment: YouTubeCommentResource? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentResource(
    val id: String? = null,
    val snippet: YouTubeCommentSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentSnippet(
    val authorDisplayName: String? = null,
    val authorProfileImageUrl: String? = null,
    @Json(name = "textDisplay") val textDisplay: String? = null,
    @Json(name = "textOriginal") val textOriginal: String? = null,
    val likeCount: Long? = null,
    val publishedAt: String? = null
)
