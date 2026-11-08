package com.music.bitchord.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.music.bitchord.android.ui.player.CompactNowPlayingScreen
import com.music.bitchord.android.ui.screens.CompactHomeScreen
import com.music.bitchord.android.ui.screens.CompactLibraryScreen
import com.music.bitchord.android.ui.screens.CompactSearchScreen
import com.music.bitchord.android.ui.theme.CompactDimens

/**
 * Android shell redesigned for small screens:
 * - Bottom navigation for thumb reach (compact width)
 * - Persistent mini-player above nav
 * - Full-screen now playing overlay for portrait phones
 * - Single-column, touch-friendly content layouts
 *
 * All tracks and playback come from the shared BitMusic data layer.
 */
@Composable
fun BitMusicAndroidApp(
    windowSizeClass: WindowSizeClass,
    viewModel: BitMusicViewModel = viewModel(),
) {
    val isCompactWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    var destination by rememberSaveable { mutableStateOf(CompactDestination.Home) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    val playerState = viewModel.playerState

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
                CompactDestination.Home -> CompactHomeScreen(
                    modifier = contentModifier,
                    tracks = viewModel.homeTracks,
                    isLoading = viewModel.isLoadingHome,
                    error = viewModel.homeError,
                    onRetry = viewModel::refreshHome,
                    onTrackClick = { song -> viewModel.play(song, viewModel.homeTracks); showNowPlaying = true },
                    compact = isCompactWidth,
                )
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
            CompactNowPlayingScreen(
                state = playerState,
                error = viewModel.playbackError,
                onBack = { showNowPlaying = false },
                onPlayPause = viewModel::togglePlayPause,
                onSeek = viewModel::seekTo,
                onNext = { viewModel.skipNext() },
                onPrevious = viewModel::skipPrevious,
                onSelectTrack = { song -> viewModel.play(song, viewModel.queue) },
            )
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
