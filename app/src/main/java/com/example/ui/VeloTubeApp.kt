package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DownloadPromptViewModel
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

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusToast) {
        val msg = statusToast ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        downloadPromptViewModel.clearToast()
    }

    val activeTransferCount = activeDownloads.values.count {
        it.status == DownloadJobStatus.RESOLVING ||
            it.status == DownloadJobStatus.DOWNLOADING ||
            it.status == DownloadJobStatus.EXPORTING_GALLERY ||
            it.status == DownloadJobStatus.QUEUED
    }

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

    val isTopLevelRoute = currentDestination?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<SearchRoute>() ||
            dest.hasRoute<DownloadsRoute>() ||
            dest.hasRoute<LibraryRoute>()
    } ?: true

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useRail = maxWidth >= 680.dp && isTopLevelRoute

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (isTopLevelRoute && !useRail) {
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
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isTopLevelRoute) Modifier.padding(innerPadding) else Modifier)
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
                                onVideoClick = openWatch,
                                onDownloadClick = { downloadPromptViewModel.openDownloadSheet(it) }
                            )
                        }

                        composable<DownloadsRoute> {
                            DownloadsScreen(
                                downloadViewModel = downloadPromptViewModel,
                                onPlayOffline = { entity ->
                                    navController.navigate(OfflinePlayerRoute(entity.videoId))
                                }
                            )
                        }

                        composable<LibraryRoute> {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                downloadedIds = downloadedIds,
                                onVideoClick = openWatch,
                                onDownloadClick = { downloadPromptViewModel.openDownloadSheet(it) }
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
                            WatchScreen(
                                viewModel = watchViewModel,
                                downloadedIds = downloadedIds,
                                onBack = { navController.popBackStack() },
                                onRelatedClick = openWatch,
                                onDownloadClick = { downloadPromptViewModel.openDownloadSheet(it) },
                                onOpenInGallery = { downloadPromptViewModel.openInGallery(it) }
                            )
                        }

                        composable<OfflinePlayerRoute> { entry ->
                            val args = entry.toRoute<OfflinePlayerRoute>()
                            val entity = downloadedVideos.firstOrNull { it.videoId == args.videoId }
                            OfflinePlayerScreen(
                                entity = entity,
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

        DownloadQualitySheet(
            state = downloadSheetState,
            onDismiss = { downloadPromptViewModel.dismissSheet() },
            onSelectFormat = { video, format ->
                downloadPromptViewModel.startSelectedDownload(video, format)
            }
        )
    }
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
