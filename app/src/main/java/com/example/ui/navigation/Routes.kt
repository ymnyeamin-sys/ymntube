package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object DownloadsRoute

@Serializable
data object LibraryRoute

@Serializable
data class WatchRoute(
    val videoId: String,
    val initialTitle: String = "",
    val initialChannel: String = "",
    val initialThumb: String = ""
)

@Serializable
data class OfflinePlayerRoute(
    val downloadId: String
)
