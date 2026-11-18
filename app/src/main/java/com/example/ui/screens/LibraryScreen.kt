package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.VideoItem
import com.example.data.remote.ApiClient
import com.example.ui.components.CompactVideoRow
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.CyanSoft
import com.example.ui.theme.EmeraldGallery
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    downloadedIds: Set<String>,
    onVideoClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val history by viewModel.watchHistory.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingConfig.collectAsStateWithLifecycle()
    val configMessage by viewModel.configMessage.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var downloadUrlInput by rememberSaveable { mutableStateOf(prefs.downloadApiBaseUrl) }
    var remoteConfigInput by rememberSaveable { mutableStateOf(prefs.remoteConfigUrl) }

    LaunchedEffect(prefs.downloadApiBaseUrl) {
        downloadUrlInput = prefs.downloadApiBaseUrl
    }
    LaunchedEffect(prefs.remoteConfigUrl) {
        if (prefs.remoteConfigUrl.isNotBlank() && remoteConfigInput.isBlank()) {
            remoteConfigInput = prefs.remoteConfigUrl
        }
    }

    val tabs = listOf("Saved (${watchlist.size})", "History (${history.size})", "Settings")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceLevel1)
                .border(1.dp, OutlineSubtle, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val active = selectedTab == index
                Surface(
                    color = if (active) CrimsonAccent else SurfaceLevel1,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTab = index }
                        .testTag("library_tab_$index")
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> {
                if (watchlist.isEmpty()) {
                    LibraryEmptyState(
                        icon = Icons.Rounded.Bookmark,
                        title = "No Saved Videos Yet",
                        subtitle = "Tap Save on any video watch page to keep it in your personal watchlist."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(watchlist, key = { it.videoId }) { entry ->
                            val video = entry.toVideoItem()
                            CompactVideoRow(
                                video = video,
                                isDownloaded = video.id in downloadedIds,
                                onClick = { onVideoClick(video) },
                                onDownloadClick = { onDownloadClick(video) }
                            )
                        }
                    }
                }
            }

            1 -> {
                if (history.isEmpty()) {
                    LibraryEmptyState(
                        icon = Icons.Rounded.History,
                        title = "Watch History is Empty",
                        subtitle = "Videos you open in VeloTube will appear here for quick replay."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { viewModel.clearHistory() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteSweep,
                                        contentDescription = null,
                                        tint = CrimsonAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Clear History", color = CrimsonAccent)
                                }
                            }
                        }
                        items(history, key = { it.videoId }) { entry ->
                            val video = entry.toVideoItem()
                            CompactVideoRow(
                                video = video,
                                isDownloaded = video.id in downloadedIds,
                                onClick = { onVideoClick(video) },
                                onDownloadClick = { onDownloadClick(video) }
                            )
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!configMessage.isNullOrBlank()) {
                        item {
                            Surface(
                                color = CyanSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.clearConfigMessage() }
                            ) {
                                Text(
                                    text = configMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanElectric,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            color = SurfaceLevel1,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, OutlineSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PermMedia,
                                    contentDescription = null,
                                    tint = EmeraldGallery
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sync Downloads to Gallery",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Automatically export MP4 videos to Movies/VeloTube so they appear in your Android Gallery.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = prefs.autoExportToGallery,
                                    onCheckedChange = { viewModel.setAutoExportToGallery(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextPrimary,
                                        checkedTrackColor = EmeraldGallery
                                    )
                                )
                            }
                        }
                    }

                    // Download API Base URL Editor
                    item {
                        Surface(
                            color = SurfaceLevel1,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, OutlineSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = null,
                                        tint = CyanElectric,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Download API Base URL",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                }
                                OutlinedTextField(
                                    value = downloadUrlInput,
                                    onValueChange = { downloadUrlInput = it },
                                    singleLine = true,
                                    placeholder = { Text("https://ytdlp-production-4d018ba3.up.railway.app", color = TextMuted) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceLevel2,
                                        unfocusedContainerColor = SurfaceLevel2,
                                        focusedBorderColor = CrimsonAccent,
                                        unfocusedBorderColor = OutlineSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.saveDownloadApiUrl(downloadUrlInput) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CrimsonAccent,
                                            contentColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Save URL")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.resetDownloadApiUrl() },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Reset", color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Remote Web-Admin Config Sync
                    item {
                        Surface(
                            color = SurfaceLevel1,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, OutlineSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = null,
                                        tint = EmeraldGallery,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Remote Config Sync (Web Admin)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "Paste your hosted velotube-config.json URL (from VeloTube Web Admin or GitHub Gist Raw) to remotely update the YouTube API key & Download API URL.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                OutlinedTextField(
                                    value = remoteConfigInput,
                                    onValueChange = { remoteConfigInput = it },
                                    singleLine = true,
                                    placeholder = { Text("https://gist.githubusercontent.com/.../velotube-config.json", color = TextMuted) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceLevel2,
                                        unfocusedContainerColor = SurfaceLevel2,
                                        focusedBorderColor = EmeraldGallery,
                                        unfocusedBorderColor = OutlineSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { viewModel.syncRemoteConfig(remoteConfigInput) },
                                    enabled = remoteConfigInput.isNotBlank() && !isSyncing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGallery,
                                        contentColor = ObsidianBg
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            color = ObsidianBg,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Syncing…")
                                    } else {
                                        Text("Sync Remote Config")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val regions = listOf("US", "BD", "IN", "GB", "CA", "JP")
                        Surface(
                            color = SurfaceLevel1,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, OutlineSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Trending Feed Region",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(regions) { code ->
                                        val selected = prefs.regionCode == code
                                        Surface(
                                            color = if (selected) CrimsonAccent else SurfaceLevel2,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { viewModel.setRegionCode(code) }
                                        ) {
                                            Text(
                                                text = code,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = TextPrimary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val ytConfigured = ApiClient.isYouTubeKeyConfigured(prefs.youtubeApiKeyOverride)
                        ApiStatusCard(
                            title = "YouTube Data API v3",
                            badgeText = if (ytConfigured) {
                                if (prefs.youtubeApiKeyOverride.isNotBlank()) "Remote" else "Ready"
                            } else "YOUTUBE_API_KEY",
                            connected = ytConfigured,
                            subtitle = "Powers trending feeds, search, channel info, and comments."
                        )
                    }

                    item {
                        ApiStatusCard(
                            title = "Custom Download API",
                            badgeText = "Active",
                            connected = true,
                            subtitle = prefs.downloadApiBaseUrl
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiStatusCard(
    title: String,
    badgeText: String,
    connected: Boolean,
    subtitle: String
) {
    Surface(
        color = SurfaceLevel1,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineSubtle, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (connected) Icons.Rounded.CheckCircle else Icons.Rounded.Key,
                contentDescription = null,
                tint = if (connected) EmeraldGallery else AmberWarn
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Surface(
                color = if (connected) EmeraldSoft else AmberSoft,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (connected) EmeraldGallery else AmberWarn,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun WatchHistoryEntity.toVideoItem(): VideoItem {
    return VideoItem(
        id = videoId,
        title = title,
        description = "",
        channelId = channelId,
        channelTitle = channelTitle,
        thumbnailUrl = thumbnailUrl,
        publishedAt = publishedAt,
        durationText = durationText,
        viewCount = viewCount
    )
}
