package com.example.data

import android.content.Context
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VeloTubeDatabase
import com.example.data.remote.ApiClient
import com.example.data.repository.DownloadRepository
import com.example.data.repository.YouTubeRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = VeloTubeDatabase.getInstance(appContext)
    private val dao = database.veloTubeDao()

    val userPreferencesRepository = UserPreferencesRepository(appContext)

    val youTubeRepository = YouTubeRepository(
        api = ApiClient.youTubeApiService,
        dao = dao,
        preferencesRepository = userPreferencesRepository
    )

    val downloadRepository = DownloadRepository(
        context = appContext,
        downloaderApi = ApiClient.downloaderApiService,
        okHttpClient = ApiClient.okHttpClient,
        dao = dao,
        preferencesRepository = userPreferencesRepository
    )

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
        }
    }
}
