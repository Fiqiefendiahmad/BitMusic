package com.music.bitchord.android.data

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song

/** A resolved media URL and the headers the issuing client expects. */
data class ResolvedStream(
    val url: String,
    val headers: Map<String, String>,
)

/**
 * The Android UI's thin layer over the shared catalog and resolver.
 *
 * Home, search, library, and stream URLs all come from `:shared`; nothing here
 * invents tracks. Shelf cards are lifted into [Song]s because the compact
 * layout shows one flat, tappable list.
 */
class MusicRepository {
    suspend fun home(): Result<List<Song>> =
        YtMusicRepository.home().map { feed ->
            feed.shelves
                .flatMap { shelf -> shelf.items.mapNotNull(ShelfItem::toSong) }
                .distinctBy { it.videoId }
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

private fun ShelfItem.toSong(): Song? = videoId?.let { id ->
    Song(
        videoId = id,
        title = title,
        artist = subtitle,
        thumbnailUrl = thumbnailUrl,
        durationText = null,
    )
}
