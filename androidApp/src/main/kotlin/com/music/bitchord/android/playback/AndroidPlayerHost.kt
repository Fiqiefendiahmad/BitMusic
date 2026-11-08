package com.music.bitchord.android.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.music.bitchord.data.canvas.CanvasArtwork
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.lyrics.LyricsTranslation
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.data.settings.LastPlayerScreen
import com.music.bitchord.data.settings.MixBlend
import com.music.bitchord.data.settings.SmartAnalysis
import com.music.bitchord.data.settings.TransitionWindow
import com.music.bitchord.ui.player.AudioOutputDevice
import com.music.bitchord.ui.player.AudioOutputKind
import com.music.bitchord.ui.player.CanvasVideoSpec
import com.music.bitchord.ui.player.LyricsRomanizationResult
import com.music.bitchord.ui.player.LyricsTranslationResult
import com.music.bitchord.ui.player.MAX_LYRICS_OFFSET_MS
import com.music.bitchord.ui.player.MIN_LYRICS_OFFSET_MS
import com.music.bitchord.ui.player.OutputFormatUi
import com.music.bitchord.ui.player.PartyUi
import com.music.bitchord.ui.player.PlayerHost
import com.music.bitchord.ui.player.PlayerSettingsSource
import com.music.bitchord.ui.player.SystemVolume
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * The Android answers to the shared player's [PlayerHost].
 *
 * The player itself is the same Compose tree the desktop draws — its transport,
 * queue, lyric controls and output drawer. This object supplies what only the
 * device can: the system volume, its audio routes, the vibrator, and settings
 * kept in SharedPreferences.
 */
internal class AndroidPlayerHost(context: Context) : PlayerHost {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override val settings: PlayerSettingsSource = AndroidPlayerSettings(preferences)

    /** The same settings, typed so the settings sheet can write the toggles back. */
    val androidSettings: AndroidPlayerSettings
        get() = settings as AndroidPlayerSettings

    override fun cachedCanvas(song: Song): CanvasArtwork? = null

    override suspend fun canvasFor(song: Song): CanvasArtwork? = null

    @Composable
    override fun CanvasVideo(spec: CanvasVideoSpec, modifier: Modifier) = Unit

    @Composable
    override fun rememberRemoteArtworkUrl(song: Song?): String? = song?.thumbnailUrl

    override val volume: SystemVolume = object : SystemVolume {
        private val _level = MutableStateFlow(readLevel())

        override val level: StateFlow<Float> = _level.asStateFlow()

        override fun set(level: Float) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val index = (level.coerceIn(0f, 1f) * max).roundToInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
            _level.value = readLevel()
        }

        override fun refresh() {
            _level.value = readLevel()
        }

        private fun readLevel(): Float {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        }
    }

    private val selectedOutput = MutableStateFlow(0)
    private var listedDevices: List<AudioDeviceInfo> = emptyList()

    @Composable
    override fun rememberAudioOutputs(): List<AudioOutputDevice> {
        val context = LocalContext.current
        val manager = remember(context) { context.getSystemService(AudioManager::class.java) }
        var revision by remember { mutableIntStateOf(0) }
        DisposableEffect(manager) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                    revision++
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                    revision++
                }
            }
            manager.registerAudioDeviceCallback(callback, null)
            onDispose { manager.unregisterAudioDeviceCallback(callback) }
        }
        val selected by selectedOutput.collectAsState()
        return remember(revision, selected) {
            listedDevices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            listedDevices.mapIndexed { index, device ->
                AudioOutputDevice(
                    id = index,
                    name = device.productName.toString().ifBlank { kindOf(device.type).name.lowercase() },
                    kind = kindOf(device.type),
                    isActive = index == selected,
                )
            }
        }
    }

    @Composable
    override fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit = onOpen

    override fun selectAudioOutput(id: Int?) {
        if (id == null || id !in listedDevices.indices) return
        selectedOutput.value = id
        // Android media routing is owned by the system; the drawer marks the
        // chosen route and the system picker/volume panel does the switching.
        Toast.makeText(appContext, "Use the system output picker to switch route", Toast.LENGTH_SHORT)
            .show()
    }

    override val outputFormat: StateFlow<OutputFormatUi> = MutableStateFlow(
        run {
            val rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
                ?.toIntOrNull()
            OutputFormatUi(
                summary = rate?.let { "${it / 1000f} kHz" } ?: "Android audio",
                carriesHiRes = (rate ?: 48_000) > 48_000,
            )
        },
    ).asStateFlow()

    override val party: StateFlow<PartyUi> = MutableStateFlow(PartyUi()).asStateFlow()

    override suspend fun translateLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsTranslationResult =
        when (val result = LyricsTranslation.translate(trackId, lines, targetLanguageTag)) {
            is LyricsTranslation.Result.Translated -> LyricsTranslationResult.Translated(result.lines)
            is LyricsTranslation.Result.SameLanguage -> LyricsTranslationResult.SameLanguage(result.language)
            LyricsTranslation.Result.Unavailable -> LyricsTranslationResult.Unavailable
        }

    override suspend fun romanizeLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsRomanizationResult =
        when (val result = LyricsTranslation.romanize(trackId, lines, targetLanguageTag)) {
            is LyricsTranslation.RomanizationResult.Romanized -> LyricsRomanizationResult.Romanized(result.lines)
            LyricsTranslation.RomanizationResult.AlreadyRomanized -> LyricsRomanizationResult.AlreadyRomanized
            LyricsTranslation.RomanizationResult.Unavailable -> LyricsRomanizationResult.Unavailable
        }

    override fun showMessage(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    @Composable
    override fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Audio pipeline") },
            text = {
                Column {
                    Text("ExoPlayer → Android audio")
                    Text(outputFormat.value.summary)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            },
        )
    }

    private fun kindOf(type: Int): AudioOutputKind = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        -> AudioOutputKind.PHONE
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        -> AudioOutputKind.WIRED
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        -> AudioOutputKind.USB
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        -> AudioOutputKind.BLUETOOTH
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        AudioDeviceInfo.TYPE_HDMI_EARC,
        -> AudioOutputKind.HDMI
        else -> AudioOutputKind.OTHER
    }

    private companion object {
        const val PREFS = "bitchord"
    }
}

internal class AndroidPlayerSettings(
    private val prefs: SharedPreferences,
) : PlayerSettingsSource {
    private fun savedQuality(): AudioQuality =
        runCatching { AudioQuality.valueOf(prefs.getString(KEY_AUDIO_QUALITY, "HIGH") ?: "HIGH") }
            .getOrDefault(AudioQuality.HIGH)

    override val animatedCanvas = MutableStateFlow(prefs.getBoolean("animated_canvas", true))
    override val audioQualityCellular = MutableStateFlow(savedQuality())
    override val audioQualityWifi = MutableStateFlow(savedQuality())
    override val canvasOverCellular = MutableStateFlow(prefs.getBoolean("canvas_over_cellular", false))
    override val fullBleedArtwork = MutableStateFlow(prefs.getBoolean("full_bleed_artwork", true))
    override val hideSongStatus = MutableStateFlow(prefs.getBoolean("hide_song_status", false))
    override val showQueueHistory = MutableStateFlow(prefs.getBoolean("show_queue_history", false))
    override val hideVolumeBar = MutableStateFlow(prefs.getBoolean("hide_volume_bar", false))
    override val hideStatusBar = MutableStateFlow(prefs.getBoolean("hide_status_bar", false))
    override val lastPlayerScreen = MutableStateFlow(
        runCatching {
            LastPlayerScreen.valueOf(prefs.getString(KEY_LAST_SCREEN, "MAIN") ?: "MAIN")
        }.getOrDefault(LastPlayerScreen.MAIN),
    )
    override val legacyMeshGradient = MutableStateFlow(prefs.getBoolean("legacy_mesh", false))
    override val lyricsBlur = MutableStateFlow(prefs.getBoolean("lyrics_blur", true))
    override val lyricsOffsetMs = MutableStateFlow(
        prefs.getInt(KEY_LYRICS_OFFSET, 0).coerceIn(MIN_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS),
    )
    override val lyricsSourceOrder = MutableStateFlow<List<LyricsSource>>(LyricsSource.offered)
    override val meteredConnection = MutableStateFlow<Boolean?>(null)
    override val preferUsbDac = MutableStateFlow(false)
    override val prioritizeSpotifyCanvas = MutableStateFlow(prefs.getBoolean("prioritize_canvas", false))
    override val reduceAnimation = MutableStateFlow(prefs.getBoolean("reduce_animation", false))
    override val reduceDynamicBlur = MutableStateFlow(prefs.getBoolean("reduce_dynamic_blur", false))
    override val showNerdStats = MutableStateFlow(prefs.getBoolean("show_nerd_stats", false))
    override val smartAnalysis = MutableStateFlow(SmartAnalysis())
    override val smartFadeEnabled = MutableStateFlow(false)
    override val smartMixInProgress = MutableStateFlow(false)
    override val smartMixBlend = MutableStateFlow<MixBlend?>(null)
    override val smartTransitionWindow = MutableStateFlow<TransitionWindow?>(null)
    override val spotifyCanvasAutoHide = MutableStateFlow(prefs.getBoolean("spotify_canvas_auto_hide", true))
    override val syncedLyrics = MutableStateFlow(prefs.getBoolean("synced_lyrics", true))
    override val translationLanguage = MutableStateFlow("")
    override val versionAlignmentInProgress = MutableStateFlow(false)

    override fun setLastPlayerScreen(value: LastPlayerScreen) {
        lastPlayerScreen.value = value
        prefs.edit().putString(KEY_LAST_SCREEN, value.name).apply()
    }

    override fun setLyricsOffsetMs(value: Int) {
        val clamped = value.coerceIn(MIN_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS)
        lyricsOffsetMs.value = clamped
        prefs.edit().putInt(KEY_LYRICS_OFFSET, clamped).apply()
    }

    fun setAnimatedCanvas(value: Boolean) = saveBoolean(animatedCanvas, "animated_canvas", value)
    fun setCanvasOverCellular(value: Boolean) = saveBoolean(canvasOverCellular, "canvas_over_cellular", value)
    fun setFullBleedArtwork(value: Boolean) = saveBoolean(fullBleedArtwork, "full_bleed_artwork", value)
    fun setHideSongStatus(value: Boolean) = saveBoolean(hideSongStatus, "hide_song_status", value)
    fun setShowQueueHistory(value: Boolean) = saveBoolean(showQueueHistory, "show_queue_history", value)
    fun setHideVolumeBar(value: Boolean) = saveBoolean(hideVolumeBar, "hide_volume_bar", value)
    fun setHideStatusBar(value: Boolean) = saveBoolean(hideStatusBar, "hide_status_bar", value)
    fun setLegacyMeshGradient(value: Boolean) = saveBoolean(legacyMeshGradient, "legacy_mesh", value)
    fun setLyricsBlur(value: Boolean) = saveBoolean(lyricsBlur, "lyrics_blur", value)
    fun setPrioritizeSpotifyCanvas(value: Boolean) =
        saveBoolean(prioritizeSpotifyCanvas, "prioritize_canvas", value)
    fun setReduceAnimation(value: Boolean) = saveBoolean(reduceAnimation, "reduce_animation", value)
    fun setReduceDynamicBlur(value: Boolean) = saveBoolean(reduceDynamicBlur, "reduce_dynamic_blur", value)
    fun setShowNerdStats(value: Boolean) = saveBoolean(showNerdStats, "show_nerd_stats", value)
    fun setSpotifyCanvasAutoHide(value: Boolean) =
        saveBoolean(spotifyCanvasAutoHide, "spotify_canvas_auto_hide", value)
    fun setSyncedLyrics(value: Boolean) = saveBoolean(syncedLyrics, "synced_lyrics", value)

    fun setAudioQuality(value: AudioQuality) {
        audioQualityWifi.value = value
        audioQualityCellular.value = value
        prefs.edit()
            .putString(KEY_AUDIO_QUALITY, value.name)
            .putInt(KEY_STREAM_MAX_KBPS, value.maxKbps)
            .apply()
        StreamResolver.maxKbps = { value.maxKbps }
    }

    private fun saveBoolean(
        flow: MutableStateFlow<Boolean>,
        key: String,
        value: Boolean,
    ) {
        flow.value = value
        prefs.edit().putBoolean(key, value).apply()
    }

    private companion object {
        const val KEY_LAST_SCREEN = "last_player_screen"
        const val KEY_LYRICS_OFFSET = "lyrics_offset_ms"
        const val KEY_AUDIO_QUALITY = "audio_quality"
        const val KEY_STREAM_MAX_KBPS = "stream_max_kbps"
    }
}
