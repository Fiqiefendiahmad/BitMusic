package com.music.bitchord.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.bitchord.data.model.Song
import java.util.Locale

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
    Profile("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
}

/** Everything the compact player chrome draws, sourced from the real ExoPlayer. */
data class PlayerUiState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val queue: List<Song> = emptyList(),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
)

/** `m:ss`, with hours only when the track actually has them. */
internal fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0) return "0:00"
    val totalSeconds = milliseconds / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
