package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DownloadedVideoEntity
import com.example.data.remote.FormatUtils
import com.example.ui.components.ExoVideoPlayer
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.CyanSoft
import com.example.ui.theme.EmeraldGallery
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OfflinePlayerScreen(
    entity: DownloadedVideoEntity?,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .then(if (isFullscreen) Modifier else Modifier.statusBarsPadding())
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
                modifier = Modifier.fillMaxSize(),
                onFullscreenToggle = { isFullscreen = it }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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

            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Storage Location",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = if (entity.isSavedToGallery) {
                            "Available offline inside VeloTube Vault and in your device Gallery under Movies/VeloTube."
                        } else {
                            "Stored offline inside VeloTube Vault."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
    }
}
