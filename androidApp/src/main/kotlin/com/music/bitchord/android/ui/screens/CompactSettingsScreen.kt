package com.music.bitchord.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.android.auth.AndroidYouTubeAuth
import com.music.bitchord.android.auth.YouTubeSignInActivity
import com.music.bitchord.android.playback.AndroidPlayerHost
import com.music.bitchord.android.playback.PlaybackHolder
import com.music.bitchord.android.ui.theme.CompactDimens
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.ui.player.PlayerPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-page settings (not an overlay). Mirrors the original desktop settings
 * groups with a compact, readable layout. Toggles write through to the same
 * SharedPreferences-backed [com.music.bitchord.android.playback.AndroidPlayerSettings]
 * the full player uses.
 */
@Composable
fun CompactSettingsScreen(
    themeMode: Int = -1, // -1 follow device, 0 light, 1 dark
    onThemeModeChange: (Int) -> Unit = {},
    onExportLogs: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val host = PlayerPlatform.host as? AndroidPlayerHost
    val settings = host?.androidSettings
    val outputFormat = host?.outputFormat?.collectAsState()?.value

    val pageBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface.copy(alpha = 1f)

    if (settings == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(pageBg)
                .statusBarsPadding()
                .padding(CompactDimens.ScreenPadding),
        ) {
            SettingsHeader(onBack)
            Spacer(Modifier.height(8.dp))
            Text("Settings are unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val quality by settings.audioQualityWifi.collectAsState()
    val fullBleed by settings.fullBleedArtwork.collectAsState()
    val animatedCanvas by settings.animatedCanvas.collectAsState()
    val reduceAnimation by settings.reduceAnimation.collectAsState()
    val reduceBlur by settings.reduceDynamicBlur.collectAsState()
    val hideSongStatus by settings.hideSongStatus.collectAsState()
    val queueHistory by settings.showQueueHistory.collectAsState()
    val nerdStats by settings.showNerdStats.collectAsState()
    val hideVolume by settings.hideVolumeBar.collectAsState()
    val hideStatus by settings.hideStatusBar.collectAsState()
    val syncedLyrics by settings.syncedLyrics.collectAsState()
    val lyricsBlur by settings.lyricsBlur.collectAsState()
    val canvasOverCellular by settings.canvasOverCellular.collectAsState()
    val prioritizeCanvas by settings.prioritizeSpotifyCanvas.collectAsState()
    val spotifyAutoHide by settings.spotifyCanvasAutoHide.collectAsState()
    val legacyMesh by settings.legacyMeshGradient.collectAsState()
    val cacheMb by settings.cacheSizeMb.collectAsState()
    val context = LocalContext.current
    val ytCookie by AndroidYouTubeAuth.cookie.collectAsStateWithLifecycle()
    val signedIn = !ytCookie.isNullOrBlank()
    var showCachePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var cacheUsedBytes by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(showCachePicker) {
        cacheUsedBytes = withContext(Dispatchers.IO) {
            runCatching { PlaybackHolder.cacheDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrNull()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = CompactDimens.ScreenPadding,
            end = CompactDimens.ScreenPadding,
            top = 4.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(CompactDimens.ItemSpacing),
    ) {
        item { SettingsHeader(onBack) }
        item {
            SectionCard("Account") {
                Text(
                    text = if (signedIn) "Signed in to YouTube Music" else "Browsing as guest",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                OutlinedButton(
                    onClick = {
                        if (signedIn) {
                            AndroidYouTubeAuth.signOut(context)
                        } else {
                            context.startActivity(Intent(context, YouTubeSignInActivity::class.java))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (signedIn) "Sign out" else "Sign in to YouTube Music")
                }
            }
        }
        item { SectionCard("Appearance") {
            val themeLabel = when (themeMode) {
                0 -> "Light"
                1 -> "Dark"
                else -> "Follow device setting"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemePicker = true }
                    .padding(vertical = 8.dp),
            ) {
                Text("Theme", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    themeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Follow device setting matches your Android light/dark appearance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } }

        item { SectionCard("Audio quality") {
            val highQuality = quality != AudioQuality.LOW
            SettingSwitch(
                "High quality audio",
                if (highQuality) "Best available stream" else "Data saver · smaller stream",
                highQuality,
            ) { enabled ->
                settings.setAudioQuality(if (enabled) AudioQuality.HIGH else AudioQuality.LOW)
            }
            if (outputFormat != null) {
                Text(
                    "Output: ${outputFormat.summary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } }

        item { SectionCard("Playback") {
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
        } }

        item { SectionCard("Battery & background playback") {
            val pm = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
            var isIgnoring by remember { mutableStateOf(pm?.isIgnoringBatteryOptimizations(context.packageName) == true) }

            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
                isIgnoring = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
                onPauseOrDispose { }
            }

            Text(
                text = if (isIgnoring) "Battery optimization: Unrestricted" else "Battery optimization: Optimized (may sleep in background)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isIgnoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isIgnoring) {
                    "BitMusic is allowed to keep playing continuously when screen is locked or idle."
                } else {
                    "On Samsung devices, battery optimization can pause audio when the screen is turned off. Set to Unrestricted to prevent Samsung from putting BitMusic to sleep."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val intent = if (!isIgnoring) {
                        Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    } else {
                        Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    }
                    runCatching { context.startActivity(intent) }.onFailure {
                        val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(fallback) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isIgnoring) "Open Battery Settings" else "Request Unrestricted Battery")
            }
        } }

        item { SectionCard("Canvas") {
            SettingSwitch("Canvas over cellular", "Allow animated canvases on metered data", canvasOverCellular) {
                settings.setCanvasOverCellular(it)
            }
            SettingSwitch("Prioritize Spotify canvas", "Prefer Spotify's clip when both exist", prioritizeCanvas) {
                settings.setPrioritizeSpotifyCanvas(it)
            }
            SettingSwitch("Auto-hide canvas", "Hide the canvas after a few seconds", spotifyAutoHide) {
                settings.setSpotifyCanvasAutoHide(it)
            }
            SettingSwitch("Legacy mesh gradient", "Use the older artwork gradient", legacyMesh) {
                settings.setLegacyMeshGradient(it)
            }
        } }

        item { SectionCard("Diagnostics") {
            Surface(
                onClick = onExportLogs,
                shape = RoundedCornerShape(CompactDimens.CornerRadius),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Export logs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Save crash and runtime logs to Downloads", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } }

        item {
            SectionCard("Storage & cache") {
                val used = cacheUsedBytes
                Text(
                    text = "Music cache speeds up opening and replaying songs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = { showCachePicker = true },
                    shape = RoundedCornerShape(CompactDimens.CornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Cache size limit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "${cacheMb} MB" + (used?.let { " · ${formatBytes(it)} used" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        PlaybackHolder.clearCache(context)
                        cacheUsedBytes = 0L
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Clear music cache")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Cache size changes apply after restarting playback/app if media is active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { SectionCard("Links") {
            LinkRow("This app (Android fork)", "github.com/Fiqiefendiahmad/BitMusic") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Fiqiefendiahmad/BitMusic")))
            }
            LinkRow("Main BitMusic repository", "github.com/1999AZZAR/BitMusic") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/1999AZZAR/BitMusic")))
            }
            LinkRow("License", "View the project license") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Fiqiefendiahmad/BitMusic/blob/android/small-screen-redesign/LICENSE")))
            }
        } }

        item { SectionCard("About & legal") {
            Text(
                "BitMusic is an open-source music client. This Android build is a small-screen redesign fork that uses the shared BitMusic data and stream configuration where available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Text(
                "Terms & conditions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Use this app responsibly and in accordance with applicable laws and the terms of any services you access. You are responsible for your account, network usage, and the content you play. The app is provided as-is, without warranties; see the project license for details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            Text(
                "Privacy",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Playback and search requests go to the services configured by BitMusic/YouTube Music. If you sign in, your session cookie is stored locally on this device to enable account features. Diagnostic export is user-initiated and saved to your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } }

        item {
            Text(
                "BitMusic ${com.music.bitchord.android.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }

    if (showThemePicker) {
        val options = listOf(
            -1 to "Follow device setting",
            0 to "Light",
            1 to "Dark",
        )
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (value, label) ->
                        val selected = value == themeMode
                        Surface(
                            onClick = {
                                onThemeModeChange(value)
                                showThemePicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Text(
                                    label,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) { Text("Close") }
            },
        )
    }

    if (showCachePicker) {
        val options = listOf(128, 256, 512, 1024, 2048)
        AlertDialog(
            onDismissRequest = { showCachePicker = false },
            title = { Text("Music cache size") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { mb ->
                        val selected = mb == cacheMb
                        Surface(
                            onClick = {
                                settings.setCacheSizeMb(mb)
                                showCachePicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Text(
                                    "$mb MB",
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCachePicker = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(CompactDimens.CornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 1f),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            content()
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
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    bytes < 1024L * 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

@Composable
private fun LinkRow(title: String, detail: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
