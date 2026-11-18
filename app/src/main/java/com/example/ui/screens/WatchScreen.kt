package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DownloadedVideoEntity
import com.example.data.model.VideoItem
import com.example.data.remote.FormatUtils
import com.example.ui.components.CompactVideoRow
import com.example.ui.components.ExoVideoPlayer
import com.example.ui.components.YouTubeWebPlayer
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
import com.example.ui.viewmodel.PlayerMode
import com.example.ui.viewmodel.WatchViewModel

@Composable
fun WatchScreen(
    viewModel: WatchViewModel,
    downloadedIds: Set<String>,
    onBack: () -> Unit,
    onRelatedClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onOpenInGallery: (DownloadedVideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isDescriptionExpanded by rememberSaveable { mutableStateOf(false) }
    var isCommentsExpanded by rememberSaveable { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .then(if (isFullscreen) Modifier else Modifier.statusBarsPadding())
    ) {
        // Video Player Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier.aspectRatio(16f / 9f)
                )
                .background(Color.Black)
        ) {
            when (state.playerMode) {
                PlayerMode.OFFLINE_VAULT -> {
                    val path = state.offlineDownload?.localFilePath
                    if (!path.isNullOrBlank()) {
                        ExoVideoPlayer(
                            mediaUriOrPath = path,
                            modifier = Modifier.fillMaxSize(),
                            onFullscreenToggle = { isFullscreen = it }
                        )
                    } else {
                        YouTubeWebPlayer(
                            videoId = state.video.id,
                            modifier = Modifier.fillMaxSize(),
                            onSwitchToNativePlayer = { viewModel.tryNativeExoStream() }
                        )
                    }
                }

                PlayerMode.EXO_NATIVE_STREAM -> {
                    val streamUrl = state.directStreamUrl
                    if (!streamUrl.isNullOrBlank()) {
                        ExoVideoPlayer(
                            mediaUriOrPath = streamUrl,
                            modifier = Modifier.fillMaxSize(),
                            onFullscreenToggle = { isFullscreen = it }
                        )
                    } else {
                        YouTubeWebPlayer(
                            videoId = state.video.id,
                            modifier = Modifier.fillMaxSize(),
                            onSwitchToNativePlayer = { viewModel.tryNativeExoStream() }
                        )
                    }
                }

                PlayerMode.YOUTUBE_EMBED -> {
                    YouTubeWebPlayer(
                        videoId = state.video.id,
                        modifier = Modifier.fillMaxSize(),
                        onSwitchToNativePlayer = { viewModel.tryNativeExoStream() }
                    )
                }
            }

            // Back Button overlay
            IconButton(
                onClick = {
                    if (isFullscreen) isFullscreen = false else onBack()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(Color(0x8809090D))
                    .testTag("watch_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            // Fullscreen toggle overlay for YouTubeWebPlayer
            if (state.playerMode == PlayerMode.YOUTUBE_EMBED) {
                IconButton(
                    onClick = { isFullscreen = !isFullscreen },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(Color(0x8809090D))
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        tint = TextPrimary
                    )
                }
            }
        }

        if (isFullscreen) return@Column

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Player Mode Switcher Pills
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ModeChip(
                            label = "YouTube Player",
                            icon = Icons.Rounded.PlayArrow,
                            selected = state.playerMode == PlayerMode.YOUTUBE_EMBED,
                            accent = CrimsonAccent,
                            onClick = { viewModel.switchToYouTubeEmbed() }
                        )
                    }
                    item {
                        ModeChip(
                            label = if (state.isResolvingStream) "Resolving HD…" else "Native ExoPlayer",
                            icon = Icons.Rounded.Hd,
                            selected = state.playerMode == PlayerMode.EXO_NATIVE_STREAM,
                            accent = CyanElectric,
                            onClick = { viewModel.tryNativeExoStream() }
                        )
                    }
                    if (state.offlineDownload != null) {
                        item {
                            ModeChip(
                                label = "Offline Vault",
                                icon = Icons.Rounded.OfflinePin,
                                selected = state.playerMode == PlayerMode.OFFLINE_VAULT,
                                accent = EmeraldGallery,
                                onClick = { viewModel.switchToOfflinePlayer() }
                            )
                        }
                    }
                }
            }

            if (!state.streamErrorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = CrimsonSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.streamErrorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Title & Stats
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = state.video.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val meta = buildList {
                            if (state.video.viewCount > 0) add(FormatUtils.formatViews(state.video.viewCount))
                            val rel = FormatUtils.formatRelativeTime(state.video.publishedAt)
                            if (rel.isNotBlank()) add(rel)
                        }.joinToString(" • ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = if (isDescriptionExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Toggle description",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = isDescriptionExpanded && state.video.description.isNotBlank()) {
                        Surface(
                            color = SurfaceLevel1,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .border(1.dp, OutlineSubtle, RoundedCornerShape(14.dp))
                        ) {
                            Text(
                                text = state.video.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Action Pill Row (Download, Gallery if downloaded, Save, Share)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        val isDownloaded = state.offlineDownload != null
                        ActionPill(
                            icon = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                            label = if (isDownloaded) "Downloaded" else stringResource(R.string.download_video),
                            containerColor = if (isDownloaded) EmeraldSoft else CrimsonAccent,
                            contentColor = if (isDownloaded) EmeraldGallery else TextPrimary,
                            onClick = { onDownloadClick(state.video) },
                            modifier = Modifier.testTag("watch_download_button")
                        )
                    }
                    state.offlineDownload?.let { dl ->
                        if (dl.isSavedToGallery) {
                            item {
                                ActionPill(
                                    icon = Icons.Rounded.PermMedia,
                                    label = stringResource(R.string.open_in_gallery),
                                    containerColor = CyanSoft,
                                    contentColor = CyanElectric,
                                    onClick = { onOpenInGallery(dl) }
                                )
                            }
                        }
                    }
                    item {
                        ActionPill(
                            icon = if (state.isSavedToWatchlist) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            label = if (state.isSavedToWatchlist) "Saved" else "Save",
                            containerColor = SurfaceLevel2,
                            contentColor = if (state.isSavedToWatchlist) CrimsonAccent else TextPrimary,
                            onClick = { viewModel.toggleWatchlist() }
                        )
                    }
                    item {
                        ActionPill(
                            icon = Icons.Rounded.Share,
                            label = "Share",
                            containerColor = SurfaceLevel2,
                            contentColor = TextPrimary,
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, state.video.title)
                                    putExtra(Intent.EXTRA_TEXT, "https://youtu.be/${state.video.id}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                            }
                        )
                    }
                }
            }

            // Channel Card
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
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val avatar = state.channelInfo?.avatarUrl ?: state.video.channelAvatarUrl
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceLevel2),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = state.video.channelTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = CrimsonAccent
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.channelInfo?.title?.ifBlank { state.video.channelTitle }
                                    ?: state.video.channelTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subs = state.channelInfo?.subscriberCount ?: 0L
                            if (subs > 0) {
                                Text(
                                    text = "${FormatUtils.formatCount(subs)} subscribers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (state.video.likeCount > 0) {
                            Surface(
                                color = CrimsonSoft,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${FormatUtils.formatCount(state.video.likeCount)} likes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Comments Card
            item {
                Surface(
                    color = SurfaceLevel1,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OutlineSubtle, RoundedCornerShape(16.dp))
                        .clickable { isCommentsExpanded = !isCommentsExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Comment,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (state.video.commentCount > 0) {
                                        "Comments (${FormatUtils.formatCount(state.video.commentCount)})"
                                    } else {
                                        "Comments"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = if (isCommentsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = "Toggle comments",
                                tint = TextSecondary
                            )
                        }

                        if (state.isLoadingComments) {
                            CircularProgressIndicator(
                                color = CrimsonAccent,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (state.comments.isNotEmpty()) {
                            val visibleComments = if (isCommentsExpanded) state.comments else state.comments.take(1)
                            visibleComments.forEach { comment ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    AsyncImage(
                                        model = comment.authorAvatarUrl,
                                        contentDescription = comment.authorName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceLevel2)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comment.authorName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            maxLines = if (isCommentsExpanded) 6 else 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = state.errorMessage ?: "Comments unavailable for this video.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Related / Up Next
            if (state.relatedVideos.isNotEmpty()) {
                item {
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                items(state.relatedVideos, key = { it.id }) { related ->
                    CompactVideoRow(
                        video = related,
                        isDownloaded = related.id in downloadedIds,
                        onClick = { onRelatedClick(related) },
                        onDownloadClick = { onDownloadClick(related) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.2f) else SurfaceLevel1,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (selected) accent else OutlineSubtle,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accent else TextSecondary
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}
