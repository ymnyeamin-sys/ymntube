package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.DEFAULT_DOWNLOAD_API_BASE_URL
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3/"
    private const val YOUTUBE_PLACEHOLDER = "YOUR_YOUTUBE_DATA_API_V3_KEY"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(95, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(logging)
            .build()
    }

    val youTubeApiService: YouTubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeApiService::class.java)
    }

    val downloaderApiService: DownloaderApiService by lazy {
        Retrofit.Builder()
            .baseUrl("$DEFAULT_DOWNLOAD_API_BASE_URL/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DownloaderApiService::class.java)
    }

    fun getBuildConfigYouTubeKey(): String? {
        val key = BuildConfig.YOUTUBE_API_KEY.trim()
        return if (key.isEmpty() || key == YOUTUBE_PLACEHOLDER) null else key
    }

    fun getEffectiveYouTubeApiKey(overrideKey: String?): String? {
        val cleanOverride = overrideKey?.trim().orEmpty()
        if (cleanOverride.isNotEmpty() && cleanOverride != YOUTUBE_PLACEHOLDER) {
            return cleanOverride
        }
        return getBuildConfigYouTubeKey()
    }

    fun isYouTubeKeyConfigured(overrideKey: String? = null): Boolean =
        getEffectiveYouTubeApiKey(overrideKey) != null
}
