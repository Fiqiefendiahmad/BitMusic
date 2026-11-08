package com.music.bitchord.android.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.android.ui.PlayerUiState
import com.music.bitchord.android.ui.components.TrackArtwork
import com.music.bitchord.android.ui.formatTime
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.data.model.Song

/**
 * Portrait-first now playing redesign for small Android screens:
 * - Large, centered artwork constrained to viewport width
 * - Primary controls clustered in the lower thumb zone
 * - Queue preview below controls (no side panes)
 * - Safe-area padding for status/navigation bars
 */
@Composable
fun CompactNowPlayingScreen(
    state: PlayerUiState,
    error: String?,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSelectTrack: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.current ?: return
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val shownProgress = dragProgress ?: state.progress

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = CompactDimens.ScreenPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(CompactDimens.MinTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close now playing")
                }
                Text(
                    "Now Playing",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(CompactDimens.MinTouchTarget))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                TrackArtwork(
                    url = track.thumbnailUrl,
                    modifier = Modifier
                        .widthIn(max = CompactDimens.ArtworkNowPlaying)
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f),
                    fallbackIconSize = 72.dp,
                )
            }

            Spacer(Modifier.height(CompactDimens.SectionSpacing))
            Text(
                track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(16.dp))
            Slider(
                value = shownProgress,
                onValueChange = { dragProgress = it },
                onValueChangeFinished = {
                    dragProgress?.let(onSeek)
                    dragProgress = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatTime(
                        if (state.durationMs > 0) (state.durationMs * shownProgress).toLong() else 0L,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }

            if (error != null) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(30.dp))
                }
                FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(72.dp)) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(30.dp))
                }
            }

            Spacer(Modifier.height(CompactDimens.SectionSpacing))
            Text("Up next", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(state.queue.take(8), key = { _, song -> song.videoId }) { _, item ->
                    QueueRow(
                        item = item,
                        active = item.videoId == track.videoId,
                        onClick = { onSelectTrack(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(item: Song, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(CompactDimens.CornerRadiusSmall),
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TrackArtwork(
                url = item.thumbnailUrl,
                modifier = Modifier.size(40.dp),
                fallbackIconSize = 16.dp,
            )
            Column(Modifier.weight(1f)) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(
                    item.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.durationText?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
