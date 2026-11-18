package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_videos")
data class DownloadedVideoEntity(
    @PrimaryKey val downloadId: String,
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationText: String,
    val qualityLabel: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val localFilePath: String,
    val mediaStoreUri: String? = null,
    val isSavedToGallery: Boolean = false,
    val downloadedAt: Long = System.currentTimeMillis()
)
