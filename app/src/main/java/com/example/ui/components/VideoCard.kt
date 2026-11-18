package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.data.remote.FormatUtils
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.EmeraldGallery
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VideoCard(
    video: VideoItem,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, OutlineSubtle, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("video_card_${video.id}"),
        color = SurfaceLevel1,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(SurfaceLevel2)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0xB309090D)
                                )
                            )
                        )
                )

                if (isDownloaded) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xD909090D))
                            .border(1.dp, EmeraldGallery.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Downloaded offline",
                            tint = EmeraldGallery,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGallery
                        )
                    }
                }

                if (video.durationText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC09090D))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = video.durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceLevel2)
                        .border(1.dp, OutlineSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!video.channelAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = video.channelAvatarUrl,
                            contentDescription = video.channelTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = CrimsonAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val metaParts = buildList {
                        if (video.channelTitle.isNotBlank()) add(video.channelTitle)
                        if (video.viewCount > 0) add(FormatUtils.formatViews(video.viewCount))
                        val rel = FormatUtils.formatRelativeTime(video.publishedAt)
                        if (rel.isNotBlank()) add(rel)
                    }
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("download_btn_${video.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDownloaded) EmeraldSoft else SurfaceLevel2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                            contentDescription = "Download ${video.title}",
                            tint = if (isDownloaded) EmeraldGallery else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
