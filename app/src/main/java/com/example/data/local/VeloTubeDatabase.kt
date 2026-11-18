package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DownloadedVideoEntity::class,
        WatchHistoryEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VeloTubeDatabase : RoomDatabase() {
    abstract fun veloTubeDao(): VeloTubeDao

    companion object {
        @Volatile
        private var instance: VeloTubeDatabase? = null

        fun getInstance(context: Context): VeloTubeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VeloTubeDatabase::class.java,
                    "velotube_db"
                ).build().also { instance = it }
            }
        }
    }
}
