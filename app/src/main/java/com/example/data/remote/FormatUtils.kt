package com.example.data.remote

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern
import kotlin.math.abs

object FormatUtils {

    fun formatIsoDuration(isoDuration: String?): String {
        if (isoDuration.isNullOrBlank()) return ""
        val pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val matcher = pattern.matcher(isoDuration)
        if (!matcher.matches()) return ""
        val hours = matcher.group(1)?.toIntOrNull() ?: 0
        val minutes = matcher.group(2)?.toIntOrNull() ?: 0
        val seconds = matcher.group(3)?.toIntOrNull() ?: 0
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatSecondsDuration(secondsStr: String?): String {
        val total = secondsStr?.toLongOrNull() ?: return ""
        if (total <= 0) return ""
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", count / 1_000_000_000.0)
            count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    fun formatViews(count: Long): String {
        return if (count <= 0L) "No views" else "${formatCount(count)} views"
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Unknown size"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }

    fun formatRelativeTime(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = sdf.parse(isoDate) ?: return ""
            val diffMs = abs(System.currentTimeMillis() - parsed.time)
            val minutes = diffMs / (60 * 1000)
            val hours = minutes / 60
            val days = hours / 24
            val months = days / 30
            val years = days / 365
            when {
                years > 0 -> "${years}y ago"
                months > 0 -> "${months}mo ago"
                days > 0 -> "${days}d ago"
                hours > 0 -> "${hours}h ago"
                minutes > 0 -> "${minutes}m ago"
                else -> "Just now"
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun extractYouTubeVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }
        val patterns = listOf(
            "(?:v=|/v/|embed/|shorts/|live/)([a-zA-Z0-9_-]{11})",
            "youtu\\.be/([a-zA-Z0-9_-]{11})"
        )
        for (p in patterns) {
            val matcher = Pattern.compile(p).matcher(trimmed)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    fun isDirectMediaUrl(input: String): Boolean {
        val lower = input.trim().lowercase(Locale.US)
        return (lower.startsWith("http://") || lower.startsWith("https://")) &&
            (lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".m4a") || lower.contains(".mkv"))
    }
}
