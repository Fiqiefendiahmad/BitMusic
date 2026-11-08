package com.music.bitchord.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.data.model.Song

@Composable
fun CompactSearchScreen(
    query: String,
    results: List<Song>,
    isSearching: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onTrackClick: (Song) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(
                    horizontal = if (compact) CompactDimens.ScreenPadding else 24.dp,
                    vertical = 8.dp,
                ),
        ) {
            Text("Search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                placeholder = { Text("Songs, artists, albums") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = if (compact) CompactDimens.ScreenPadding else 24.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(CompactDimens.ItemSpacing),
        ) {
            when {
                isSearching -> item {
                    CircularProgressIndicator(Modifier.padding(24.dp))
                }
                error != null -> item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                query.isBlank() -> item {
                    Text(
                        "Search YouTube Music",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                results.isEmpty() -> item {
                    Text("No results", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> items(results, key = { it.videoId }) { track ->
                    TrackRow(track = track, onClick = { onTrackClick(track) })
                }
            }
        }
    }
}
