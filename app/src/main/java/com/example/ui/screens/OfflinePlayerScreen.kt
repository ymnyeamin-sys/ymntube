package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DownloadedVideoEntity
import com.example.data.remote.FormatUtils
import com.example.ui.components.ExoMediaSpec
import com.example.ui.components.ExoVideoPlayer
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CrimsonSoft
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

@Composable
fun OfflinePlayerScreen(
    entity: DownloadedVideoEntity?,
    allDownloads: List<DownloadedVideoEntity>,
    backgroundPlayEnabled: Boolean,
    onSelectVideo: (DownloadedVideoEntity) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onFullscreenChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenInGallery: (DownloadedVideoEntity) -> Unit,
    onExportToGallery: (DownloadedVideoEntity) -> Unit,
    onShare: (DownloadedVideoEntity) -> Unit,
    onDelete: (DownloadedVideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    LaunchedEffect(isFullscreen) {
        onFullscreenChanged(isFullscreen)
    }

    DisposableEffect(isFullscreen) {
        val activity = context as? Activity
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onFullscreenChanged(false)
        }
    }

    if (entity == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Downloaded video not found.",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
        }
        return
    }

    val playlist = remember(allDownloads) {
        allDownloads.map { item ->
            ExoMediaSpec(
                id = item.downloadId,
                uriOrPath = item.localFilePath,
                title = item.title,
                artist = "${item.channelTitle} • ${item.qualityLabel}",
                artworkUrl = item.thumbnailUrl
            )
        }
    }

    val sameVideoVariants = remember(allDownloads, entity.videoId) {
        allDownloads.filter { it.videoId == entity.videoId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier.aspectRatio(16f / 9f)
                )
                .background(Color.Black)
        ) {
            ExoVideoPlayer(
                mediaUriOrPath = entity.localFilePath,
                title = entity.title,
                artist = entity.channelTitle,
                artworkUrl = entity.thumbnailUrl,
                playlist = playlist,
                currentItemId = entity.downloadId,
                backgroundPlayEnabled = backgroundPlayEnabled,
                isFullscreen = isFullscreen,
                onCurrentItemChanged = { newDownloadId ->
                    allDownloads.firstOrNull { it.downloadId == newDownloadId }?.let(onSelectVideo)
                },
                onFullscreenToggle = { isFullscreen = it },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = { if (isFullscreen) isFullscreen = false else onBack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(Color(0x8809090D))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        }

        if (isFullscreen) return@Column

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = entity.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = entity.channelTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = CyanSoft, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "${entity.qualityLabel} • ${FormatUtils.formatBytes(entity.fileSizeBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanElectric,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    if (entity.isSavedToGallery) {
                        Surface(color = EmeraldSoft, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "Synced in Android Gallery",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldGallery,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (sameVideoVariants.size > 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Switch Downloaded Resolution",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sameVideoVariants, key = { it.downloadId }) { variant ->
                                val selected = variant.downloadId == entity.downloadId
                                Surface(
                                    color = if (selected) CrimsonSoft else SurfaceLevel1,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            if (selected) CrimsonAccent else OutlineSubtle,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectVideo(variant) }
                                ) {
                                    Text(
                                        text = "${variant.qualityLabel} (${FormatUtils.formatBytes(variant.fileSizeBytes)})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) CrimsonAccent else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lock-screen / background play card
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
                            .padding(14.dp),
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
                                text = "Keep playing music & videos when the screen is locked or minimized.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = backgroundPlayEnabled,
                            onCheckedChange = onToggleBackgroundPlay,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = CyanElectric
                            )
                        )
                    }
                }
            }

            // Actions row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (entity.isSavedToGallery) onOpenInGallery(entity)
                            else onExportToGallery(entity)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGallery,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PermMedia,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (entity.isSavedToGallery) stringResource(R.string.open_in_gallery) else "Save to Gallery",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = { onShare(entity) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onDelete(entity)
                            onBack()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = CrimsonAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Vault Queue / Up Next
            val upNext = allDownloads.filter { it.downloadId != entity.downloadId }
            if (upNext.isNotEmpty()) {
                item {
                    Text(
                        text = "Up Next in YmnTube Vault (${upNext.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                items(upNext, key = { it.downloadId }) { item ->
                    Surface(
                        color = SurfaceLevel1,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, OutlineSubtle, RoundedCornerShape(14.dp))
                            .clickable { onSelectVideo(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(104.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceLevel2),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.thumbnailUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = TextPrimary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0x99000000))
                                        .padding(4.dp)
                                        .size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.qualityLabel} • ${item.channelTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
