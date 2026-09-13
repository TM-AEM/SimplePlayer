package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VideoItem
import com.example.data.VideoRepository
import com.example.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VideoSortOrder {
    DATE_DESC,
    NAME_ASC,
    SIZE_DESC
}

data class VideoListUiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val allVideos: List<VideoItem> = emptyList(),
    val displayedVideos: List<VideoItem> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: VideoSortOrder = VideoSortOrder.DATE_DESC
)

class VideoListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val _uiState = MutableStateFlow(
        VideoListUiState(
            hasPermission = PermissionUtils.hasVideoPermission(application)
        )
    )
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val hasPerm = PermissionUtils.hasVideoPermission(getApplication())
        _uiState.update { it.copy(hasPermission = hasPerm) }
        if (hasPerm) {
            loadVideos()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            loadVideos()
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val videos = repository.queryDeviceVideos()
            _uiState.update { state ->
                val filtered = applyFilterAndSort(videos, state.searchQuery, state.sortOrder)
                state.copy(
                    isLoading = false,
                    allVideos = videos,
                    displayedVideos = filtered
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(state.allVideos, query, state.sortOrder)
            state.copy(
                searchQuery = query,
                displayedVideos = filtered
            )
        }
    }

    fun setSortOrder(order: VideoSortOrder) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(state.allVideos, state.searchQuery, order)
            state.copy(
                sortOrder = order,
                displayedVideos = filtered
            )
        }
    }

    private fun applyFilterAndSort(
        videos: List<VideoItem>,
        query: String,
        sortOrder: VideoSortOrder
    ): List<VideoItem> {
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            videos
        } else {
            videos.filter { it.title.contains(trimmed, ignoreCase = true) }
        }

        return when (sortOrder) {
            VideoSortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateAdded }
            VideoSortOrder.NAME_ASC -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            VideoSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
        }
    }
}
