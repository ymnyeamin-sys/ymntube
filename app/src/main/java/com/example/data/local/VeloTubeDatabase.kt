package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DownloadedVideoEntity::class,
        WatchHistoryEntity::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VeloTubeDatabase : RoomDatabase() {
    abstract fun veloTubeDao(): VeloTubeDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloaded_videos_new (
                        downloadId TEXT NOT NULL PRIMARY KEY,
                        videoId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        channelTitle TEXT NOT NULL,
                        thumbnailUrl TEXT NOT NULL,
                        durationText TEXT NOT NULL,
                        qualityLabel TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        fileSizeBytes INTEGER NOT NULL,
                        localFilePath TEXT NOT NULL,
                        mediaStoreUri TEXT,
                        isSavedToGallery INTEGER NOT NULL,
                        downloadedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO downloaded_videos_new (
                        downloadId, videoId, title, channelTitle, thumbnailUrl,
                        durationText, qualityLabel, mimeType, fileSizeBytes,
                        localFilePath, mediaStoreUri, isSavedToGallery, downloadedAt
                    )
                    SELECT
                        videoId || '_' || qualityLabel, videoId, title, channelTitle, thumbnailUrl,
                        durationText, qualityLabel, mimeType, fileSizeBytes,
                        localFilePath, mediaStoreUri, isSavedToGallery, downloadedAt
                    FROM downloaded_videos
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE downloaded_videos")
                db.execSQL("ALTER TABLE downloaded_videos_new RENAME TO downloaded_videos")
            }
        }

        @Volatile
        private var instance: VeloTubeDatabase? = null

        fun getInstance(context: Context): VeloTubeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VeloTubeDatabase::class.java,
                    "velotube_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
