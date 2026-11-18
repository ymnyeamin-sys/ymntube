package com.example

import com.example.data.remote.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun extractYouTubeVideoId_handlesStandardAndShortUrls() {
        assertEquals("dQw4w9WgXcQ", FormatUtils.extractYouTubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", FormatUtils.extractYouTubeVideoId("https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("dQw4w9WgXcQ", FormatUtils.extractYouTubeVideoId("https://youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", FormatUtils.extractYouTubeVideoId("dQw4w9WgXcQ"))
        assertNull(FormatUtils.extractYouTubeVideoId("not a video url"))
    }

    @Test
    fun formatIsoDuration_formatsMinutesAndHours() {
        assertEquals("12:05", FormatUtils.formatIsoDuration("PT12M5S"))
        assertEquals("1:03:09", FormatUtils.formatIsoDuration("PT1H3M9S"))
        assertEquals("0:45", FormatUtils.formatIsoDuration("PT45S"))
    }

    @Test
    fun formatCountAndBytes_formatsReadably() {
        assertEquals("1.4M", FormatUtils.formatCount(1_420_000L))
        assertEquals("12.5K", FormatUtils.formatCount(12_500L))
        assertTrue(FormatUtils.formatBytes(26_000_000L).contains("MB"))
    }
}
