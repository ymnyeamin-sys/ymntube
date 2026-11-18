package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.ApiResult
import com.example.data.model.VideoItem
import com.example.data.repository.DownloadRepository
import com.example.data.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val videos: List<VideoItem>) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
    data class MissingApiKey(val keyName: String) : SearchUiState
}

class SearchViewModel(
    private val youTubeRepository: YouTubeRepository,
    downloadRepository: DownloadRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = youTubeRepository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedIds: StateFlow<Set<String>> = downloadRepository.downloadedVideoIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _uiState.value = SearchUiState.Idle
        }
    }

    fun submitSearch(overrideQuery: String? = null) {
        val q = (overrideQuery ?: _query.value).trim()
        if (q.isEmpty()) return
        _query.value = q

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            when (val result = youTubeRepository.searchVideos(q)) {
                is ApiResult.Success -> {
                    _uiState.value = if (result.data.isEmpty()) {
                        SearchUiState.Empty(q)
                    } else {
                        SearchUiState.Success(result.data)
                    }
                }
                is ApiResult.Error -> _uiState.value = SearchUiState.Error(result.message)
                is ApiResult.MissingApiKey -> _uiState.value = SearchUiState.MissingApiKey(result.keyName)
            }
        }
    }

    fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            youTubeRepository.deleteSearchQuery(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            youTubeRepository.clearSearchHistory()
        }
    }

    class Factory(
        private val youTubeRepository: YouTubeRepository,
        private val downloadRepository: DownloadRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(youTubeRepository, downloadRepository) as T
        }
    }
}
