package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DownloadedVideoEntity
import com.example.data.model.ActiveDownload
import com.example.data.model.DownloadJobStatus
import com.example.data.remote.FormatUtils
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
import com.example.ui.viewmodel.DownloadPromptViewModel

@Composable
fun DownloadsScreen(
    downloadViewModel: DownloadPromptViewModel,
    onPlayOffline: (DownloadedVideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMap by downloadViewModel.activeDownloads.collectAsStateWithLifecycle()
    val downloadedList by downloadViewModel.downloadedVideos.collectAsStateWithLifecycle()
    var urlInput by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    val activeList = activeMap.values.toList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Downloads & Gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Saved inside VeloTube Vault and synced to Movies/VeloTube in your device Gallery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // URL Quick-Downloader Bar
        item {
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineSubtle, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Quick Link Downloader",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.paste_url_hint),
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = CyanElectric
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    keyboard?.hide()
                                    downloadViewModel.openDownloadFromInput(urlInput)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceLevel2,
                                unfocusedContainerColor = SurfaceLevel2,
                                focusedBorderColor = CrimsonAccent,
                                unfocusedBorderColor = OutlineSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_download_url_input")
                        )
                        Button(
                            onClick = {
                                keyboard?.hide()
                                downloadViewModel.openDownloadFromInput(urlInput)
                            },
                            enabled = urlInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonAccent,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("quick_download_fetch_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Fetch download formats",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Active Downloads
        if (activeList.isNotEmpty()) {
            item {
                Text(
                    text = "Active Transfers (${activeList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            items(activeList, key = { it.downloadId }) { job ->
                ActiveDownloadCard(
                    job = job,
                    onCancel = { downloadViewModel.cancelDownload(job.downloadId) },
                    onDismiss = { downloadViewModel.dismissActiveItem(job.downloadId) }
                )
            }
        }

        // Offline Vault Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Offline Vault (${downloadedList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (downloadedList.isNotEmpty()) {
                    val totalSize = downloadedList.sumOf { it.fileSizeBytes }
                    Text(
                        text = FormatUtils.formatBytes(totalSize),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanElectric
                    )
                }
            }
        }

        if (downloadedList.isEmpty()) {
            item {
                Surface(
                    color = SurfaceLevel1,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OutlineSubtle, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_downloads),
                            contentDescription = "Empty offline vault",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Text(
                            text = stringResource(R.string.empty_downloads_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.empty_downloads_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(downloadedList, key = { it.videoId }) { entity ->
                DownloadedVaultCard(
                    entity = entity,
                    onPlayOffline = { onPlayOffline(entity) },
                    onOpenInGallery = { downloadViewModel.openInGallery(entity) },
                    onExportToGallery = { downloadViewModel.exportToGallery(entity) },
                    onShare = { downloadViewModel.shareVideo(entity) },
                    onDelete = { downloadViewModel.deleteVideo(entity) }
                )
            }
        }
    }
}

@Composable
private fun ActiveDownloadCard(
    job: ActiveDownload,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = SurfaceLevel1,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineSubtle, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val statusLabel = when (job.status) {
                        DownloadJobStatus.QUEUED -> "Queued…"
                        DownloadJobStatus.RESOLVING -> "Generating download link…"
                        DownloadJobStatus.DOWNLOADING -> {
                            val pct = (job.progress * 100).toInt()
                            val downloaded = FormatUtils.formatBytes(job.bytesDownloaded)
                            if (job.totalBytes > 0) {
                                "$pct% • $downloaded / ${FormatUtils.formatBytes(job.totalBytes)}"
                            } else {
                                "Downloading • $downloaded"
                            }
                        }
                        DownloadJobStatus.EXPORTING_GALLERY -> "Syncing to device Gallery…"
                        DownloadJobStatus.COMPLETED -> "Completed & synced"
                        DownloadJobStatus.FAILED -> job.errorMessage ?: "Download failed"
                    }
                    Text(
                        text = "${job.qualityLabel} • $statusLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (job.status) {
                            DownloadJobStatus.COMPLETED -> EmeraldGallery
                            DownloadJobStatus.FAILED -> CrimsonAccent
                            else -> CyanElectric
                        }
                    )
                }

                IconButton(
                    onClick = if (job.status == DownloadJobStatus.COMPLETED || job.status == DownloadJobStatus.FAILED) {
                        onDismiss
                    } else {
                        onCancel
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel or dismiss",
                        tint = TextSecondary
                    )
                }
            }

            if (job.status == DownloadJobStatus.RESOLVING ||
                job.status == DownloadJobStatus.DOWNLOADING ||
                job.status == DownloadJobStatus.EXPORTING_GALLERY
            ) {
                if ((job.status == DownloadJobStatus.DOWNLOADING && job.totalBytes > 0) ||
                    job.status == DownloadJobStatus.EXPORTING_GALLERY
                ) {
                    LinearProgressIndicator(
                        progress = { job.progress },
                        color = CrimsonAccent,
                        trackColor = SurfaceLevel2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    LinearProgressIndicator(
                        color = CrimsonAccent,
                        trackColor = SurfaceLevel2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedVaultCard(
    entity: DownloadedVideoEntity,
    onPlayOffline: () -> Unit,
    onOpenInGallery: () -> Unit,
    onExportToGallery: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = SurfaceLevel1,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineSubtle, RoundedCornerShape(18.dp))
            .clickable(onClick = onPlayOffline)
            .testTag("downloaded_card_${entity.videoId}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceLevel2)
                ) {
                    if (entity.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = entity.thumbnailUrl,
                            contentDescription = entity.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xCC09090D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play offline",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (entity.durationText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xCC09090D))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = entity.durationText,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = entity.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entity.channelTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CyanSoft,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${entity.qualityLabel} • ${FormatUtils.formatBytes(entity.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanElectric,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        if (entity.isSavedToGallery) {
                            Surface(
                                color = EmeraldSoft,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.saved_in_gallery),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGallery,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayOffline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonAccent,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = if (entity.isSavedToGallery) onOpenInGallery else onExportToGallery,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("gallery_btn_${entity.videoId}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PermMedia,
                        contentDescription = null,
                        tint = EmeraldGallery,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (entity.isSavedToGallery) "Gallery" else "Save Gallery",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldGallery,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share video",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete video",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
