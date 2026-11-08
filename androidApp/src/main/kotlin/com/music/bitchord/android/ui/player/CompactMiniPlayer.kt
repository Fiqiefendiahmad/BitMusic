package com.music.bitchord.android.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import com.music.bitchord.android.playback.AndroidPlayerHost
import com.music.bitchord.android.ui.PlayerUiState
import com.music.bitchord.android.ui.components.TrackArtwork
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.ui.player.PlayerPlatform

@OptIn(ExperimentalMaterial3Api::class, dev.chrisbanes.haze.ExperimentalHazeApi::class)
@Composable
fun CompactMiniPlayer(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    val track = state.current ?: return
    val reduceDynamicBlurFlow = (PlayerPlatform.host as? AndroidPlayerHost)?.androidSettings?.reduceDynamicBlur
    val reduceDynamicBlur by if (reduceDynamicBlurFlow != null) {
        reduceDynamicBlurFlow.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(false) }
    }
    val progressColor = MaterialTheme.colorScheme.primary
    val accentColor by animateColorAsState(
        targetValue = progressColor,
        animationSpec = tween(durationMillis = 150),
        label = "mini-progress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        val baseAlpha = if (reduceDynamicBlur) 0.92f else 0.84f
        val baseModifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = baseAlpha))
            .clickable(onClick = onOpen)
            .semantics {
                role = Role.Button
                contentDescription = "Open now playing: ${track.title} by ${track.artist}"
            }

        val glassModifier = if (hazeState != null && !reduceDynamicBlur) {
            baseModifier.hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)),
                    blurRadius = 18.dp,
                ),
            )
        } else {
            baseModifier
        }

        Column(
            modifier = glassModifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.48f),
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TrackArtwork(
                    url = track.thumbnailUrl,
                    modifier = Modifier.size(44.dp),
                    fallbackIconSize = 20.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = accentColor,
                    )
                } else {
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            )
        }
    }
}
