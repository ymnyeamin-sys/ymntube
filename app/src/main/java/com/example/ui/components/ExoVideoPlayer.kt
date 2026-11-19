package com.example.ui.components

import android.content.ComponentName
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.media.PlaybackService
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

data class ExoMediaSpec(
    val id: String,
    val uriOrPath: String,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = ""
)

@OptIn(UnstableApi::class)
@Composable
fun ExoVideoPlayer(
    mediaUriOrPath: String,
    modifier: Modifier = Modifier,
    title: String = "",
    artist: String = "",
    artworkUrl: String = "",
    playlist: List<ExoMediaSpec> = emptyList(),
    currentItemId: String = "",
    backgroundPlayEnabled: Boolean = true,
    isFullscreen: Boolean = false,
    onCurrentItemChanged: ((String) -> Unit)? = null,
    onFullscreenToggle: ((Boolean) -> Unit)? = null,
    onPlaybackError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentPlaybackError by rememberUpdatedState(onPlaybackError)
    val currentItemCallback by rememberUpdatedState(onCurrentItemChanged)
    val latestBackgroundPlay by rememberUpdatedState(backgroundPlayEnabled)

    var activePlayer by remember { mutableStateOf<Player?>(null) }
    var isControllerOwned by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    var showControls by remember { mutableStateOf(true) }
    var isTouchLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var repeatOne by remember { mutableStateOf(false) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var controlInteractionNonce by remember { mutableIntStateOf(0) }

    val effectivePlaylist = remember(playlist, mediaUriOrPath, title, artist, artworkUrl, currentItemId) {
        if (playlist.isNotEmpty()) {
            playlist
        } else {
            listOf(
                ExoMediaSpec(
                    id = currentItemId.ifBlank { mediaUriOrPath },
                    uriOrPath = mediaUriOrPath,
                    title = title,
                    artist = artist,
                    artworkUrl = artworkUrl
                )
            )
        }
    }

    val targetIndex = remember(effectivePlaylist, currentItemId, mediaUriOrPath) {
        effectivePlaylist.indexOfFirst {
            (currentItemId.isNotBlank() && it.id == currentItemId) || it.uriOrPath == mediaUriOrPath
        }.coerceAtLeast(0)
    }

    // Connect to MediaSessionService only while lifecycle >= STARTED & background play enabled
    DisposableEffect(context, lifecycleOwner, backgroundPlayEnabled) {
        var released = false
        var localFallback: ExoPlayer? = null
        var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
        var startObserver: LifecycleEventObserver? = null

        fun initLocalPlayer() {
            if (released || activePlayer != null) return
            isControllerOwned = false
            localFallback = runCatching { ExoPlayer.Builder(context).build() }.getOrNull()?.also {
                activePlayer = it
            }
        }

        fun connectSessionController() {
            if (released || controllerFuture != null) return
            runCatching {
                val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val future = MediaController.Builder(context, sessionToken).buildAsync()
                controllerFuture = future
                future.addListener(
                    {
                        if (released) return@addListener
                        val controller = runCatching { future.get() }.getOrNull()
                        if (controller != null) {
                            isControllerOwned = true
                            activePlayer = controller
                        } else {
                            initLocalPlayer()
                        }
                    },
                    ContextCompat.getMainExecutor(context)
                )
            }.onFailure {
                initLocalPlayer()
            }
        }

        if (!backgroundPlayEnabled) {
            initLocalPlayer()
        } else if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            connectSessionController()
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (!released && (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME)) {
                    connectSessionController()
                }
            }
            startObserver = observer
            lifecycleOwner.lifecycle.addObserver(observer)
        }

        onDispose {
            released = true
            startObserver?.let { lifecycleOwner.lifecycle.removeObserver(it) }
            val p = activePlayer
            if (p != null && !latestBackgroundPlay) {
                runCatching { p.pause() }
            }
            controllerFuture?.let { MediaController.releaseFuture(it) }
            runCatching { localFallback?.release() }
            activePlayer = null
        }
    }

    // Listener for playback state & errors
    DisposableEffect(activePlayer) {
        val player = activePlayer ?: return@DisposableEffect onDispose {}
        isPlaying = player.isPlaying
        isBuffering = player.playbackState == Player.STATE_BUFFERING
        durationMs = player.duration.coerceAtLeast(0L)
        positionMs = player.currentPosition.coerceAtLeast(0L)

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId
                if (!id.isNullOrBlank()) {
                    currentItemCallback?.invoke(id)
                }
                durationMs = player.duration.coerceAtLeast(0L)
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                currentPlaybackError?.invoke(
                    error.localizedMessage ?: "Native playback failed (${error.errorCodeName})"
                )
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    // Load or switch item inside MediaController / ExoPlayer
    LaunchedEffect(activePlayer, effectivePlaylist, targetIndex) {
        val player = activePlayer ?: return@LaunchedEffect
        val targetSpec = effectivePlaylist.getOrNull(targetIndex) ?: return@LaunchedEffect

        val currentMediaIds = (0 until player.mediaItemCount).map { idx ->
            player.getMediaItemAt(idx).mediaId
        }
        val desiredMediaIds = effectivePlaylist.map { it.id }

        if (currentMediaIds != desiredMediaIds) {
            val mediaItems = effectivePlaylist.map { spec ->
                val uri = toPlayableUri(spec.uriOrPath)
                val metadata = MediaMetadata.Builder()
                    .setTitle(spec.title.ifBlank { "YmnTube Media" })
                    .setArtist(spec.artist.ifBlank { "YmnTube" })
                    .apply {
                        if (spec.artworkUrl.isNotBlank()) {
                            setArtworkUri(spec.artworkUrl.toUri())
                        }
                    }
                    .build()
                MediaItem.Builder()
                    .setMediaId(spec.id)
                    .setUri(uri)
                    .setMediaMetadata(metadata)
                    .build()
            }
            player.setMediaItems(mediaItems, targetIndex, 0L)
            player.prepare()
            player.playWhenReady = true
        } else if (player.currentMediaItem?.mediaId != targetSpec.id) {
            player.seekToDefaultPosition(targetIndex)
            player.prepare()
            player.playWhenReady = true
        }
    }

    // Apply speed & repeat settings
    LaunchedEffect(activePlayer, playbackSpeed, repeatOne) {
        val player = activePlayer ?: return@LaunchedEffect
        player.setPlaybackSpeed(playbackSpeed)
        player.repeatMode = if (repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Live timeline updater
    LaunchedEffect(activePlayer) {
        val player = activePlayer ?: return@LaunchedEffect
        while (isActive) {
            if (!isScrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration
                if (dur > 0) durationMs = dur
            }
            delay(250L)
        }
    }

    // Auto-hide HUD while playing
    LaunchedEffect(showControls, isPlaying, isTouchLocked, controlInteractionNonce) {
        if (showControls && isPlaying && !isTouchLocked) {
            delay(3500L)
            showControls = false
        }
    }

    // Clear seek feedback toast
    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(900L)
            seekFeedbackText = null
        }
    }

    // Pause on background only if user explicitly disabled background play
    DisposableEffect(lifecycleOwner, activePlayer, isControllerOwned) {
        val observer = LifecycleEventObserver { _, event ->
            if (!latestBackgroundPlay &&
                (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP)
            ) {
                activePlayer?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isTouchLocked, showControls, activePlayer) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                        controlInteractionNonce++
                    },
                    onDoubleTap = { offset ->
                        if (isTouchLocked) return@detectTapGestures
                        val player = activePlayer ?: return@detectTapGestures
                        val isLeft = offset.x < size.width / 2f
                        val deltaMs = if (isLeft) -10_000L else 10_000L
                        val target = (player.currentPosition + deltaMs)
                            .coerceIn(0L, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
                        player.seekTo(target)
                        positionMs = target
                        seekFeedbackText = if (isLeft) "-10s" else "+10s"
                        controlInteractionNonce++
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    this.resizeMode = resizeMode
                    player = activePlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player != activePlayer) {
                    view.player = activePlayer
                }
                if (view.resizeMode != resizeMode) {
                    view.resizeMode = resizeMode
                }
            }
        )

        if (isBuffering && activePlayer != null) {
            CircularProgressIndicator(
                color = CrimsonAccent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp),
                strokeWidth = 3.dp
            )
        }

        // Double-tap seek feedback badge
        AnimatedVisibility(
            visible = seekFeedbackText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color(0xCC09090D),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = seekFeedbackText.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        // Touch Locked Unlock Button
        AnimatedVisibility(
            visible = isTouchLocked && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Surface(
                color = Color(0xCC09090D),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        isTouchLocked = false
                        showControls = true
                        controlInteractionNonce++
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = "Unlock player controls",
                        tint = CyanElectric,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Screen Locked • Tap to Unlock",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }

        // Full Custom Player HUD
        AnimatedVisibility(
            visible = showControls && !isTouchLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB3000000),
                                Color(0x33000000),
                                Color(0xCC000000)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Top utility row (leave left space for back button overlaid by parent screen)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxWidth()
                        .padding(start = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (title.isNotBlank() && isFullscreen) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (backgroundPlayEnabled) {
                        Surface(
                            color = Color(0x9909090D),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Headphones,
                                    contentDescription = "Background lock-screen play enabled",
                                    tint = CyanElectric,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "BG",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanElectric
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Speed cycle pill
                    Surface(
                        color = Color(0x99090D16),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                playbackSpeed = speeds[nextIdx]
                                controlInteractionNonce++
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = "Playback speed",
                                tint = TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${playbackSpeed}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Resize mode pill
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            controlInteractionNonce++
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x9909090D))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = "Cycle video aspect fit",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Loop toggle
                    IconButton(
                        onClick = {
                            repeatOne = !repeatOne
                            controlInteractionNonce++
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x9909090D))
                    ) {
                        Icon(
                            imageVector = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Toggle loop",
                            tint = if (repeatOne) CrimsonAccent else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Touch Lock toggle
                    IconButton(
                        onClick = {
                            isTouchLocked = true
                            showControls = true
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x9909090D))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Lock screen touches",
                            tint = TextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Center Transport Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (effectivePlaylist.size > 1) {
                        IconButton(
                            onClick = {
                                val p = activePlayer ?: return@IconButton
                                if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem()
                                controlInteractionNonce++
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(Color(0x8809090D))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous video",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val p = activePlayer ?: return@IconButton
                            p.seekTo((p.currentPosition - 10_000L).coerceAtLeast(0L))
                            positionMs = p.currentPosition
                            controlInteractionNonce++
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(Color(0x8809090D))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val p = activePlayer ?: return@IconButton
                            if (p.isPlaying) {
                                p.pause()
                            } else {
                                if (p.playbackState == Player.STATE_ENDED) {
                                    p.seekTo(0L)
                                }
                                p.play()
                            }
                            controlInteractionNonce++
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CrimsonAccent)
                            .testTag("exo_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val p = activePlayer ?: return@IconButton
                            val dur = p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            p.seekTo((p.currentPosition + 10_000L).coerceAtMost(dur))
                            positionMs = p.currentPosition
                            controlInteractionNonce++
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(Color(0x8809090D))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    if (effectivePlaylist.size > 1) {
                        IconButton(
                            onClick = {
                                val p = activePlayer ?: return@IconButton
                                if (p.hasNextMediaItem()) p.seekToNextMediaItem()
                                controlInteractionNonce++
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(Color(0x8809090D))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next video",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline & Fullscreen Row
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    val displayedPos = if (isScrubbing) scrubPositionMs else positionMs
                    val maxDur = durationMs.coerceAtLeast(1L)
                    Slider(
                        value = displayedPos.toFloat().coerceIn(0f, maxDur.toFloat()),
                        onValueChange = { value ->
                            isScrubbing = true
                            scrubPositionMs = value.toLong()
                            controlInteractionNonce++
                        },
                        onValueChangeFinished = {
                            activePlayer?.seekTo(scrubPositionMs)
                            positionMs = scrubPositionMs
                            isScrubbing = false
                            controlInteractionNonce++
                        },
                        valueRange = 0f..maxDur.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = CrimsonAccent,
                            activeTrackColor = CrimsonAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatMillis(displayedPos)} / ${formatMillis(durationMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )

                        if (onFullscreenToggle != null) {
                            IconButton(
                                onClick = {
                                    onFullscreenToggle(!isFullscreen)
                                    controlInteractionNonce++
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) {
                                        Icons.Rounded.FullscreenExit
                                    } else {
                                        Icons.Rounded.Fullscreen
                                    },
                                    contentDescription = "Toggle fullscreen",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toPlayableUri(mediaUriOrPath: String): Uri {
    return if (mediaUriOrPath.startsWith("http://") ||
        mediaUriOrPath.startsWith("https://") ||
        mediaUriOrPath.startsWith("content://") ||
        mediaUriOrPath.startsWith("file://")
    ) {
        mediaUriOrPath.toUri()
    } else {
        Uri.fromFile(File(mediaUriOrPath))
    }
}

private fun formatMillis(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
