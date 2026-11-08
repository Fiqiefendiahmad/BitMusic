package com.music.bitchord.android.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import java.io.File

/**
 * Process-wide playback owner so the [PlaybackService] MediaSession and the
 * in-app UI share the same ExoPlayer instance (notification/lock screen +
 * Compose controls stay in sync).
 */
object PlaybackHolder {
    private const val PREFS = "bitchord"
    private const val KEY_CACHE_MB = "player_cache_mb"
    private const val DEFAULT_CACHE_MB = 512
    private const val MIN_CACHE_MB = 64
    private const val MAX_CACHE_MB = 4096

    @Volatile private var _player: ExoPlayer? = null
    @Volatile private var _session: MediaSession? = null
    @Volatile private var _httpFactory: DefaultHttpDataSource.Factory? = null
    @Volatile private var _cache: SimpleCache? = null
    @Volatile private var _onSkipNext: (() -> Unit)? = null
    @Volatile private var _onSkipPrevious: (() -> Unit)? = null

    val player: ExoPlayer get() = checkNotNull(_player) { "PlaybackHolder not initialized" }
    val session: MediaSession get() = checkNotNull(_session) { "PlaybackHolder not initialized" }

    /** HTTP factory for request headers (StreamResolver headers must win). */
    val dataSourceFactory: DefaultHttpDataSource.Factory
        get() = checkNotNull(_httpFactory) { "PlaybackHolder not initialized" }

    fun cacheDir(appContext: Context): File = File(appContext.cacheDir, "media_cache")

    fun cacheSizeMb(appContext: Context): Int =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CACHE_MB, DEFAULT_CACHE_MB)
            .coerceIn(MIN_CACHE_MB, MAX_CACHE_MB)

    fun cacheBytes(appContext: Context): Long = cacheSizeMb(appContext).toLong() * 1024L * 1024L

    fun clearCache(appContext: Context) {
        synchronized(this) {
            runCatching { _cache?.release() }
            _cache = null
            // If player is active, cache will be recreated on next app restart/process.
            cacheDir(appContext).deleteRecursively()
        }
    }

    fun setTransportHandlers(onSkipNext: (() -> Unit)?, onSkipPrevious: (() -> Unit)?) {
        _onSkipNext = onSkipNext
        _onSkipPrevious = onSkipPrevious
    }

    fun ensure(appContext: Context) {
        if (_player != null) return
        synchronized(this) {
            if (_player != null) return
            val http = DefaultHttpDataSource.Factory()
                .setUserAgent(null)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(30_000)

            val upstream = DefaultDataSource.Factory(appContext, http)
            val mediaSourceFactory = runCatching {
                val dir = cacheDir(appContext).apply { mkdirs() }
                val evictor = LeastRecentlyUsedCacheEvictor(cacheBytes(appContext))
                val cache = SimpleCache(dir, evictor, StandaloneDatabaseProvider(appContext))
                _cache = cache
                val cacheDs = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstream)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                DefaultMediaSourceFactory(appContext).setDataSourceFactory(cacheDs)
            }.getOrElse {
                // Fallback: no disk cache if cache init fails.
                DefaultMediaSourceFactory(upstream)
            }

            val exo = ExoPlayer.Builder(appContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    /* handleAudioFocus = */ true,
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()
            val forwarded = object : ForwardingPlayer(exo) {
                override fun isCommandAvailable(command: Int): Boolean =
                    command == COMMAND_SEEK_TO_NEXT ||
                        command == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                        command == COMMAND_SEEK_TO_PREVIOUS ||
                        command == COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                        super.isCommandAvailable(command)

                override fun getAvailableCommands(): Player.Commands =
                    super.getAvailableCommands().buildUpon()
                        .add(COMMAND_SEEK_TO_NEXT)
                        .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(COMMAND_SEEK_TO_PREVIOUS)
                        .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build()

                // App manages the queue (single-item player). Make transport actions visible.
                override fun hasNextMediaItem(): Boolean = true
                override fun hasPreviousMediaItem(): Boolean = true
                override fun isCurrentMediaItemSeekable(): Boolean = true
            }
            val session = MediaSession.Builder(appContext, forwarded)
                .setCallback(TransportCallback())
                .build()
            _httpFactory = http
            _player = exo
            _session = session
        }
    }

    fun release() {
        synchronized(this) {
            _session?.release()
            _player?.release()
            runCatching { _cache?.release() }
            _session = null
            _player = null
            _httpFactory = null
            _cache = null
            _onSkipNext = null
            _onSkipPrevious = null
        }
    }

    private class TransportCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val playerCommands = Player.Commands.Builder()
                .addAll(session.player.availableCommands)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                -> {
                    _onSkipNext?.invoke()
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                -> {
                    _onSkipPrevious?.invoke()
                    return SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }
}
