package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.AppContainer
import com.example.data.model.AppUpdateInfo
import com.example.data.model.DownloadJobStatus
import com.example.data.model.VideoItem
import com.example.ui.components.DownloadQualitySheet
import com.example.ui.navigation.DownloadsRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.LibraryRoute
import com.example.ui.navigation.OfflinePlayerRoute
import com.example.ui.navigation.SearchRoute
import com.example.ui.navigation.WatchRoute
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OfflinePlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.WatchScreen
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DownloadPromptViewModel
import com.example.ui.viewmodel.DownloadSheetState
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.WatchViewModel

private data class TopDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun VeloTubeApp() {
    val context = LocalContext.current
    val container = remember(context) { AppContainer.getInstance(context) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            container.youTubeRepository,
            container.downloadRepository,
            container.userPreferencesRepository
        )
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(
            container.youTubeRepository,
            container.downloadRepository
        )
    )
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(
            container.youTubeRepository,
            container.downloadRepository,
            container.userPreferencesRepository
        )
    )
    val downloadPromptViewModel: DownloadPromptViewModel = viewModel(
        factory = DownloadPromptViewModel.Factory(
            container.downloadRepository,
            container.userPreferencesRepository
        )
    )

    val downloadSheetState by downloadPromptViewModel.sheetState.collectAsStateWithLifecycle()
    val activeDownloads by downloadPromptViewModel.activeDownloads.collectAsStateWithLifecycle()
    val downloadedVideos by downloadPromptViewModel.downloadedVideos.collectAsStateWithLifecycle()
    val downloadedIds by homeViewModel.downloadedIds.collectAsStateWithLifecycle()
    val statusToast by downloadPromptViewModel.statusToast.collectAsStateWithLifecycle()
    val availableUpdate by downloadPromptViewModel.availableUpdate.collectAsStateWithLifecycle()
    val prefs by libraryViewModel.preferences.collectAsStateWithLifecycle()

    var isPlayerFullscreen by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val ensureNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        downloadPromptViewModel.checkForAppUpdate(manual = false)
    }

    LaunchedEffect(currentDestination) {
        isPlayerFullscreen = false
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusToast) {
        val msg = statusToast ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        downloadPromptViewModel.clearToast()
    }

    val activeTransfers = activeDownloads.values.filter {
        it.status == DownloadJobStatus.RESOLVING ||
            it.status == DownloadJobStatus.DOWNLOADING ||
            it.status == DownloadJobStatus.EXPORTING_GALLERY ||
            it.status == DownloadJobStatus.QUEUED
    }
    val activeTransferCount = activeTransfers.size
    val primaryTransfer = activeTransfers.firstOrNull()

    val topDestinations = remember {
        listOf(
            TopDestination(
                route = HomeRoute,
                labelRes = R.string.nav_home,
                selectedIcon = Icons.Rounded.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_home"
            ),
            TopDestination(
                route = SearchRoute,
                labelRes = R.string.nav_search,
                selectedIcon = Icons.Rounded.Search,
                unselectedIcon = Icons.Outlined.Search,
                testTag = "nav_search"
            ),
            TopDestination(
                route = DownloadsRoute,
                labelRes = R.string.nav_downloads,
                selectedIcon = Icons.Rounded.DownloadForOffline,
                unselectedIcon = Icons.Outlined.DownloadForOffline,
                testTag = "nav_downloads"
            ),
            TopDestination(
                route = LibraryRoute,
                labelRes = R.string.nav_library,
                selectedIcon = Icons.Rounded.VideoLibrary,
                unselectedIcon = Icons.Outlined.VideoLibrary,
                testTag = "nav_library"
            )
        )
    }

    val navigateToTopLevel: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openWatch: (VideoItem) -> Unit = { video ->
        navController.navigate(
            WatchRoute(
                videoId = video.id,
                initialTitle = video.title,
                initialChannel = video.channelTitle,
                initialThumb = video.thumbnailUrl
            )
        )
    }

    val isDownloadsScreen = currentDestination?.hasRoute<DownloadsRoute>() == true

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val showChrome = !isPlayerFullscreen
        val useRail = maxWidth >= 680.dp && showChrome

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showChrome) {
                    Column {
                        if (primaryTransfer != null && !isDownloadsScreen) {
                            MiniDownloadProgressBar(
                                title = primaryTransfer.title,
                                quality = primaryTransfer.qualityLabel,
                                progress = primaryTransfer.progress,
                                status = primaryTransfer.status,
                                totalCount = activeTransferCount,
                                onClick = { navigateToTopLevel(DownloadsRoute) }
                            )
                        }

                        if (!useRail) {
                            NavigationBar(
                                containerColor = SurfaceLevel1,
                                tonalElevation = 0.dp
                            ) {
                                topDestinations.forEach { dest ->
                                    val selected = currentDestination?.hasRoute(dest.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTopLevel(dest.route) },
                                        icon = {
                                            NavIconWithBadge(
                                                icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                label = stringResource(dest.labelRes),
                                                badgeCount = if (dest.route is DownloadsRoute) activeTransferCount else 0
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(dest.labelRes),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CrimsonAccent,
                                            selectedTextColor = TextPrimary,
                                            indicatorColor = CrimsonSoft,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (showChrome) Modifier.padding(innerPadding) else Modifier)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = SurfaceLevel1,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(dest.route) },
                                icon = {
                                    NavIconWithBadge(
                                        icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        label = stringResource(dest.labelRes),
                                        badgeCount = if (dest.route is DownloadsRoute) activeTransferCount else 0
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CrimsonAccent,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = CrimsonSoft,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onVideoClick = openWatch,
                                onDownloadClick = { downloadPromptViewModel.openDownloadSheet(it) },
                                onSearchIconClick = { navigateToTopLevel(SearchRoute) }
                            )
                        }

                        composable<SearchRoute> {
                            SearchScreen(
                                viewModel = searchViewModel,
                                activeDownloads = activeDownloads,
                                onVideoClick = openWatch,
                                onDownloadClick = { downloadPromptViewModel.openDownloadSheet(it) }
                            )
                        }

                        composable<DownloadsRoute> {
                            DownloadsScreen(
                                downloadViewModel = downloadPromptViewModel,
                                onPlayOffline = { entity ->
                                    ensureNotificationPermission()
                                    navController.navigate(OfflinePlayerRoute(entity.downloadId))
                                }
                            )
                        }

                        composable<LibraryRoute> {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                downloadedIds = downloadedIds,
                                activeDownloads = activeDownloads,
                                onVideoClick = openWatch,
                                onDownloadClick = { downloadPromptViewModel.openDownloadSheet(it) },
                                onCheckForUpdate = { downloadPromptViewModel.checkForAppUpdate(manual = true) }
                            )
                        }

                        composable<WatchRoute> { entry ->
                            val args = entry.toRoute<WatchRoute>()
                            val watchViewModel: WatchViewModel = viewModel(
                                key = "watch_${args.videoId}",
                                factory = WatchViewModel.Factory(
                                    videoId = args.videoId,
                                    initialTitle = args.initialTitle,
                                    initialChannel = args.initialChannel,
                                    initialThumb = args.initialThumb,
                                    youTubeRepository = container.youTubeRepository,
                                    downloadRepository = container.downloadRepository
                                )
                            )
                            LaunchedEffect(Unit) {
                                ensureNotificationPermission()
                            }
                            WatchScreen(
                                viewModel = watchViewModel,
                                downloadedIds = downloadedIds,
                                activeDownloads = activeDownloads,
                                backgroundPlayEnabled = prefs.backgroundPlayEnabled,
                                onFullscreenChanged = { isPlayerFullscreen = it },
                                onBack = { navController.popBackStack() },
                                onRelatedClick = openWatch,
                                onDownloadClick = { downloadPromptViewModel.openDownloadSheet(it) },
                                onOpenInGallery = { downloadPromptViewModel.openInGallery(it) }
                            )
                        }

                        composable<OfflinePlayerRoute> { entry ->
                            val args = entry.toRoute<OfflinePlayerRoute>()
                            var selectedDownloadId by rememberSaveable(args.downloadId) {
                                mutableStateOf(args.downloadId)
                            }
                            val entity = downloadedVideos.firstOrNull { it.downloadId == selectedDownloadId }
                                ?: downloadedVideos.firstOrNull { it.downloadId == args.downloadId }

                            LaunchedEffect(Unit) {
                                ensureNotificationPermission()
                            }
                            OfflinePlayerScreen(
                                entity = entity,
                                allDownloads = downloadedVideos,
                                backgroundPlayEnabled = prefs.backgroundPlayEnabled,
                                onSelectVideo = { selectedDownloadId = it.downloadId },
                                onToggleBackgroundPlay = { libraryViewModel.setBackgroundPlayEnabled(it) },
                                onFullscreenChanged = { isPlayerFullscreen = it },
                                onBack = { navController.popBackStack() },
                                onOpenInGallery = { downloadPromptViewModel.openInGallery(it) },
                                onExportToGallery = { downloadPromptViewModel.exportToGallery(it) },
                                onShare = { downloadPromptViewModel.shareVideo(it) },
                                onDelete = { downloadPromptViewModel.deleteVideo(it) }
                            )
                        }
                    }
                }
            }
        }

        val readyVideoId = (downloadSheetState as? DownloadSheetState.Ready)?.video?.id
        val savedQualitiesForSheet = remember(downloadedVideos, readyVideoId) {
            if (readyVideoId == null) {
                emptySet()
            } else {
                downloadedVideos.filter { it.videoId == readyVideoId }
                    .flatMap {
                        listOf(
                            it.qualityLabel.lowercase(),
                            it.downloadId.substringAfterLast('_').lowercase()
                        )
                    }
                    .toSet()
            }
        }

        DownloadQualitySheet(
            state = downloadSheetState,
            downloadedQualities = savedQualitiesForSheet,
            onDismiss = { downloadPromptViewModel.dismissSheet() },
            onSelectFormat = { video, format ->
                ensureNotificationPermission()
                downloadPromptViewModel.startSelectedDownload(video, format)
            }
        )

        availableUpdate?.let { updateInfo ->
            AppUpdateDialog(
                info = updateInfo,
                onUpdateNow = {
                    downloadPromptViewModel.onUpdateAccepted(updateInfo)
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, updateInfo.updateUrl.toUri()).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                onSkipVersion = {
                    downloadPromptViewModel.onSkipUpdateVersion(updateInfo)
                },
                onDismiss = {
                    downloadPromptViewModel.dismissUpdateDialog()
                }
            )
        }
    }
}

@Composable
private fun MiniDownloadProgressBar(
    title: String,
    quality: String,
    progress: Float,
    status: DownloadJobStatus,
    totalCount: Int,
    onClick: () -> Unit
) {
    val pct = (progress * 100f).toInt().coerceIn(0, 100)
    Surface(
        color = SurfaceLevel2,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Downloading,
                    contentDescription = "Downloading",
                    tint = CyanElectric,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val extra = if (totalCount > 1) " (+${totalCount - 1} more)" else ""
                    val statusText = if (status == DownloadJobStatus.EXPORTING_GALLERY) {
                        "Saving to Gallery…$extra"
                    } else {
                        "Downloading $quality • $pct%$extra"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanElectric
                    )
                }
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyanElectric
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = CrimsonAccent,
                trackColor = OutlineSubtle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}

@Composable
private fun AppUpdateDialog(
    info: AppUpdateInfo,
    onUpdateNow: () -> Unit,
    onSkipVersion: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLevel1,
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = CyanElectric,
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = info.title.ifBlank { "Update Available" },
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = CrimsonSoft,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, CrimsonAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "New Version: ${info.version}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = info.changelog.ifBlank {
                        "A new version of YmnTube is available with improvements and fixes."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonAccent,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Update Now")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSkipVersion) {
                    Text("Skip", color = TextSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("Later", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
private fun NavIconWithBadge(
    icon: ImageVector,
    label: String,
    badgeCount: Int
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = CrimsonAccent,
                    contentColor = TextPrimary
                ) {
                    Text(badgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = label)
    }
}
