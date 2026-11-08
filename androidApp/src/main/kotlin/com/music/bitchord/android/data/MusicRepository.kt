package com.music.bitchord.android.data

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.model.HomeFeed
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song

/** A resolved media URL and the headers the issuing client expects. */
data class ResolvedStream(
    val url: String,
    val headers: Map<String, String>,
)

/** One album or playlist opened from a home card. */
data class Collection(
    val title: String,
    val songs: List<Song>,
    val continuation: String?,
)

/**
 * The Android UI's thin layer over the shared catalog and resolver.
 *
 * Home is kept as the shelves YouTube actually returns rather than flattened,
 * so the shared [com.music.bitchord.ui.screens.HomeScreen] can draw every
 * section, card and collection exactly as the desktop does. Tracks only, and
 * stream URLs, still come from the same shared data layer.
 */
class MusicRepository {
    suspend fun home(): Result<HomeFeed> = YtMusicRepository.home()

    suspend fun moreHome(token: String): Result<HomeFeed> = YtMusicRepository.moreHome(token)

    suspend fun browse(browseId: String, fallbackTitle: String): Result<Collection> =
        YtMusicRepository.browseSongs(browseId).map { page ->
            Collection(
                title = page.header?.title?.takeIf(String::isNotBlank) ?: fallbackTitle,
                songs = page.songs,
                continuation = page.continuation,
            )
        }

    suspend fun library(): Result<List<Song>> =
        YtMusicRepository.library().map { page ->
            (page.likedSongs + page.librarySongs).distinctBy { it.videoId }
        }

    suspend fun search(query: String): Result<List<Song>> =
        YtMusicRepository.search(query, SearchFilter.SONGS).map { results ->
            results.mapNotNull { result ->
                when (result) {
                    is SearchResult.Track -> result.song
                    is SearchResult.TopTrack -> result.song
                    is SearchResult.Browse -> null
                }
            }
        }

    suspend fun resolve(song: Song): Result<ResolvedStream> = runCatching {
        val url = StreamResolver.resolve(song.videoId)
        ResolvedStream(url = url, headers = StreamResolver.mediaHeadersFor(url))
    }
}

/**
 * A home card lifted into a playable [Song].
 *
 * The subtitle is a compact byline such as `Artist • 4:11 • Song`, so the
 * pieces are separated the same way the desktop separates them instead of
 * showing the whole line as the artist.
 */
fun ShelfItem.toSong(): Song? = videoId?.let { id ->
    val parts = subtitle.split(" • ", " · ", " | ").filter(String::isNotBlank)
    Song(
        videoId = id,
        title = title,
        artist = parts.firstOrNull {
            !it.matches(SHELF_DURATION) &&
                it.lowercase() !in SHELF_TYPE_WORDS &&
                !it.matches(SHELF_TALLY)
        } ?: "Unknown Artist",
        thumbnailUrl = thumbnailUrl,
        durationText = parts.firstOrNull { it.matches(SHELF_DURATION) },
        isVideo = parts.any {
            it.lowercase() in SHELF_VIDEO_WORDS || it.contains("views", ignoreCase = true)
        },
    )
}

/** Every playable card on the feed, in shelf order. */
fun List<HomeShelf>.playableSongs(): List<Song> =
    flatMap { shelf -> shelf.items.mapNotNull(ShelfItem::toSong) }.distinctBy { it.videoId }

private val SHELF_DURATION = Regex("""\d{1,2}:\d{2}(?::\d{2})?""")
private val SHELF_TALLY = Regex("""[0-9,.]+\s*(?:views?|likes?|songs?)""", RegexOption.IGNORE_CASE)
private val SHELF_TYPE_WORDS = setOf("song", "video", "album", "ep", "single", "playlist", "mix")
private val SHELF_VIDEO_WORDS = setOf("video", "music video")
