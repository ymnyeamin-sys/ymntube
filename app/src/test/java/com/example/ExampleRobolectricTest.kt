package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.DownloaderResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals("VeloTube", appName)
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
}
