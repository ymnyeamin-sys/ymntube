package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VeloTubeDao {
    @Query("SELECT * FROM downloaded_videos ORDER BY downloadedAt DESC")
    fun observeDownloadedVideos(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownloadedVideoById(videoId: String): DownloadedVideoEntity?

    @Query("SELECT videoId FROM downloaded_videos")
    fun observeDownloadedIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadedVideo(video: DownloadedVideoEntity)

    @Query("DELETE FROM downloaded_videos WHERE videoId = :videoId")
    suspend fun deleteDownloadedVideo(videoId: String)

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT 50")
    fun observeWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE isSavedToWatchlist = 1 ORDER BY watchedAt DESC")
    fun observeWatchlist(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getHistoryItem(videoId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(entry: WatchHistoryEntity)

    @Query("UPDATE watch_history SET isSavedToWatchlist = :saved WHERE videoId = :videoId")
    suspend fun updateWatchlistFlag(videoId: String, saved: Boolean)

    @Query("DELETE FROM watch_history WHERE isSavedToWatchlist = 0")
    suspend fun clearNonSavedHistory()

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 12")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchQuery(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
