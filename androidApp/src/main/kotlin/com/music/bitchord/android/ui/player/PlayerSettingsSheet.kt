package com.music.bitchord.android.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.bitchord.android.BuildConfig
import com.music.bitchord.android.playback.AndroidPlayerHost
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.ui.player.PlayerDrawer
import com.music.bitchord.ui.player.PlayerPlatform
import dev.chrisbanes.haze.HazeState

/**
 * The playback settings that sit behind the full player's menu, in the same
 * drawer shell the player's own lyric and output sheets use.
 */
@Composable
fun PlayerSettingsSheet(onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val settings = (PlayerPlatform.host as? AndroidPlayerHost)?.androidSettings
    val haze = remember { HazeState() }
    PlayerDrawer(hazeState = haze, title = "Settings", onDismiss = onDismiss) {
        if (settings == null) {
            Text("Settings are unavailable.", style = MaterialTheme.typography.bodyMedium)
            return@PlayerDrawer
        }

        val quality by settings.audioQualityWifi.collectAsState()
        val fullBleed by settings.fullBleedArtwork.collectAsState()
        val hideVolume by settings.hideVolumeBar.collectAsState()
        val hideStatus by settings.hideStatusBar.collectAsState()
        val syncedLyrics by settings.syncedLyrics.collectAsState()
        val lyricsBlur by settings.lyricsBlur.collectAsState()
        val hideSongStatus by settings.hideSongStatus.collectAsState()
        val queueHistory by settings.showQueueHistory.collectAsState()
        val nerdStats by settings.showNerdStats.collectAsState()
        val reduceAnimation by settings.reduceAnimation.collectAsState()
        val reduceBlur by settings.reduceDynamicBlur.collectAsState()
        val animatedCanvas by settings.animatedCanvas.collectAsState()

        SectionLabel("Audio quality")
        QualityRow(
            title = "Data saver",
            detail = "~64 kbps · smallest stream",
            selected = quality == AudioQuality.LOW,
            onClick = { settings.setAudioQuality(AudioQuality.LOW) },
        )
        QualityRow(
            title = "Best available",
            detail = "Highest progressive stream · ~256 kbps",
            selected = quality != AudioQuality.LOW,
            onClick = { settings.setAudioQuality(AudioQuality.HIGH) },
        )

        Divider()
        SectionLabel("Player")
        SettingSwitch("Full-bleed artwork", "Let the sleeve run under the status bar", fullBleed) {
            settings.setFullBleedArtwork(it)
        }
        SettingSwitch("Animated canvas", "Loop the clip some releases publish", animatedCanvas) {
            settings.setAnimatedCanvas(it)
        }
        SettingSwitch("Synced lyrics", "Follow the words to the playhead", syncedLyrics) {
            settings.setSyncedLyrics(it)
        }
        SettingSwitch("Blur behind lyrics", "Frost the player under the words", lyricsBlur) {
            settings.setLyricsBlur(it)
        }
        SettingSwitch("Hide volume bar", "Keep the transport to its buttons", hideVolume) {
            settings.setHideVolumeBar(it)
        }
        SettingSwitch("Hide status bar", "Use the whole screen for the player", hideStatus) {
            settings.setHideStatusBar(it)
        }
        SettingSwitch("Hide song status", "Drop the playback-origin caption", hideSongStatus) {
            settings.setHideSongStatus(it)
        }
        SettingSwitch("Queue history", "Show what has already played", queueHistory) {
            settings.setShowQueueHistory(it)
        }
        SettingSwitch("Nerd stats", "Show codec, bitrate and loudness", nerdStats) {
            settings.setShowNerdStats(it)
        }
        SettingSwitch("Reduce animation", "Calmer transitions throughout", reduceAnimation) {
            settings.setReduceAnimation(it)
        }
        SettingSwitch("Reduce dynamic blur", "Trade the haze for flat surfaces", reduceBlur) {
            settings.setReduceDynamicBlur(it)
        }

        Divider()
        Text(
            "BitMusic ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun QualityRow(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Divider() {
    Spacer(Modifier.height(10.dp))
    HorizontalDivider()
    Spacer(Modifier.height(10.dp))
}
