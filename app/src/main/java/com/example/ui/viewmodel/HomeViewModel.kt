package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.ApiResult
import com.example.data.model.VideoItem
import com.example.data.model.YouTubeCategory
import com.example.data.repository.DownloadRepository
import com.example.data.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(val videos: List<VideoItem>) : FeedUiState
    data class Error(val message: String) : FeedUiState
    data class MissingApiKey(val keyName: String) : FeedUiState
}

class HomeViewModel(
    private val youTubeRepository: YouTubeRepository,
    private val downloadRepository: DownloadRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(YouTubeCategory.ALL)
    val selectedCategory: StateFlow<YouTubeCategory> = _selectedCategory.asStateFlow()

    private val _feedState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val feedState: StateFlow<FeedUiState> = _feedState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val downloadedIds: StateFlow<Set<String>> = downloadRepository.downloadedVideoIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        loadTrending(YouTubeCategory.ALL)
        viewModelScope.launch {
            preferencesRepository.preferencesFlow
                .map { it.regionCode to it.youtubeApiKeyOverride }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    loadTrending(_selectedCategory.value)
                }
        }
    }

    fun selectCategory(category: YouTubeCategory) {
        if (_selectedCategory.value == category && _feedState.value is FeedUiState.Success) return
        _selectedCategory.value = category
        loadTrending(category)
    }

    fun refresh() {
        loadTrending(_selectedCategory.value, isPullRefresh = true)
    }

    fun loadTrending(category: YouTubeCategory, isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) {
                _isRefreshing.value = true
            } else {
                _feedState.value = FeedUiState.Loading
            }
            val region = preferencesRepository.preferencesFlow.first().regionCode
            when (val result = youTubeRepository.getTrendingVideos(category, region)) {
                is ApiResult.Success -> _feedState.value = FeedUiState.Success(result.data)
                is ApiResult.Error -> _feedState.value = FeedUiState.Error(result.message)
                is ApiResult.MissingApiKey -> _feedState.value = FeedUiState.MissingApiKey(result.keyName)
            }
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val youTubeRepository: YouTubeRepository,
        private val downloadRepository: DownloadRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(youTubeRepository, downloadRepository, preferencesRepository) as T
        }
    }
}
