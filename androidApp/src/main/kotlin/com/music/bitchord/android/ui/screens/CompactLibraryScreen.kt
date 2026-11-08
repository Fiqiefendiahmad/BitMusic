package com.music.bitchord.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.data.model.Song

@Composable
fun CompactLibraryScreen(
    tracks: List<Song>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onTrackClick: (Song) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (compact) CompactDimens.ScreenPadding else 24.dp,
            vertical = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(CompactDimens.ItemSpacing),
    ) {
        item {
            Text(
                "Library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.statusBarsPadding().padding(bottom = 8.dp),
            )
        }
        when {
            isLoading -> item {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            error != null -> item {
                Column {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
            tracks.isEmpty() -> item {
                Text(
                    "Your library is empty. Liked songs and saved tracks appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                item {
                    Text(
                        "Liked songs & saved tracks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(tracks, key = { it.videoId }) { track ->
                    TrackRow(track = track, onClick = { onTrackClick(track) })
                }
            }
        }
    }
}
