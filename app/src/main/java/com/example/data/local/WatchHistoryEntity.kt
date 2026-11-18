package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationText: String,
    val viewCount: Long,
    val publishedAt: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val isSavedToWatchlist: Boolean = false
)
