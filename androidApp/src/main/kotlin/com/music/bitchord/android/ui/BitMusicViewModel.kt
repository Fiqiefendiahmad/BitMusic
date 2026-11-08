package com.music.bitchord.android.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.bitchord.android.data.Collection
import com.music.bitchord.android.data.MusicRepository
import com.music.bitchord.android.data.playableSongs
import com.music.bitchord.android.data.toSong
import com.music.bitchord.android.playback.AndroidPlaybackController
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsRepository
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.ui.LyricsProviderState
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
    private var lyricsJob: Job? = null

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

    var homeState by mutableStateOf<UiState<List<HomeShelf>>>(UiState.Loading)
        private set
    var homeContinuation by mutableStateOf<String?>(null)
        private set
    var isLoadingMoreHome by mutableStateOf(false)
        private set
    var collection by mutableStateOf<Collection?>(null)
        private set
    var collectionTitle by mutableStateOf("")
        private set
    var collectionBrowseId by mutableStateOf<String?>(null)
        private set
    var collectionLoading by mutableStateOf(false)
        private set
    var collectionError by mutableStateOf<String?>(null)
        private set

    /** Home's playable cards in shelf order; the shared page still draws the shelves. */
    val homeTracks: List<Song>
        get() = (homeState as? UiState.Success)?.data.orEmpty().playableSongs()

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

    /** The shared player's playhead object, kept out of composition-wide state. */
    val playbackPosition = PlaybackPosition()

    var repeatMode by mutableIntStateOf(0) // RepeatModes.OFF
        private set
    var shuffleEnabled by mutableStateOf(false)
        private set
    var autoplayEnabled by mutableStateOf(false)
        private set

    var lyrics by mutableStateOf<List<LyricLine>?>(null)
        private set
    var lyricsSource by mutableStateOf<LyricsSource?>(null)
        private set
    var lyricsUnavailable by mutableStateOf(false)
        private set
    var lyricsProviderStates by mutableStateOf<Map<LyricsSource, LyricsProviderState>>(emptyMap())
        private set

    val queueIndex: Int
        get() = queue.indexOfFirst { it.videoId == current?.videoId }
    val hasPrevious: Boolean
        get() = queue.size > 1 || queueIndex > 0
    val hasNext: Boolean
        get() = queue.size > 1 || (queueIndex >= 0 && queueIndex < queue.lastIndex)

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
                    playbackPosition.advancing = true
                    playbackPosition.report(positionMs)
                } else {
                    playbackPosition.advancing = false
                }
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    fun refreshHome() {
        viewModelScope.launch {
            isLoadingHome = true
            homeError = null
            if (homeState !is UiState.Success) homeState = UiState.Loading
            repository.home()
                .onSuccess { feed ->
                    homeState = UiState.Success(feed.shelves)
                    homeContinuation = feed.continuation
                }
                .onFailure { error ->
                    val message = error.message ?: "Could not load the home feed"
                    homeError = message
                    if (homeState !is UiState.Success) homeState = UiState.Error(message)
                }
            isLoadingHome = false
        }
    }

    fun loadMoreHome() {
        val token = homeContinuation ?: return
        if (isLoadingMoreHome) return
        viewModelScope.launch {
            isLoadingMoreHome = true
            repository.moreHome(token)
                .onSuccess { feed ->
                    val current = (homeState as? UiState.Success)?.data.orEmpty()
                    val titles = current.map { it.title }.toSet()
                    homeState = UiState.Success(
                        current + feed.shelves.filterNot { it.title in titles },
                    )
                    homeContinuation = feed.continuation
                }
                .onFailure { homeError = it.message ?: "Could not load more of the home feed" }
            isLoadingMoreHome = false
        }
    }

    /** Opens a home card: a track plays, an album or playlist opens its real track list. */
    fun openShelfItem(item: ShelfItem) {
        val videoId = item.videoId
        if (videoId != null) {
            val song = item.toSong() ?: return
            play(song, homeTracks)
        } else {
            val browseId = item.browseId ?: return
            openCollection(browseId, item.title)
        }
    }

    fun openCollection(browseId: String, title: String) {
        viewModelScope.launch {
            collection = null
            collectionTitle = title
            collectionBrowseId = browseId
            collectionError = null
            collectionLoading = true
            repository.browse(browseId, title)
                .onSuccess {
                    collection = it
                    collectionTitle = it.title
                }
                .onFailure { collectionError = it.message ?: "Could not open this collection" }
            collectionLoading = false
        }
    }

    fun retryCollection() {
        val browseId = collectionBrowseId ?: return
        openCollection(browseId, collectionTitle)
    }

    fun closeCollection() {
        collection = null
        collectionBrowseId = null
        collectionError = null
        collectionLoading = false
    }

    fun playCollectionSong(song: Song) {
        play(song, collection?.songs.orEmpty())
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
        playbackPosition.report(0L)
        playbackPosition.seeks++
        playbackError = null
        load(song)
        loadLyrics(song)
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
        val next = when {
            shuffleEnabled && list.size > 1 -> list.filterNot { it.videoId == song.videoId }.random()
            else -> list.getOrNull(index + 1)
                ?: if (!auto || repeatMode == REPEAT_ALL || autoplayEnabled) list.firstOrNull() else null
        }
        if (next != null) {
            play(next, list)
        } else {
            playback.pause()
            positionMs = durationMs
            playbackPosition.report(durationMs)
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
        seekToMs((durationMs * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun seekToMs(targetMs: Long) {
        val target = targetMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
        positionMs = target
        playbackPosition.report(target)
        playbackPosition.seeks++
        playback.seekTo(target)
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
    }

    fun toggleAutoplay() {
        autoplayEnabled = !autoplayEnabled
    }

    fun cycleRepeat() {
        repeatMode = when (repeatMode) {
            REPEAT_ALL -> REPEAT_ONE
            REPEAT_ONE -> REPEAT_OFF
            else -> REPEAT_ALL
        }
        playback.player.repeatMode = if (repeatMode == REPEAT_ONE) {
            androidx.media3.common.Player.REPEAT_MODE_ONE
        } else {
            androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }

    fun jumpTo(index: Int) {
        queue.getOrNull(index)?.let { play(it, queue) }
    }

    fun removeFromQueue(index: Int) {
        if (index !in queue.indices) return
        val removed = queue[index]
        val next = queue.toMutableList().apply { removeAt(index) }
        if (removed.videoId == current?.videoId) {
            val replacement = next.getOrNull(index.coerceAtMost(next.lastIndex))
            if (replacement != null) {
                queue = next
                play(replacement, next)
            } else {
                playback.stop()
                current = null
                queue = emptyList()
            }
        } else {
            queue = next
        }
    }

    fun moveInQueue(from: Int, to: Int) {
        if (from !in queue.indices || to !in queue.indices || from == to) return
        queue = queue.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun clearQueue() {
        queue = current?.let { listOf(it) } ?: emptyList()
    }

    fun selectLyricsProvider(source: LyricsSource) {
        current?.let { loadLyrics(it, only = source) }
    }

    fun dismissPlaybackError() {
        playbackError = null
    }

    private fun loadLyrics(song: Song, only: LyricsSource? = null) {
        lyricsJob?.cancel()
        lyrics = null
        lyricsSource = null
        lyricsUnavailable = false
        lyricsProviderStates = LyricsSource.offered.associateWith { LyricsProviderState.NOT_FETCHED }
        lyricsJob = viewModelScope.launch {
            val found = LyricsRepository.lyrics(
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                durationMs = durationMs,
                album = song.albumName,
                sources = (only?.let { setOf(it) } ?: LyricsSource.offered.toSet()),
                order = only?.let { listOf(it) } ?: LyricsSource.offered,
                prioritizeSyllableSync = true,
                onSourceStarted = { source ->
                    lyricsProviderStates = lyricsProviderStates + (source to LyricsProviderState.FETCHING)
                },
                onSourceResult = { source, result ->
                    lyricsProviderStates = lyricsProviderStates +
                        (source to if (result != null) LyricsProviderState.FOUND else LyricsProviderState.NOT_FOUND)
                },
            )
            if (current?.videoId == song.videoId) {
                lyrics = found?.lines
                lyricsSource = found?.source
                lyricsUnavailable = found == null
            }
        }
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
        lyricsJob?.cancel()
        playback.release()
        super.onCleared()
    }

    private companion object {
        const val PROGRESS_TICK_MS = 500L
        const val SEARCH_DEBOUNCE_MS = 350L
        const val RESTART_THRESHOLD_MS = 3_000L
        const val REPEAT_OFF = 0
        const val REPEAT_ONE = 1
        const val REPEAT_ALL = 2
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
