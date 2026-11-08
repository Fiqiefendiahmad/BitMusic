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
 */
@Composable
fun BitMusicAndroidApp(windowSizeClass: WindowSizeClass) {
    val isCompactWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    var destination by rememberSaveable { mutableStateOf(CompactDestination.Home) }
    var playerState by rememberSaveable(stateSaver = PlayerStateSaver) {
        mutableStateOf(PlayerUiState(current = SampleData.tracks.first(), queue = SampleData.tracks))
    }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    val onSelectTrack: (UiTrack) -> Unit = { track ->
        playerState = playerState.copy(
            current = track,
            isPlaying = true,
            progress = 0f,
            queue = listOf(track) + SampleData.tracks.filterNot { it.id == track.id },
        )
        showNowPlaying = true
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
                                onPlayPause = { playerState = playerState.copy(isPlaying = !playerState.isPlaying) },
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
                    tracks = SampleData.tracks,
                    onTrackClick = onSelectTrack,
                    compact = isCompactWidth,
                )
                CompactDestination.Search -> CompactSearchScreen(
                    modifier = contentModifier,
                    onTrackClick = onSelectTrack,
                    compact = isCompactWidth,
                )
                CompactDestination.Library -> CompactLibraryScreen(
                    modifier = contentModifier,
                    tracks = SampleData.tracks,
                    onTrackClick = onSelectTrack,
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
                onBack = { showNowPlaying = false },
                onPlayPause = { playerState = playerState.copy(isPlaying = !playerState.isPlaying) },
                onSeek = { progress -> playerState = playerState.copy(progress = progress.coerceIn(0f, 1f)) },
                onSelectTrack = onSelectTrack,
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

/** Minimal saver for prototype state; production should use ViewModel + parcelable/domain models. */
private val PlayerStateSaver = androidx.compose.runtime.saveable.Saver<PlayerUiState, String>(
    save = { state ->
        listOf(
            state.current?.id.orEmpty(),
            state.isPlaying.toString(),
            state.progress.toString(),
        ).joinToString("|")
    },
    restore = { saved ->
        val parts = saved.split("|")
        val current = SampleData.tracks.find { it.id == parts.getOrNull(0) }
        PlayerUiState(
            current = current,
            isPlaying = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: false,
            progress = parts.getOrNull(2)?.toFloatOrNull() ?: 0f,
            queue = SampleData.tracks,
        )
    },
)
