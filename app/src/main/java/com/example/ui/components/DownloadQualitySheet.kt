package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DownloadFormat
import com.example.data.model.VideoItem
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.CyanSoft
import com.example.ui.theme.EmeraldGallery
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DownloadSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualitySheet(
    state: DownloadSheetState,
    onDismiss: () -> Unit,
    onSelectFormat: (VideoItem, DownloadFormat) -> Unit
) {
    if (state is DownloadSheetState.Hidden) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceLevel1,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (state) {
                is DownloadSheetState.Resolving -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CrimsonAccent,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Checking Available Resolutions…",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                is DownloadSheetState.Error -> {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = CrimsonSoft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                is DownloadSheetState.Ready -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = state.video.thumbnailUrl,
                            contentDescription = state.video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(104.dp)
                                .height(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceLevel2)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.video.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.video.channelTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldSoft)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PermMedia,
                            contentDescription = null,
                            tint = EmeraldGallery,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Saves inside VeloTube Vault + syncs to Android Gallery",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldGallery
                        )
                    }

                    Text(
                        text = "Available Qualities (${state.formats.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.formats, key = { it.formatId }) { fmt ->
                            FormatOptionRow(
                                format = fmt,
                                onClick = { onSelectFormat(state.video, fmt) }
                            )
                        }
                    }
                }

                DownloadSheetState.Hidden -> Unit
            }
        }
    }
}

@Composable
private fun FormatOptionRow(
    format: DownloadFormat,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, OutlineSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("format_option_${format.formatId}"),
        color = SurfaceLevel2,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (format.hasVideo) CyanSoft else CrimsonSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (format.hasVideo) Icons.Rounded.HighQuality else Icons.Rounded.AudioFile,
                    contentDescription = null,
                    tint = if (format.hasVideo) CyanElectric else CrimsonAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = format.qualityLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = if (format.hasVideo) "${format.ext.uppercase()} • Video + Audio" else "${format.ext.uppercase()} • Audio",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "Download ${format.qualityLabel}",
                tint = CrimsonAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
