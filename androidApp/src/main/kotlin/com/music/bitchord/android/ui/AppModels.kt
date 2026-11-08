package com.music.bitchord.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level destinations optimized for bottom navigation on small screens. */
enum class CompactDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Search("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    Library("library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
}

data class UiTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationLabel: String = "0:00",
    val source: String = "YouTube",
)

data class PlayerUiState(
    val current: UiTrack? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val queue: List<UiTrack> = emptyList(),
)

object SampleData {
    val tracks = listOf(
        UiTrack("1", "Midnight Drive", "Aurora Lane", "Neon Hours", "3:42", "YouTube"),
        UiTrack("2", "Small Screen", "BitMusic", "Android Redesign", "2:58", "Local"),
        UiTrack("3", "Signal Flow", "Mono Circuit", "Patch Notes", "4:11", "SoundCloud"),
        UiTrack("4", "Paper Planes", "Daylight Echo", "City Tape", "3:05", "Audius"),
        UiTrack("5", "Low Latency", "Cavacore", "Desktop Engine", "5:20", "YouTube"),
    )
}
