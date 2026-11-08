package com.music.bitchord.android.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.music.bitchord.data.model.Song

/**
 * Controls the shared [PlaybackHolder] ExoPlayer/MediaSession. Metadata is
 * published to the session for notification and lock-screen controls.
 */
class AndroidPlaybackController(
    private val appContext: Context,
    private val onPlayingChanged: (Boolean) -> Unit,
    private val onBufferingChanged: (Boolean) -> Unit,
    private val onEnded: () -> Unit,
    private val onError: (String) -> Unit,
    private val onSkipNext: (() -> Unit)? = null,
    private val onSkipPrevious: (() -> Unit)? = null,
) {
    init {
        PlaybackHolder.ensure(appContext)
        PlaybackHolder.setTransportHandlers(onSkipNext, onSkipPrevious)
        PlaybackHolder.player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlayingChanged(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> onBufferingChanged(true)
                    Player.STATE_READY -> onBufferingChanged(false)
                    Player.STATE_ENDED -> {
                        onBufferingChanged(false)
                        onEnded()
                    }
                    Player.STATE_IDLE -> onBufferingChanged(false)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onBufferingChanged(false)
                onError(error.message ?: "Playback failed")
            }
        })
    }

    val player get() = PlaybackHolder.player

    val isPlaying: Boolean get() = player.isPlaying
    val positionMs: Long get() = player.currentPosition.coerceAtLeast(0)
    val durationMs: Long
        get() = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0

    fun play(url: String, headers: Map<String, String>, song: Song? = null) {
        ensureService()
        PlaybackHolder.dataSourceFactory.setDefaultRequestProperties(headers)
        val meta = MediaMetadata.Builder()
            .setTitle(song?.title ?: "Unknown title")
            .setArtist(song?.artist)
            .setArtworkUri(song?.thumbnailUrl?.let { Uri.parse(it) })
            .build()
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaId(song?.videoId ?: url)
            .setMediaMetadata(meta)
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    fun pause() = player.pause()
    fun resume() {
        ensureService()
        player.play()
    }
    fun seekTo(positionMs: Long) = player.seekTo(positionMs.coerceAtLeast(0))

    fun stop() {
        player.stop()
        player.clearMediaItems()
    }

    fun release() {
        // Shared holder is released by app/service lifecycle; do not release here.
    }

    private fun ensureService() {
        val intent = Intent(appContext, PlaybackService::class.java)
        ContextCompat.startForegroundService(appContext, intent)
    }
}
