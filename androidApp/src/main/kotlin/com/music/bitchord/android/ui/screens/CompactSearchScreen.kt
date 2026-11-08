package com.music.bitchord.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.android.ui.components.TrackArtwork
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song

@Composable
fun CompactSearchScreen(
    modifier: Modifier = Modifier,
    query: String,
    results: List<SearchResult>,
    suggestions: List<Song> = emptyList(),
    filter: SearchFilter,
    isSearching: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlaySuggestion: (Song, List<Song>) -> Unit = { _, _ -> },
    onOpenBrowse: (title: String, browseId: String, type: String) -> Unit,
    compact: Boolean,
) {
    val filters = listOf(
        SearchFilter.SONGS to "Songs",
        SearchFilter.ALBUMS to "Albums",
        SearchFilter.ARTISTS to "Artists",
        SearchFilter.PLAYLISTS to "Playlists",
        SearchFilter.VIDEOS to "Videos",
    )
    val songResults = results.mapNotNull { (it as? SearchResult.Track)?.song ?: (it as? SearchResult.TopTrack)?.song }

    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            horizontal = CompactDimens.ScreenPadding,
            vertical = 16.dp,
        ).let { PaddingValues(start = it.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr), top = 16.dp, end = it.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr), bottom = 120.dp) },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Search",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                label = { Text("Songs, artists, albums") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filters.forEach { (value, label) ->
                    FilterChip(
                        selected = filter == value,
                        onClick = { onFilterChange(value) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        when {
            query.isBlank() -> {
                if (suggestions.isEmpty()) {
                    item { SearchHint("Search songs, albums, artists, playlists, or videos.") }
                } else {
                    item {
                        Text(
                            "Most played",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    items(suggestions, key = { "s:${it.videoId}" }) { song ->
                        TrackResultRow(song) { onPlaySuggestion(song, suggestions) }
                    }
                }
            }
            isSearching -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }
            }
            error != null -> item { SearchHint(error) }
            results.isEmpty() -> item { SearchHint("No results for “$query”.") }
            else -> items(results, key = { resultKey(it) }) { result ->
                when (result) {
                    is SearchResult.Track -> TrackResultRow(result.song) {
                        onPlaySong(result.song, songResults.ifEmpty { listOf(result.song) })
                    }
                    is SearchResult.TopTrack -> TrackResultRow(result.song) {
                        onPlaySong(result.song, songResults.ifEmpty { listOf(result.song) })
                    }
                    is SearchResult.Browse -> {
                        val item = result.item
                        val typeLabel = item.type.name.lowercase().replaceFirstChar { it.titlecase() }
                        BrowseResultRow(
                            title = item.title,
                            subtitle = listOfNotNull(typeLabel.takeIf { it.isNotBlank() }, item.subtitle.takeIf { it.isNotBlank() }).joinToString(" • "),
                            thumbnailUrl = item.thumbnailUrl,
                            icon = browseIcon(typeLabel),
                            onClick = { onOpenBrowse(item.title, item.browseId, typeLabel) },
                            enabled = true,
                        )
                    }
                }
            }
        }
    }
}

private fun resultKey(result: SearchResult): String = when (result) {
    is SearchResult.Track -> "t:${result.song.videoId}:${result.song.title}"
    is SearchResult.TopTrack -> "tt:${result.song.videoId}"
    is SearchResult.Browse -> "b:${result.item.browseId}:${result.item.title}:${result.item.type.name}"
}

private fun browseIcon(type: String): ImageVector = when {
    type.contains("artist", true) -> Icons.Outlined.Person
    type.contains("album", true) -> Icons.Outlined.Album
    type.contains("playlist", true) -> Icons.Outlined.PlaylistPlay
    type.contains("video", true) -> Icons.Outlined.VideoLibrary
    else -> Icons.Outlined.MusicNote
}

@Composable
private fun SearchHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun TrackResultRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(url = song.thumbnailUrl, modifier = Modifier.size(CompactDimens.ArtworkSmall))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(song.artist.takeIf { it.isNotBlank() }, song.durationText?.takeIf { it.isNotBlank() }).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BrowseResultRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(CompactDimens.ArtworkSmall)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnailUrl != null) {
                TrackArtwork(url = thumbnailUrl, modifier = Modifier.size(CompactDimens.ArtworkSmall))
            } else {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!enabled) {
            Text("Unavailable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
