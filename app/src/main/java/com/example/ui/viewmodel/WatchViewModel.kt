package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadedVideoEntity
import com.example.data.model.ApiResult
import com.example.data.model.ChannelInfo
import com.example.data.model.VideoComment
import com.example.data.model.VideoItem
import com.example.data.repository.DownloadRepository
import com.example.data.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlayerMode {
    YOUTUBE_EMBED,
    EXO_NATIVE_STREAM,
    OFFLINE_VAULT
}

data class WatchUiState(
    val video: VideoItem,
    val channelInfo: ChannelInfo? = null,
    val relatedVideos: List<VideoItem> = emptyList(),
    val comments: List<VideoComment> = emptyList(),
    val isLoadingDetails: Boolean = true,
    val isLoadingComments: Boolean = false,
    val isSavedToWatchlist: Boolean = false,
    val offlineDownload: DownloadedVideoEntity? = null,
    val offlineVariants: List<DownloadedVideoEntity> = emptyList(),
    val directStreamUrl: String? = null,
    val playerMode: PlayerMode = PlayerMode.YOUTUBE_EMBED,
    val isResolvingStream: Boolean = false,
    val streamErrorMessage: String? = null,
    val errorMessage: String? = null
)

class WatchViewModel(
    private val videoId: String,
    initialTitle: String,
    initialChannel: String,
    initialThumb: String,
    private val youTubeRepository: YouTubeRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WatchUiState(
            video = VideoItem(
                id = videoId,
                title = initialTitle.ifBlank { "Loading video…" },
                description = "",
                channelId = "",
                channelTitle = initialChannel,
                thumbnailUrl = initialThumb.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                publishedAt = ""
            )
        )
    )
    val uiState: StateFlow<WatchUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        observeOfflineStatus()
    }

    private fun observeOfflineStatus() {
        viewModelScope.launch {
            downloadRepository.downloadedVideos.collect { list ->
                val variants = list.filter { it.videoId == videoId }
                val current = _uiState.value
                val preferredMatch = current.offlineDownload?.let { prev ->
                    variants.firstOrNull { it.downloadId == prev.downloadId }
                } ?: variants.firstOrNull()
                val nextMode = if (preferredMatch != null &&
                    current.playerMode == PlayerMode.YOUTUBE_EMBED &&
                    current.directStreamUrl == null
                ) {
                    PlayerMode.OFFLINE_VAULT
                } else {
                    current.playerMode
                }
                _uiState.value = current.copy(
                    offlineDownload = preferredMatch,
                    offlineVariants = variants,
                    playerMode = nextMode
                )
            }
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetails = true, errorMessage = null)
            val saved = youTubeRepository.isVideoInWatchlist(videoId)
            val variants = downloadRepository.getDownloadedVideosForVideo(videoId)
            val local = variants.firstOrNull()

            if (local != null) {
                _uiState.value = _uiState.value.copy(
                    offlineDownload = local,
                    offlineVariants = variants,
                    playerMode = PlayerMode.OFFLINE_VAULT
                )
            }

            when (val detailsRes = youTubeRepository.getVideoDetails(videoId)) {
                is ApiResult.Success -> {
                    val video = detailsRes.data
                    youTubeRepository.recordWatchHistory(video)
                    _uiState.value = _uiState.value.copy(
                        video = video,
                        isLoadingDetails = false,
                        isSavedToWatchlist = saved
                    )
                    loadSecondaryData(video)
                }
                is ApiResult.MissingApiKey -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetails = false,
                        isSavedToWatchlist = saved,
                        errorMessage = "Video metadata syncing — check connection if details do not load."
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetails = false,
                        isSavedToWatchlist = saved,
                        errorMessage = detailsRes.message
                    )
                }
            }
        }
    }

    private fun loadSecondaryData(video: VideoItem) {
        viewModelScope.launch {
            val channelRes = youTubeRepository.getChannelInfo(video.channelId)
            if (channelRes is ApiResult.Success) {
                _uiState.value = _uiState.value.copy(channelInfo = channelRes.data)
            }
        }
        viewModelScope.launch {
            val relatedRes = youTubeRepository.getRelatedVideos(video)
            if (relatedRes is ApiResult.Success) {
                _uiState.value = _uiState.value.copy(relatedVideos = relatedRes.data)
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            val commentsRes = youTubeRepository.getVideoComments(video.id)
            _uiState.value = _uiState.value.copy(
                comments = (commentsRes as? ApiResult.Success)?.data ?: emptyList(),
                isLoadingComments = false
            )
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val newSaved = youTubeRepository.toggleWatchlist(_uiState.value.video)
            _uiState.value = _uiState.value.copy(isSavedToWatchlist = newSaved)
        }
    }

    private var hasAutoFallbackAttempted = false

    fun switchToYouTubeEmbed() {
        _uiState.value = _uiState.value.copy(
            playerMode = PlayerMode.YOUTUBE_EMBED,
            streamErrorMessage = null
        )
    }

    fun switchToOfflinePlayer(selected: DownloadedVideoEntity? = null) {
        val target = selected ?: _uiState.value.offlineDownload
        if (target != null) {
            _uiState.value = _uiState.value.copy(
                offlineDownload = target,
                playerMode = PlayerMode.OFFLINE_VAULT,
                streamErrorMessage = null
            )
        }
    }

    fun onWebPlayerError(errorCode: Int) {
        if (hasAutoFallbackAttempted || _uiState.value.playerMode != PlayerMode.YOUTUBE_EMBED) return
        hasAutoFallbackAttempted = true
        tryNativeExoStream(autoTriggeredByEmbedError = errorCode)
    }

    fun onNativePlaybackError(message: String) {
        _uiState.value = _uiState.value.copy(
            directStreamUrl = null,
            isResolvingStream = false,
            streamErrorMessage = message
        )
    }

    fun tryNativeExoStream(autoTriggeredByEmbedError: Int? = null) {
        val currentUrl = _uiState.value.directStreamUrl
        if (currentUrl != null) {
            _uiState.value = _uiState.value.copy(
                playerMode = PlayerMode.EXO_NATIVE_STREAM,
                streamErrorMessage = null
            )
            return
        }
        if (_uiState.value.isResolvingStream) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                playerMode = PlayerMode.EXO_NATIVE_STREAM,
                isResolvingStream = true,
                streamErrorMessage = null
            )
            val res = downloadRepository.resolvePlayableStreamUrl(videoId)
            if (res is ApiResult.Success) {
                _uiState.value = _uiState.value.copy(
                    directStreamUrl = res.data,
                    playerMode = PlayerMode.EXO_NATIVE_STREAM,
                    isResolvingStream = false,
                    streamErrorMessage = null
                )
            } else {
                val msg = (res as? ApiResult.Error)?.message ?: "Could not resolve native stream"
                val prefix = if (autoTriggeredByEmbedError != null) {
                    "YouTube embed restricted (Error $autoTriggeredByEmbedError). "
                } else ""
                _uiState.value = _uiState.value.copy(
                    isResolvingStream = false,
                    streamErrorMessage = "$prefix$msg"
                )
            }
        }
    }

    class Factory(
        private val videoId: String,
        private val initialTitle: String,
        private val initialChannel: String,
        private val initialThumb: String,
        private val youTubeRepository: YouTubeRepository,
        private val downloadRepository: DownloadRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WatchViewModel(
                videoId,
                initialTitle,
                initialChannel,
                initialThumb,
                youTubeRepository,
                downloadRepository
            ) as T
        }
    }
}
