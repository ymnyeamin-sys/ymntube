package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.VideoItem
import com.example.data.repository.DownloaderResponseParser
import com.example.data.repository.HomeSuggestionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("YmnTube", appName)
    }

    @Test
    fun `parseAvailableResolutions handles raw digits, p-suffixed strings, and object arrays`() {
        val numericPayload = """{"resolutions": [338, 1012, 1080], "all": ["144p"]}"""
        val (numOpts, numErr) = DownloaderResponseParser.parseAvailableResolutions(numericPayload)
        assertNull(numErr)
        assertEquals(listOf("338", "1012", "1080"), numOpts.map { it.requestQuality })
        assertEquals(listOf("338p", "1012p", "1080p"), numOpts.map { it.displayLabel })

        val stringPayload = """{"all": ["144p", "360", "720p"]}"""
        val (strOpts, strErr) = DownloaderResponseParser.parseAvailableResolutions(stringPayload)
        assertNull(strErr)
        assertEquals(listOf("144p", "360", "720p"), strOpts.map { it.requestQuality })
        assertEquals(listOf("144p", "360p", "720p"), strOpts.map { it.displayLabel })

        val objectPayload = """
            {
              "progressive": [
                {"quality": "360p (MP4)", "resolution": "640x360", "height": 360, "ext": "mp4", "filesize": 1234567},
                {"resolution": "1280x720", "ext": "mp4"}
              ]
            }
        """.trimIndent()
        val (objOpts, objErr) = DownloaderResponseParser.parseAvailableResolutions(objectPayload)
        assertNull(objErr)
        assertEquals(listOf("360", "720"), objOpts.map { it.requestQuality })
        assertEquals(listOf("360p", "720p"), objOpts.map { it.displayLabel })
        assertEquals(1234567L, objOpts[0].sizeBytes)
    }

    @Test
    fun `buildQualityCandidates emits both raw digits and p-suffixed fallback`() {
        assertEquals(listOf("1080", "1080p"), DownloaderResponseParser.buildQualityCandidates("1080"))
        assertEquals(listOf("1080p", "1080"), DownloaderResponseParser.buildQualityCandidates("1080p"))
        assertEquals(
            listOf("338", "338p", "18"),
            DownloaderResponseParser.buildQualityCandidates("338", "18")
        )
    }

    @Test
    fun `parseVideoInfo handles flexible fields`() {
        val infoPayload = """
            {
              "title": "Sample Video",
              "uploader": "Creator",
              "duration": "125.4",
              "view_count": 54000,
              "thumbnail": [{"url": "https://i.ytimg.com/vi/abc/hqdefault.jpg"}]
            }
        """.trimIndent()
        val parsed = DownloaderResponseParser.parseVideoInfo(infoPayload)
        assertEquals("Sample Video", parsed?.title)
        assertEquals("Creator", parsed?.author)
        assertEquals(125L, parsed?.durationSeconds)
        assertEquals(54000L, parsed?.viewCount)
        assertEquals("https://i.ytimg.com/vi/abc/hqdefault.jpg", parsed?.thumbnailUrl)
    }

    @Test
    fun `parseDownloadResponse preserves downloadUrl when informational message exists`() {
        val payload = """
            {
              "download_url": "https://cdn.example.com/video.mp4",
              "selected": {"height": 360},
              "message": "Download ready"
            }
        """.trimIndent()
        val parsed = DownloaderResponseParser.parseDownloadResponse(payload)
        assertEquals("https://cdn.example.com/video.mp4", parsed?.downloadUrl)
        assertEquals(360, parsed?.selected?.height)
        assertEquals("Download ready", parsed?.message)
        assertNull(parsed?.error)
    }

    @Test
    fun `parseAppUpdateInfo parses enabled version code, string booleans, and gist API`() {
        val enabledJson = """
            {
              "update_enabled": true,
              "version": "1.1.0",
              "version_code": 2,
              "update_url": "https://example.com/ymntube-v1.1.0.apk",
              "title": "New YmnTube Update",
              "changelog": "Background playback & faster downloads"
            }
        """.trimIndent()
        val enabled = DownloaderResponseParser.parseAppUpdateInfo(enabledJson)
        assertTrue(enabled?.updateEnabled == true)
        assertEquals("1.1.0", enabled?.version)
        assertEquals(2, enabled?.versionCode)
        assertEquals("https://example.com/ymntube-v1.1.0.apk", enabled?.updateUrl)
        assertEquals("New YmnTube Update", enabled?.title)

        val flexibleJson = """{"update_on": "on", "version": "v1.0.1", "downloadUrl": "https://example.com/release"}"""
        val flex = DownloaderResponseParser.parseAppUpdateInfo(flexibleJson)
        assertTrue(flex?.updateEnabled == true)
        assertTrue(DownloaderResponseParser.isVersionNewer(flex!!.version, flex.versionCode, "1.0", 1))
        assertFalse(DownloaderResponseParser.isVersionNewer("v1.0", 1, "1.0", 1))

        val disabledJson = """{"update_enabled": false, "version": "1.1.0", "update_url": "https://example.com/app"}"""
        val disabled = DownloaderResponseParser.parseAppUpdateInfo(disabledJson)
        assertFalse(disabled?.updateEnabled ?: true)

        val gistPayload = """
            {
              "files": {
                "update.json": {
                  "content": "{\"update_enabled\":true,\"version\":\"1.2.0\",\"version_code\":3,\"update_url\":\"https://example.com/app.apk\"}"
                }
              }
            }
        """.trimIndent()
        val extracted = DownloaderResponseParser.extractGistFileContent(gistPayload, "update.json")
        val fromGist = DownloaderResponseParser.parseAppUpdateInfo(extracted.orEmpty())
        assertEquals("1.2.0", fromGist?.version)
        assertEquals(3, fromGist?.versionCode)
    }

    @Test
    fun `HomeSuggestionEngine collapses near-duplicate titles and diversifies overall taste`() {
        val watches = listOf(
            WatchHistoryEntity("v1", "Eto Prem Eto Maya (Full Song) | Mon Dilona", "c1", "Channel A", "", "", 0L, "", 100L),
            WatchHistoryEntity("v2", "Samsung Galaxy S26 Unboxing & Full Review Bangla", "c2", "TechBD", "", "", 0L, "", 90L)
        )
        val searches = listOf(SearchHistoryEntity("eto prem eto maya song", 100L))

        val queries = HomeSuggestionEngine.buildDiverseTasteQueries(
            searches = searches,
            watches = watches,
            saved = emptyList(),
            regionCode = "BD"
        )
        assertTrue(queries.size >= 2)
        assertTrue(queries.none { it.equals("Eto Prem Eto Maya Full Song", ignoreCase = true) })

        val dupBucket = listOf(
            VideoItem("a1", "Eto Prem Eto Maya (Full Song) | Official Video", "", "c1", "Ch1", null, "", ""),
            VideoItem("a2", "Eto Prem Eto Maya Full Song Lyrics HD", "", "c3", "Ch3", null, "", ""),
            VideoItem("a3", "Eto Prem Eto Maya (Full Song) Cover Version", "", "c4", "Ch4", null, "", "")
        )
        val freshBucket = listOf(
            VideoItem("b1", "Tumi Robe Nirobe New Bangla Song", "", "c5", "Ch5", null, "", ""),
            VideoItem("b2", "Best Budget Phones in Bangladesh 2026", "", "c2", "TechBD", null, "", ""),
            VideoItem("b3", "Dhaka Street Food Tour", "", "c6", "Foodie", null, "", "")
        )
        val ranked = HomeSuggestionEngine.rankAndDiversify(
            buckets = listOf(dupBucket, freshBucket),
            watchedIds = emptySet(),
            preferredChannels = setOf("techbd"),
            limit = 20
        )
        // Strict title dedupe collapses a1, a2, a3 into 1 distinct song title (or pads only if needed)
        val etoPremCount = ranked.take(4).count { it.title.contains("Eto Prem Eto Maya") }
        assertEquals(1, etoPremCount)
        assertTrue(ranked.any { it.id == "b1" })
        assertTrue(ranked.any { it.id == "b2" })
    }
}
