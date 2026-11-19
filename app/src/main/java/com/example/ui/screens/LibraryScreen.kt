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
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.ActiveDownload
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    downloadedIds: Set<String>,
    activeDownloads: Map<String, ActiveDownload>,
    isCheckingUpdate: Boolean = false,
    onVideoClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onCheckForUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val history by viewModel.watchHistory.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingConfig.collectAsStateWithLifecycle()
    val configMessage by viewModel.configMessage.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

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
                                activeDownload = activeDownloads.values.firstOrNull { it.videoId == video.id },
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
                        subtitle = "Videos you open in YmnTube will appear here for quick replay."
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
                                activeDownload = activeDownloads.values.firstOrNull { it.videoId == video.id },
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

                    // Background / Lock-Screen Playback Switch
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
                                    imageVector = Icons.Rounded.Headphones,
                                    contentDescription = null,
                                    tint = CyanElectric
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Background & Lock-Screen Play",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Keep music and videos playing even when the screen is locked.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = prefs.backgroundPlayEnabled,
                                    onCheckedChange = { viewModel.setBackgroundPlayEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextPrimary,
                                        checkedTrackColor = CyanElectric
                                    )
                                )
                            }
                        }
                    }

                    // Sync Downloads to Gallery Switch
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
                                        text = "Automatically export MP4 videos to Movies/YmnTube in your Android Gallery.",
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

                    // Home Feed Region Selector (BD default)
                    item {
                        val regions = listOf("BD", "IN", "US", "GB", "CA", "JP")
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
                                    text = "Home Feed Region",
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

                    // Automatic Remote Config & App Updates (Read-only status + action buttons)
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
                                        text = "YmnTube Cloud Sync & Updates",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "Configuration syncs automatically on startup. Installed version: v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.syncFixedRemoteConfig() },
                                        enabled = !isSyncing,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldGallery,
                                            contentColor = ObsidianBg
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                color = ObsidianBg,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Syncing…")
                                        } else {
                                            Text("Sync Config")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = onCheckForUpdate,
                                        enabled = !isCheckingUpdate,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isCheckingUpdate) {
                                            CircularProgressIndicator(
                                                color = CyanElectric,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Checking…", color = CyanElectric, maxLines = 1)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.SystemUpdate,
                                                contentDescription = null,
                                                tint = CyanElectric,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Check Update", color = CyanElectric, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val ytConfigured = ApiClient.isYouTubeKeyConfigured(prefs.youtubeApiKeyOverride)
                        ApiStatusCard(
                            title = "YouTube Service",
                            badgeText = if (ytConfigured) "Connected" else "Syncing",
                            connected = ytConfigured,
                            subtitle = "Automatically provisioned via remote configuration."
                        )
                    }

                    item {
                        ApiStatusCard(
                            title = "Download Service",
                            badgeText = "Active",
                            connected = true,
                            subtitle = "Automatically provisioned via remote configuration."
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
