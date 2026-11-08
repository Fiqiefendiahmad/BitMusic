package com.music.bitchord.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.data.DebugLog
import com.music.bitchord.data.innertube.Innertube
import com.music.bitchord.data.innertube.InnerTubeXResolver
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.android.playback.AndroidPlayerHost
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.player.PlayerPlatform
import java.util.Locale

/**
 * Process-wide setup for the shared data layer.
 *
 * The Android app deliberately reuses the desktop's configuration and stream
 * resolution instead of reimplementing playback: [InnerTubeXResolver] receives
 * Android's own cache directory and a SharedPreferences-backed config store,
 * and [Innertube] follows the device language. No sample catalog is involved.
 */
class BitMusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatform.init(this)
    }
}

object AndroidPlatform {
    private lateinit var preferences: SharedPreferences

    fun init(application: Application) {
        preferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        DebugLog.sink = DebugLog.Sink { level, tag, message, error ->
            when (level) {
                'E' -> android.util.Log.e(tag, message, error)
                'W' -> android.util.Log.w(tag, message, error)
                'I' -> android.util.Log.i(tag, message)
                else -> android.util.Log.d(tag, message)
            }
        }

        Innertube.appLanguage = { Locale.getDefault().toLanguageTag() }
        StreamResolver.maxKbps = { preferences.getInt(KEY_MAX_KBPS, Int.MAX_VALUE) }

        InnerTubeXResolver.init(
            filesDir = application.filesDir,
            store = SharedPreferencesConfigStore(preferences),
            poTokenProvider = null,
            prewarm = true,
        )

        PlayerPlatform.install(AndroidPlayerHost(application))
        AppUi.install(AndroidAppUiHost(application))
    }

    private const val PREFS_NAME = "bitchord"
    private const val KEY_MAX_KBPS = "stream_max_kbps"
}

/** InnerTubeX's remote player config, kept where the rest of the app's settings live. */
private class SharedPreferencesConfigStore(
    private val preferences: SharedPreferences,
) : InnerTubeXResolver.ConfigStore {
    override fun getString(key: String): String? =
        preferences.getString(PREFIX + key, null)?.takeIf { it.isNotEmpty() }

    override fun getLong(key: String): Long? =
        preferences.getLong(PREFIX + key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }

    override fun putString(key: String, value: String) {
        preferences.edit().putString(PREFIX + key, value).apply()
    }

    override fun putLong(key: String, value: Long) {
        preferences.edit().putLong(PREFIX + key, value).apply()
    }

    private companion object {
        const val PREFIX = "innertubex_"
    }
}
