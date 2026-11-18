package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UrlRequestBody(
    val url: String
)

@JsonClass(generateAdapter = true)
data class DownloadWithQualityBody(
    val url: String,
    val quality: String
)

@JsonClass(generateAdapter = true)
data class DownloadQualityResponse(
    @Json(name = "download_url") val downloadUrl: String? = null,
    val selected: SelectedQualityInfo? = null,
    val error: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SelectedQualityInfo(
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class RemoteAppConfig(
    @Json(name = "youtube_api_key") val youtubeApiKey: String? = null,
    @Json(name = "download_api_url") val downloadApiUrl: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)
