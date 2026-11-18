package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DEFAULT_DOWNLOAD_API_BASE_URL
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.ApiResult
import com.example.data.repository.DownloadRepository
import com.example.data.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val youTubeRepository: YouTubeRepository,
    private val downloadRepository: DownloadRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val watchlist: StateFlow<List<WatchHistoryEntity>> = youTubeRepository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = youTubeRepository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _isSyncingConfig = MutableStateFlow(false)
    val isSyncingConfig: StateFlow<Boolean> = _isSyncingConfig.asStateFlow()

    private val _configMessage = MutableStateFlow<String?>(null)
    val configMessage: StateFlow<String?> = _configMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val savedRemoteUrl = preferencesRepository.preferencesFlow.first().remoteConfigUrl
            if (savedRemoteUrl.isNotBlank()) {
                downloadRepository.syncRemoteConfig(savedRemoteUrl)
            }
        }
    }

    fun setAutoExportToGallery(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoExportToGallery(enabled)
        }
    }

    fun setRegionCode(region: String) {
        viewModelScope.launch {
            preferencesRepository.setRegionCode(region)
        }
    }

    fun saveDownloadApiUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.setDownloadApiBaseUrl(url)
            _configMessage.value = "Download API URL updated."
        }
    }

    fun resetDownloadApiUrl() {
        viewModelScope.launch {
            preferencesRepository.setDownloadApiBaseUrl(DEFAULT_DOWNLOAD_API_BASE_URL)
            _configMessage.value = "Download API URL reset to default Railway endpoint."
        }
    }

    fun syncRemoteConfig(configUrl: String) {
        viewModelScope.launch {
            _isSyncingConfig.value = true
            _configMessage.value = null
            when (val res = downloadRepository.syncRemoteConfig(configUrl)) {
                is ApiResult.Success -> {
                    _configMessage.value = "Synced remote config (API key & Download URL updated)."
                }
                is ApiResult.Error -> {
                    _configMessage.value = res.message
                }
                is ApiResult.MissingApiKey -> Unit
            }
            _isSyncingConfig.value = false
        }
    }

    fun clearConfigMessage() {
        _configMessage.value = null
    }

    fun clearHistory() {
        viewModelScope.launch {
            youTubeRepository.clearWatchHistory()
        }
    }

    class Factory(
        private val youTubeRepository: YouTubeRepository,
        private val downloadRepository: DownloadRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(youTubeRepository, downloadRepository, preferencesRepository) as T
        }
    }
}
