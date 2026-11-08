package com.music.bitchord.android

import android.content.Context
import androidx.compose.runtime.Composable
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.ui.AppUiHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The shared pages' settings, answered from the same preferences the player uses. */
internal class AndroidAppUiHost(context: Context) : AppUiHost {
    private val prefs = context.applicationContext.getSharedPreferences("bitchord", Context.MODE_PRIVATE)

    override val swipeToPlayNext: StateFlow<Boolean> =
        MutableStateFlow(prefs.getBoolean("swipe_play_next", true))

    private val _homeRecentsViewType = MutableStateFlow(
        runCatching {
            LibraryViewType.valueOf(prefs.getString("home_recents_view", "LIST") ?: "LIST")
        }.getOrDefault(LibraryViewType.LIST),
    )
    override val homeRecentsViewType: StateFlow<LibraryViewType> = _homeRecentsViewType

    override fun setHomeRecentsViewType(value: LibraryViewType) {
        _homeRecentsViewType.value = value
        prefs.edit().putString("home_recents_view", value.name).apply()
    }

    override val pinnedPlaylists: StateFlow<List<String>> = MutableStateFlow(emptyList())

    override val librarySort: StateFlow<LibrarySort> = MutableStateFlow(
        runCatching {
            LibrarySort.valueOf(prefs.getString("library_sort", "DEFAULT") ?: "DEFAULT")
        }.getOrDefault(LibrarySort.DEFAULT),
    )

    @Composable
    override fun downloadedIds(): Set<String> = emptySet()
}
