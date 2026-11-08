package com.music.bitchord.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.bitchord.android.ui.player.CompactMiniPlayer
import com.music.bitchord.android.ui.player.PlayerSettingsSheet
import com.music.bitchord.android.ui.screens.CompactCollectionScreen
import com.music.bitchord.android.ui.screens.CompactLibraryScreen
import com.music.bitchord.android.ui.screens.CompactSearchScreen
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.UiState
import com.music.bitchord.ui.player.NowPlayingScreen
import com.music.bitchord.ui.screens.HomeScreen

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
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val playerState = viewModel.playerState
    val collectionOpen = viewModel.collectionLoading ||
        viewModel.collection != null ||
        viewModel.collectionError != null

    // A collection opened from Home sits above the feed; back returns to it.
    BackHandler(enabled = collectionOpen) {
        viewModel.closeCollection()
    }

    // The player screen owns its own back layers (lyrics, queue, drawers); when
    // none is open, back closes the player rather than leaving the app.
    BackHandler(enabled = !collectionOpen && showNowPlaying && playerState.current != null) {
        showNowPlaying = false
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!showNowPlaying) {
                    Column {
                        AnimatedVisibility(
                            visible = playerState.current != null,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it },
                        ) {
                            CompactMiniPlayer(
                                state = playerState,
                                onPlayPause = viewModel::togglePlayPause,
                                onOpen = { showNowPlaying = true },
                            )
                        }
                        if (isCompactWidth) {
                            CompactBottomBar(
                                selected = destination,
                                onSelect = { destination = it },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()

            when (destination) {
                CompactDestination.Home -> if (collectionOpen) {
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
                    HomeScreen(
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
                        contentPadding = PaddingValues(bottom = 24.dp),
                        title = "Listen now",
                        signedIn = false,
                        leadHero = false,
                        currentSong = playerState.current,
                        isPlaying = playerState.isPlaying,
                        onLoadMore = viewModel::loadMoreHome,
                        loadingMore = viewModel.isLoadingMoreHome,
                    )
                }
                CompactDestination.Search -> CompactSearchScreen(
                    modifier = contentModifier,
                    query = viewModel.searchQuery,
                    results = viewModel.searchResults,
                    isSearching = viewModel.isSearching,
                    error = viewModel.searchError,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onTrackClick = { song -> viewModel.play(song, viewModel.searchResults); showNowPlaying = true },
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
                )
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && playerState.current != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val song = playerState.current ?: return@BoxWithConstraints
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
                    signedIn = false,
                    accountName = null,
                    likeStatus = LikeStatus.INDIFFERENT,
                    onToggleLike = {},
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
                    onOpenMenu = { showSettings = true },
                    onOpenAlbum = {},
                    onOpenArtist = { _, _ -> },
                    onOpenPlaybackSource = {},
                    onListenTogether = {},
                    lyrics = viewModel.lyrics,
                    lyricsSource = viewModel.lyricsSource,
                    lyricsProviderStates = viewModel.lyricsProviderStates,
                    onSelectLyricsProvider = viewModel::selectLyricsProvider,
                    lyricsUnavailable = viewModel.lyricsUnavailable,
                    lyricsOffsetOpen = false,
                    onDismissLyricsOffset = {},
                    windowWidth = maxWidth,
                    windowHeight = maxHeight,
                )
            }
        }

        if (showSettings) {
            PlayerSettingsSheet(onDismiss = { showSettings = false })
        }
    }
}

@Composable
private fun CompactBottomBar(
    selected: CompactDestination,
    onSelect: (CompactDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        windowInsets = WindowInsets.navigationBars,
    ) {
        CompactDestination.entries.forEach { dest ->
            val isSelected = dest == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(dest) },
                icon = { Icon(if (isSelected) dest.selectedIcon else dest.unselectedIcon, dest.label) },
                label = { Text(dest.label, maxLines = 1) },
            )
        }
    }
}
