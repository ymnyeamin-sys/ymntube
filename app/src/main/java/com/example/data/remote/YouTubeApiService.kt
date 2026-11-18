package com.example.data.remote

import com.example.data.model.YouTubeChannelListResponse
import com.example.data.model.YouTubeCommentThreadsResponse
import com.example.data.model.YouTubeSearchListResponse
import com.example.data.model.YouTubeVideoListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("videos")
    suspend fun getPopularVideos(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("chart") chart: String = "mostPopular",
        @Query("regionCode") regionCode: String = "BD",
        @Query("videoCategoryId") videoCategoryId: String? = null,
        @Query("maxResults") maxResults: Int = 25,
        @Query("pageToken") pageToken: String? = null,
        @Query("key") apiKey: String
    ): YouTubeVideoListResponse

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("pageToken") pageToken: String? = null,
        @Query("safeSearch") safeSearch: String = "moderate",
        @Query("key") apiKey: String
    ): YouTubeSearchListResponse

    @GET("videos")
    suspend fun getVideosByIds(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("id") ids: String,
        @Query("key") apiKey: String
    ): YouTubeVideoListResponse

    @GET("channels")
    suspend fun getChannelsByIds(
        @Query("part") part: String = "snippet,statistics",
        @Query("id") ids: String,
        @Query("key") apiKey: String
    ): YouTubeChannelListResponse

    @GET("commentThreads")
    suspend fun getVideoComments(
        @Query("part") part: String = "snippet",
        @Query("videoId") videoId: String,
        @Query("order") order: String = "relevance",
        @Query("maxResults") maxResults: Int = 20,
        @Query("textFormat") textFormat: String = "plainText",
        @Query("key") apiKey: String
    ): YouTubeCommentThreadsResponse
}
