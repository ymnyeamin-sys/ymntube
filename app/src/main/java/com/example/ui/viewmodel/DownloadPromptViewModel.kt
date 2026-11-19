package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.DownloadedVideoEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.ApiResult
import com.example.data.model.AppUpdateInfo
import com.example.data.model.DownloadFormat
import com.example.data.model.VideoItem
import com.example.data.remote.FormatUtils
import com.example.data.repository.AppUpdateCheckResult
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DownloadSheetState {
    data object Hidden : DownloadSheetState
    data class Resolving(val title: String) : DownloadSheetState
    data class Ready(
        val video: VideoItem,
        val formats: List<DownloadFormat>
    ) : DownloadSheetState
    data class Error(
        val title: String,
        val message: String
    ) : DownloadSheetState
}

class DownloadPromptViewModel(
    private val downloadRepository: DownloadRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _sheetState = MutableStateFlow<DownloadSheetState>(DownloadSheetState.Hidden)
    val sheetState: StateFlow<DownloadSheetState> = _sheetState.asStateFlow()

    val activeDownloads = downloadRepository.activeDownloads
    val downloadedVideos: StateFlow<List<DownloadedVideoEntity>> =
        downloadRepository.downloadedVideos.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    fun checkForAppUpdate(manual: Boolean = false) {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            try {
                when (val result = downloadRepository.checkForAppUpdate(ignoreAcknowledged = manual)) {
                    is AppUpdateCheckResult.Available -> {
                        _availableUpdate.value = result.info
                    }
                    is AppUpdateCheckResult.Disabled -> {
                        if (manual) {
                            _statusToast.value = "App update is turned off right now"
                        }
                    }
                    is AppUpdateCheckResult.UpToDate -> {
                        if (manual) {
                            _statusToast.value = "YmnTube v${result.installedVersion} is up to date"
                        }
                    }
                    is AppUpdateCheckResult.Error -> {
                        if (manual) {
                            _statusToast.value = "Update check failed: ${result.message}"
                        }
                    }
                }
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun onUpdateAccepted(info: AppUpdateInfo) {
        _availableUpdate.value = null
        viewModelScope.launch {
            downloadRepository.acknowledgeUpdateVersion(info.version)
        }
    }

    fun onSkipUpdateVersion(info: AppUpdateInfo) {
        _availableUpdate.value = null
        viewModelScope.launch {
            downloadRepository.acknowledgeUpdateVersion(info.version)
        }
    }

    fun dismissUpdateDialog() {
        _availableUpdate.value = null
    }

    fun openDownloadSheet(video: VideoItem) {
        _sheetState.value = DownloadSheetState.Resolving(video.title.ifBlank { "Fetching resolutions…" })
        viewModelScope.launch {
            when (val result = downloadRepository.resolveFormatsForVideo(video.id, video)) {
                is ApiResult.Success -> {
                    _sheetState.value = DownloadSheetState.Ready(
                        video = result.data.video,
                        formats = result.data.formats
                    )
                }
                is ApiResult.MissingApiKey -> {
                    _sheetState.value = DownloadSheetState.Error(
                        title = video.title,
                        message = "API configuration required."
                    )
                }
                is ApiResult.Error -> {
                    _sheetState.value = DownloadSheetState.Error(
                        title = video.title,
                        message = result.message
                    )
                }
            }
        }
    }

    fun openDownloadFromInput(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        if (FormatUtils.isDirectMediaUrl(trimmed)) {
            downloadRepository.startDirectUrlDownload(trimmed)
            _statusToast.value = "Downloading direct media…"
            return
        }

        val videoId = FormatUtils.extractYouTubeVideoId(trimmed)
        if (videoId == null) {
            _statusToast.value = "Enter a valid YouTube link, video ID, or direct MP4 URL"
            return
        }

        openDownloadSheet(
            VideoItem(
                id = videoId,
                title = "Resolving $videoId…",
                description = "",
                channelId = "",
                channelTitle = "",
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                publishedAt = ""
            )
        )
    }

    fun startSelectedDownload(video: VideoItem, format: DownloadFormat) {
        _sheetState.value = DownloadSheetState.Hidden
        viewModelScope.launch {
            val autoGallery = preferencesRepository.preferencesFlow.first().autoExportToGallery
            downloadRepository.startDownload(video, format, exportToGallery = autoGallery)
            _statusToast.value = "Downloading ${format.qualityLabel}…"
        }
    }

    fun dismissSheet() {
        _sheetState.value = DownloadSheetState.Hidden
    }

    fun cancelDownload(downloadId: String) {
        downloadRepository.cancelDownload(downloadId)
    }

    fun dismissActiveItem(downloadId: String) {
        downloadRepository.dismissCompletedOrFailed(downloadId)
    }

    fun openInGallery(entity: DownloadedVideoEntity) {
        val opened = downloadRepository.openInGallery(entity)
        if (!opened) {
            _statusToast.value = "Could not open Gallery viewer on this device"
        }
    }

    fun exportToGallery(entity: DownloadedVideoEntity) {
        viewModelScope.launch {
            val ok = downloadRepository.exportExistingToGallery(entity)
            _statusToast.value = if (ok) {
                "Saved to device Gallery (Movies/YmnTube)"
            } else {
                "Gallery export failed — check storage permissions"
            }
        }
    }

    fun shareVideo(entity: DownloadedVideoEntity) {
        val ok = downloadRepository.shareDownloadedVideo(entity)
        if (!ok) {
            _statusToast.value = "Export to Gallery first to share via system sheet"
        }
    }

    fun deleteVideo(entity: DownloadedVideoEntity) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadedVideo(entity)
            _statusToast.value = "Removed from YmnTube Vault"
        }
    }

    fun clearToast() {
        _statusToast.value = null
    }

    class Factory(
        private val downloadRepository: DownloadRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DownloadPromptViewModel(downloadRepository, preferencesRepository) as T
        }
    }
}
