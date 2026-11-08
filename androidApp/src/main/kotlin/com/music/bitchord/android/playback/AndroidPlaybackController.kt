package com.music.bitchord.android.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * ExoPlayer bound to the URL and headers produced by the shared
 * [com.music.bitchord.data.innertube.StreamResolver].
 *
 * The data-source factory is reused between tracks; its default request
 * properties are replaced with the headers of the client that minted the
 * current URL before each prepare.
 */
class AndroidPlaybackController(
    context: Context,
    private val onIsPlayingChanged: (Boolean) -> Unit,
    private val onBufferingChanged: (Boolean) -> Unit,
    private val onEnded: () -> Unit,
    private val onError: (String) -> Unit,
) {
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        // The resolver's per-client User-Agent must survive; ExoPlayer's own
        // default would overwrite it and the CDN would answer 403.
        .setUserAgent(null)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(30_000)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onIsPlayingChanged(isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> onBufferingChanged(true)
                        Player.STATE_READY -> onBufferingChanged(false)
                        Player.STATE_ENDED -> {
                            onBufferingChanged(false)
                            onEnded()
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    onBufferingChanged(false)
                    onError(error.message ?: "Playback failed")
                }
            })
        }

    val isPlaying: Boolean
        get() = player.isPlaying

    val positionMs: Long
        get() = player.currentPosition.coerceAtLeast(0)

    val durationMs: Long
        get() = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0

    fun play(url: String, headers: Map<String, String>) {
        httpDataSourceFactory.setDefaultRequestProperties(headers)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    fun pause() = player.pause()

    fun resume() = player.play()

    fun seekTo(positionMs: Long) = player.seekTo(positionMs.coerceAtLeast(0))

    fun stop() {
        player.stop()
        player.clearMediaItems()
    }

    fun release() = player.release()
}
