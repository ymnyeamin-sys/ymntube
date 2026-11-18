package com.example.data.model

enum class YouTubeCategory(
    val label: String,
    val categoryId: String?
) {
    ALL("For You", null),
    MUSIC("Music", "10"),
    GAMING("Gaming", "20"),
    TECH("Tech", "28"),
    SPORTS("Sports", "17"),
    NEWS("News", "25"),
    EDUCATION("Learn", "27")
}

data class VideoItem(
    val id: String,
    val title: String,
    val description: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String? = null,
    val thumbnailUrl: String,
    val publishedAt: String,
    val durationText: String = "",
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L
)

data class ChannelInfo(
    val id: String,
    val title: String,
    val avatarUrl: String,
    val subscriberCount: Long
)

data class VideoComment(
    val id: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val text: String,
    val likeCount: Long,
    val publishedAt: String
)

data class DownloadFormat(
    val formatId: String,
    val qualityLabel: String,
    val mimeType: String,
    val ext: String,
    val url: String,
    val contentLengthBytes: Long,
    val hasAudio: Boolean,
    val hasVideo: Boolean,
    val bitrate: Long = 0L,
    val rawFormatId: String? = null
)

enum class DownloadJobStatus {
    QUEUED,
    RESOLVING,
    DOWNLOADING,
    EXPORTING_GALLERY,
    COMPLETED,
    FAILED
}

data class ActiveDownload(
    val downloadId: String,
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val qualityLabel: String,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadJobStatus = DownloadJobStatus.QUEUED,
    val errorMessage: String? = null
)

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>
    data class MissingApiKey(val keyName: String) : ApiResult<Nothing>
}
