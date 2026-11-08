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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.bitchord.android.ui.SampleData
import com.music.bitchord.android.ui.UiTrack
import com.music.bitchord.android.ui.theme.CompactDimens

@Composable
fun CompactSearchScreen(
    onTrackClick: (UiTrack) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = if (query.isBlank()) SampleData.tracks else SampleData.tracks.filter {
        it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
    }

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
                onValueChange = { query = it },
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
            if (results.isEmpty()) {
                item { Text("No results", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(results, key = { it.id }) { track ->
                TrackRow(track = track, onClick = { onTrackClick(track) })
            }
        }
    }
}
