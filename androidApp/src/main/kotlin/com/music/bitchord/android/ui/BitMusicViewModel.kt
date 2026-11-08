package com.music.bitchord.android.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.bitchord.android.data.MusicRepository
import com.music.bitchord.android.playback.AndroidPlaybackController
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * One owner for the real catalog and the real player.
 *
 * Tracks come from [MusicRepository], stream URLs from the shared resolver,
 * and playback from ExoPlayer. The UI remains a thin portrait-first shell.
 */
class BitMusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository()
    private var resolveJob: Job? = null
    private var searchJob: Job? = null

    private val playback = AndroidPlaybackController(
        context = application,
        onIsPlayingChanged = { isPlaying = it },
        onBufferingChanged = { isBuffering = it },
        onEnded = { skipNext(auto = true) },
        onError = { message ->
            playbackError = message
            isBuffering = false
        },
    )

    var homeTracks by mutableStateOf<List<Song>>(emptyList())
        private set
    var libraryTracks by mutableStateOf<List<Song>>(emptyList())
        private set
    var searchResults by mutableStateOf<List<Song>>(emptyList())
        private set
    var searchQuery by mutableStateOf("")
        private set
    var isLoadingHome by mutableStateOf(true)
        private set
    var isLoadingLibrary by mutableStateOf(true)
        private set
    var isSearching by mutableStateOf(false)
        private set
    var isResolving by mutableStateOf(false)
        private set
    var homeError by mutableStateOf<String?>(null)
        private set
    var libraryError by mutableStateOf<String?>(null)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    var playbackError by mutableStateOf<String?>(null)
        private set

    var current by mutableStateOf<Song?>(null)
        private set
    var queue by mutableStateOf<List<Song>>(emptyList())
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set

    val playerState: PlayerUiState
        get() = PlayerUiState(
            current = current,
            isPlaying = isPlaying,
            progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            queue = queue,
            positionMs = positionMs,
            durationMs = durationMs,
            isBuffering = isResolving || isBuffering,
        )

    init {
        refreshHome()
        refreshLibrary()
        viewModelScope.launch {
            while (isActive) {
                if (playback.isPlaying) {
                    positionMs = playback.positionMs
                    playback.durationMs.takeIf { it > 0 }?.let { durationMs = it }
                }
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    fun refreshHome() {
        viewModelScope.launch {
            isLoadingHome = true
            homeError = null
            repository.home()
                .onSuccess { homeTracks = it }
                .onFailure { homeError = it.message ?: "Could not load the home feed" }
            isLoadingHome = false
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            isLoadingLibrary = true
            libraryError = null
            repository.library()
                .onSuccess { libraryTracks = it }
                .onFailure { libraryError = it.message ?: "Could not load the library" }
            isLoadingLibrary = false
        }
    }

    fun onSearchQueryChange(value: String) {
        searchQuery = value
        searchJob?.cancel()
        if (value.isBlank()) {
            searchResults = emptyList()
            searchError = null
            isSearching = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            isSearching = true
            searchError = null
            repository.search(value)
                .onSuccess { searchResults = it }
                .onFailure { searchError = it.message ?: "Search failed" }
            isSearching = false
        }
    }

    /** Plays [song] in the context of [sourceQueue] (home, search results, or the current queue). */
    fun play(song: Song, sourceQueue: List<Song>) {
        val contextQueue = if (sourceQueue.any { it.videoId == song.videoId }) {
            sourceQueue
        } else {
            listOf(song)
        }.distinctBy { it.videoId }

        queue = contextQueue
        current = song
        positionMs = 0L
        durationMs = parseDurationMs(song.durationText)
        playbackError = null
        load(song)
    }

    fun togglePlayPause() {
        val song = current
        if (song == null) {
            homeTracks.firstOrNull()?.let { play(it, homeTracks) }
            return
        }
        if (isPlaying) {
            playback.pause()
        } else {
            if (playback.player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                load(song)
            } else {
                playback.resume()
            }
        }
    }

    fun skipNext(auto: Boolean = false) {
        val song = current ?: return
        val list = queue
        val index = list.indexOfFirst { it.videoId == song.videoId }
        val next = list.getOrNull(index + 1)
            ?: if (!auto) list.firstOrNull() else null
        if (next != null) {
            play(next, list)
        } else {
            playback.pause()
            positionMs = durationMs
        }
    }

    fun skipPrevious() {
        val song = current ?: return
        if (positionMs > RESTART_THRESHOLD_MS) {
            seekTo(0f)
            return
        }
        val list = queue
        val index = list.indexOfFirst { it.videoId == song.videoId }
        val previous = list.getOrNull(index - 1) ?: list.lastOrNull()
        if (previous != null) play(previous, list)
    }

    fun seekTo(fraction: Float) {
        val target = (durationMs * fraction.coerceIn(0f, 1f)).toLong()
        positionMs = target
        playback.seekTo(target)
    }

    fun dismissPlaybackError() {
        playbackError = null
    }

    private fun load(song: Song) {
        resolveJob?.cancel()
        isResolving = true
        resolveJob = viewModelScope.launch {
            repository.resolve(song)
                .onSuccess { stream ->
                    if (current?.videoId == song.videoId) {
                        playback.play(stream.url, stream.headers)
                        isResolving = false
                    }
                }
                .onFailure { error ->
                    if (current?.videoId == song.videoId) {
                        playbackError = error.message ?: "Could not resolve this track"
                        isResolving = false
                        isPlaying = false
                    }
                }
        }
    }

    override fun onCleared() {
        resolveJob?.cancel()
        searchJob?.cancel()
        playback.release()
        super.onCleared()
    }

    private companion object {
        const val PROGRESS_TICK_MS = 500L
        const val SEARCH_DEBOUNCE_MS = 350L
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}

/** `4:11` to milliseconds, so the scrubber has a length before the stream reports one. */
internal fun parseDurationMs(text: String?): Long {
    val parts = text?.split(':') ?: return 0L
    if (parts.isEmpty() || parts.size > 3) return 0L
    return runCatching {
        parts.fold(0L) { total, part -> total * 60 + part.toLong() } * 1_000
    }.getOrDefault(0L)
}
