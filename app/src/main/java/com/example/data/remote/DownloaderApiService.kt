package com.example.data.remote

import com.example.data.model.DownloadWithQualityBody
import com.example.data.model.RemoteAppConfig
import com.example.data.model.UrlRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface DownloaderApiService {
    @POST
    suspend fun getVideoInfo(
        @Url fullUrl: String,
        @Body body: UrlRequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun getAvailableResolutions(
        @Url fullUrl: String,
        @Body body: UrlRequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun requestDownload(
        @Url fullUrl: String,
        @Body body: UrlRequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun requestDownloadWithBodyQuality(
        @Url fullUrl: String,
        @Body body: DownloadWithQualityBody
    ): Response<ResponseBody>

    @GET
    suspend fun requestDownloadGet(
        @Url fullUrl: String
    ): Response<ResponseBody>

    @GET
    suspend fun fetchRemoteConfig(
        @Url configUrl: String
    ): Response<RemoteAppConfig>
}
