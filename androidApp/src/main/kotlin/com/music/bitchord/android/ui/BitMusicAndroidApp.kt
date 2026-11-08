package com.music.bitchord.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.widget.Toast
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.bitchord.android.LogExporter
import com.music.bitchord.android.auth.AndroidYouTubeAuth
import com.music.bitchord.android.playback.AndroidPlayerHost
import com.music.bitchord.android.ui.player.CompactMiniPlayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.ui.player.PlayerPlatform
import com.music.bitchord.android.ui.screens.CompactCollectionScreen
import com.music.bitchord.android.ui.screens.CompactLibraryScreen
import com.music.bitchord.android.ui.screens.CompactProfileScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import com.music.bitchord.android.ThemePreference
import com.music.bitchord.android.ui.screens.CompactSettingsScreen
import com.music.bitchord.android.ui.screens.CompactSearchScreen
import com.music.bitchord.android.ui.theme.BitMusicCompactTheme
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.UiState
import com.music.bitchord.ui.player.NowPlayingScreen
import com.music.bitchord.ui.screens.HomeScreen
import dev.chrisbanes.haze.ExperimentalHazeApi
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Android shell redesigned for small screens:
 * - Bottom navigation for thumb reach (compact width)
 * - Persistent mini-player above nav
 * - Full-screen now playing overlay for portrait phones
 * - Single-column, touch-friendly content layouts
 *
 * All tracks and playback come from the shared BitMusic data layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitMusicAndroidApp(
    windowSizeClass: WindowSizeClass,
    viewModel: BitMusicViewModel = viewModel(),
) {
    val isCompactWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    var destination by rememberSaveable { mutableStateOf(CompactDestination.Home) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showFullSettings by rememberSaveable { mutableStateOf(false) }
    val playerState = viewModel.playerState
    val hazeState = remember { HazeState() }
    val androidSettings = (PlayerPlatform.host as? AndroidPlayerHost)?.androidSettings
    val reduceDynamicBlurFlow = androidSettings?.reduceDynamicBlur
    val reduceDynamicBlur by if (reduceDynamicBlurFlow != null) {
        reduceDynamicBlurFlow.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(false) }
    }
    val reduceAnimationFlow = androidSettings?.reduceAnimation
    val reduceAnimation by if (reduceAnimationFlow != null) {
        reduceAnimationFlow.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(false) }
    }
    val ytCookie by AndroidYouTubeAuth.cookie.collectAsStateWithLifecycle()
    val signedIn = !ytCookie.isNullOrBlank()
    val accountName = if (signedIn) "YouTube Music" else null
    val context = LocalContext.current
    val collectionOpen = viewModel.collectionLoading ||
        viewModel.collection != null ||
        viewModel.collectionError != null

    // Persisted appearance override: -1 follows the system, 0 light, 1 dark.
    var darkModeOverride by rememberSaveable { mutableStateOf(ThemePreference.get(context)) }
    val darkTheme = when (darkModeOverride) {
        1 -> true
        0 -> false
        else -> isSystemInDarkTheme()
    }
    fun setDarkTheme(enabled: Boolean) {
        darkModeOverride = if (enabled) 1 else 0
        ThemePreference.set(context, darkModeOverride)
    }

    // A collection opened from Home sits above the feed; back returns to it.
    BackHandler(enabled = collectionOpen) {
        viewModel.closeCollection()
    }

    BackHandler(enabled = showFullSettings) {
        showFullSettings = false
    }

    // The player screen owns its own back layers (lyrics, queue, drawers); when
    // none is open, back closes the player rather than leaving the app.
    BackHandler(enabled = !collectionOpen && showNowPlaying && playerState.current != null) {
        showNowPlaying = false
    }

    val view = LocalView.current
    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }

    BitMusicCompactTheme(darkTheme = darkTheme) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .hazeSource(hazeState),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!showNowPlaying) {
                    Column {
                        AnimatedVisibility(
                            visible = playerState.current != null,
                            enter = if (reduceAnimation) EnterTransition.None else slideInVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(140)),
                            exit = if (reduceAnimation) ExitTransition.None else slideOutVertically(animationSpec = tween(140, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(120)),
                        ) {
                            CompactMiniPlayer(
                                state = playerState,
                                onPlayPause = viewModel::togglePlayPause,
                                onOpen = { showNowPlaying = true },
                                hazeState = hazeState,
                            )
                        }
                        if (isCompactWidth) {
                            if (playerState.current != null) Spacer(Modifier.height(2.dp))
                            CompactBottomBar(
                                selected = destination,
                                onSelect = { destination = it },
                                hazeState = hazeState,
                                reduceDynamicBlur = reduceDynamicBlur,
                                reduceAnimation = reduceAnimation,
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            // Let content flow behind the floating glass bar so it reads as
            // floating rather than a solid footer; the bar's own padding keeps
            // the last row reachable.
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .consumeWindowInsets(innerPadding)
                .imePadding()

            if (collectionOpen) {
                CompactCollectionScreen(
                    modifier = contentModifier,
                    title = viewModel.collectionTitle,
                    songs = viewModel.collection?.songs.orEmpty(),
                    isLoading = viewModel.collectionLoading,
                    error = viewModel.collectionError,
                    onBack = viewModel::closeCollection,
                    onRetry = viewModel::retryCollection,
                    onSongClick = { song ->
                        viewModel.playCollectionSong(song)
                        showNowPlaying = true
                    },
                )
            } else {
                AnimatedContent(
                    targetState = destination,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val offset = { w: Int -> if (forward) w / 5 else -w / 5 }
                        val outOffset = { w: Int -> if (forward) -w / 6 else w / 6 }
                        (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(tween(220, easing = FastOutSlowInEasing), initialOffsetX = offset)) togetherWith
                            (fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(tween(180, easing = FastOutSlowInEasing), targetOffsetX = outOffset))
                    },
                    label = "tab-content",
                    modifier = Modifier.fillMaxSize(),
                ) { dest ->
                    when (dest) {
                        CompactDestination.Home -> HomeScreen(
                            state = viewModel.homeState,
                            listState = rememberLazyListState(),
                            onItemClick = { item, _ ->
                                if (item.videoId != null) showNowPlaying = true
                                viewModel.openShelfItem(item)
                            },
                            onRetry = viewModel::refreshHome,
                            refreshing = viewModel.isLoadingHome &&
                                viewModel.homeState is UiState.Success,
                            onRefresh = viewModel::refreshHome,
                            pullState = rememberPullToRefreshState(),
                            modifier = contentModifier.statusBarsPadding(),
                            contentPadding = PaddingValues(bottom = 120.dp),
                            title = "Listen now",
                            signedIn = false,
                            leadHero = true,
                            currentSong = playerState.current,
                            isPlaying = playerState.isPlaying,
                            onLoadMore = viewModel::loadMoreHome,
                            loadingMore = viewModel.isLoadingMoreHome,
                        )
                        CompactDestination.Search -> CompactSearchScreen(
                            modifier = contentModifier,
                            query = viewModel.searchQuery,
                            results = viewModel.searchResults,
                            filter = viewModel.searchFilter,
                            isSearching = viewModel.isSearching,
                            error = viewModel.searchError,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onFilterChange = viewModel::onSearchFilterChange,
                            onPlaySong = { song, queue -> viewModel.play(song, queue); showNowPlaying = true },
                            onOpenBrowse = { title, browseId, type -> viewModel.openCollection(browseId, title, type) },
                            suggestions = viewModel.searchSuggestions,
                            onPlaySuggestion = { song, queue -> viewModel.play(song, queue); showNowPlaying = true },
                            compact = isCompactWidth,
                        )
                        CompactDestination.Library -> CompactLibraryScreen(
                            modifier = contentModifier,
                            tracks = viewModel.libraryTracks,
                            isLoading = viewModel.isLoadingLibrary,
                            error = viewModel.libraryError,
                            onRetry = viewModel::refreshLibrary,
                            onTrackClick = { song -> viewModel.play(song, viewModel.libraryTracks); showNowPlaying = true },
                            compact = isCompactWidth,
                            hazeState = hazeState,
                        )
                        CompactDestination.Profile -> {
                            // Settings are reached from the account avatar, not a tab.
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && playerState.current != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val song = playerState.current ?: return@BoxWithConstraints
                var minimizeDrag by remember { mutableFloatStateOf(0f) }
                val playerWindowWidth = maxWidth
                val playerWindowHeight = maxHeight
                Box(Modifier.fillMaxSize()) {
                    NowPlayingScreen(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    isLoading = playerState.isBuffering,
                    audioVersionSwitching = false,
                    qualityUpgraded = false,
                    position = viewModel.playbackPosition,
                    durationMs = playerState.durationMs,
                    queue = playerState.queue,
                    queueIndex = viewModel.queueIndex,
                    hasPrevious = viewModel.hasPrevious,
                    hasNext = viewModel.hasNext,
                    repeatMode = viewModel.repeatMode,
                    shuffleEnabled = viewModel.shuffleEnabled,
                    autoplayEnabled = viewModel.autoplayEnabled,
                    signedIn = signedIn,
                    accountName = accountName,
                    likeStatus = viewModel.likeStatus(playerState.current),
                    onToggleLike = viewModel::toggleLikeCurrent,
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = { viewModel.skipNext() },
                    onPrevious = viewModel::skipPrevious,
                    onBlockedControl = {},
                    onSeek = viewModel::seekToMs,
                    onSeekFraction = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeat,
                    onToggleAutoplay = viewModel::toggleAutoplay,
                    onJumpTo = viewModel::jumpTo,
                    onRemoveFromQueue = viewModel::removeFromQueue,
                    onMoveInQueue = viewModel::moveInQueue,
                    onClearQueue = viewModel::clearQueue,
                    onOpenMenu = {},
                    onOpenAlbum = {},
                    onOpenArtist = { _, _ -> },
                    onOpenPlaybackSource = {},
                    onListenTogether = {
                        val shareText = buildString {
                            append(song.title)
                            if (song.artist.isNotBlank()) append(" — ").append(song.artist)
                            if (song.videoId.isNotBlank()) {
                                append("\nhttps://music.youtube.com/watch?v=").append(song.videoId)
                            }
                        }
                        runCatching {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(intent, "Share track")
                            )
                        }.onFailure {
                            com.music.bitchord.ui.player.PlayerPlatform.host.showMessage(
                                "Couldn’t open share sheet"
                            )
                        }
                    },
                    lyrics = viewModel.lyrics,
                    lyricsSource = viewModel.lyricsSource,
                    lyricsProviderStates = viewModel.lyricsProviderStates,
                    onSelectLyricsProvider = viewModel::selectLyricsProvider,
                    lyricsUnavailable = viewModel.lyricsUnavailable,
                    lyricsOffsetOpen = false,
                    onDismissLyricsOffset = {},
                    windowWidth = playerWindowWidth,
                    windowHeight = playerWindowHeight,
                )
                    // Swipe down from top edge to minimize the player.
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(160.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = { minimizeDrag = 0f },
                                    onDragCancel = { minimizeDrag = 0f },
                                    onVerticalDrag = { _, dragAmount ->
                                        minimizeDrag += dragAmount
                                        if (minimizeDrag > 140f) {
                                            minimizeDrag = 0f
                                            showNowPlaying = false
                                        } else if (minimizeDrag < -40f) {
                                            minimizeDrag = 0f
                                        }
                                    },
                                )
                            },
                    )
                }
            }
        }


        // Full settings page (not an overlay). Replaces the shell avatar while open.
        if (showFullSettings) {
            CompactSettingsScreen(
                modifier = Modifier.fillMaxSize(),
                themeMode = darkModeOverride,
                onThemeModeChange = { darkModeOverride = it },
                onExportLogs = {
                    val result = LogExporter.export(context)
                    Toast.makeText(
                        context,
                        result.getOrElse { it.message ?: "Export failed" }.let { name ->
                            if (result.isSuccess) "Saved $name to Downloads" else name
                        },
                        Toast.LENGTH_LONG,
                    ).show()
                },
                onBack = { showFullSettings = false },
            )
        } else if (!showNowPlaying) {
            // Circular account entry on the top-right; settings are reached only from here.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp),
            ) {
                Surface(
                    onClick = { showFullSettings = true },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50),
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Account and settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalHazeApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CompactBottomBar(
    selected: CompactDestination,
    onSelect: (CompactDestination) -> Unit,
    hazeState: HazeState,
    reduceDynamicBlur: Boolean = false,
    reduceAnimation: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val barShape = RoundedCornerShape(28.dp)
    val baseBg = if (reduceDynamicBlur) 0.94f else 0.86f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        var rowModifier = Modifier
            .height(CompactDimens.BottomNavHeight)
            .fillMaxWidth()
            .clip(barShape)
        if (!reduceDynamicBlur) {
            rowModifier = rowModifier.hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                    blurRadius = 20.dp,
                ),
            )
        }
        rowModifier = rowModifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = baseBg),
                        MaterialTheme.colorScheme.surface.copy(alpha = (baseBg - 0.1f).coerceAtLeast(0.7f)),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                shape = barShape,
            )
            .padding(horizontal = 8.dp)
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (dest in CompactDestination.entries.filter { it != CompactDestination.Profile }) {
                CompactNavItem(
                    dest = dest,
                    isSelected = dest == selected,
                    onSelect = onSelect,
                    haptic = haptic,
                    reduceAnimation = reduceAnimation,
                )
            }
        }
    }
}

@Composable
fun CompactNavItem(
    dest: CompactDestination,
    isSelected: Boolean,
    onSelect: (CompactDestination) -> Unit,
    haptic: HapticFeedback,
    reduceAnimation: Boolean = false,
) {
    val animMs = if (reduceAnimation) 0 else 140
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(durationMillis = animMs, easing = FastOutSlowInEasing),
        label = "nav-scale",
    )
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween<Color>(durationMillis = animMs),
        label = "nav-tint",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(dest)
            }
            .padding(vertical = 6.dp),
    ) {
        Icon(
            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
            contentDescription = dest.label,
            tint = iconTint,
            modifier = Modifier.size(24.dp).scale(scale),
        )
        AnimatedVisibility(
            visible = isSelected,
            enter = if (reduceAnimation) EnterTransition.None else fadeIn(tween(100)),
            exit = if (reduceAnimation) ExitTransition.None else fadeOut(tween(80)),
        ) {
            Text(
                text = dest.label,
                style = MaterialTheme.typography.labelMedium,
                color = iconTint,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
