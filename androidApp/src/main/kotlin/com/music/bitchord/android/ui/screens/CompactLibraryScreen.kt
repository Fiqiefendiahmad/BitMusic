package com.music.bitchord.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.android.ui.components.TrackArtwork
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.data.model.Song
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalHazeApi::class)
@Composable
fun CompactLibraryScreen(
    tracks: List<Song>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onTrackClick: (Song) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val horizontal = if (compact) CompactDimens.ScreenPadding else 24.dp
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LibraryHeader(
                count = tracks.size,
                onRetry = onRetry,
                hazeState = hazeState,
                modifier = Modifier.statusBarsPadding(),
            )
        }
        when {
            isLoading -> item { LoadingGlassCard(hazeState) }
            error != null -> item { ErrorGlassCard(error, onRetry, hazeState) }
            tracks.isEmpty() -> item { EmptyGlassCard(hazeState) }
            else -> {
                itemsIndexed(tracks, key = { _, s -> s.videoId }) { index, track ->
                    LibraryTrackCard(
                        index = index + 1,
                        track = track,
                        onClick = { onTrackClick(track) },
                        hazeState = hazeState,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun LibraryHeader(count: Int, onRetry: () -> Unit, hazeState: HazeState?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    val base = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    var boxMod = Modifier.fillMaxWidth().clip(shape)
    if (hazeState != null) {
        boxMod = boxMod.hazeEffect(
            hazeState,
            style = HazeStyle(
                backgroundColor = MaterialTheme.colorScheme.surface,
                tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                blurRadius = 22.dp,
            ),
        )
    }
    Box(
        modifier = modifier.then(boxMod)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        base,
                    ),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), shape)
            .padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (count > 0) "$count saved tracks" else "Liked songs & saved tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh library", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun LibraryTrackCard(index: Int, track: Song, onClick: () -> Unit, hazeState: HazeState?) {
    val shape = RoundedCornerShape(20.dp)
    var cardMod = Modifier.fillMaxWidth().clip(shape)
    if (hazeState != null) {
        cardMod = cardMod.hazeEffect(
            hazeState,
            style = HazeStyle(
                backgroundColor = MaterialTheme.colorScheme.surface,
                tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)),
                blurRadius = 14.dp,
            ),
        )
    }
    Surface(
        onClick = onClick,
        modifier = cardMod.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = index.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 10.dp),
            )
            TrackArtwork(url = track.thumbnailUrl, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    listOf(track.artist.takeIf { it.isNotBlank() }, track.durationText).filterNotNull().joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun LoadingGlassCard(hazeState: HazeState?) = StateGlassCard(hazeState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun ErrorGlassCard(error: String, onRetry: () -> Unit, hazeState: HazeState?) = StateGlassCard(hazeState) {
    Column {
        Text(error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun EmptyGlassCard(hazeState: HazeState?) = StateGlassCard(hazeState) {
    Text(
        "Your library is empty. Sign in and like songs on YouTube Music, or save tracks, to see them here.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun StateGlassCard(hazeState: HazeState?, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    var mod = Modifier.fillMaxWidth().clip(shape)
    if (hazeState != null) {
        mod = mod.hazeEffect(
            hazeState,
            style = HazeStyle(
                backgroundColor = MaterialTheme.colorScheme.surface,
                tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                blurRadius = 16.dp,
            ),
        )
    }
    Box(
        mod.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), shape)
            .padding(18.dp),
    ) { content() }
}
