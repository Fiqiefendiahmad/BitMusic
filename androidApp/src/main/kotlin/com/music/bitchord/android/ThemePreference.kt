package com.music.bitchord.android

import android.content.Context

/**
 * Persisted appearance override for the Android shell.
 *
 * Values: -1 = follow the system, 0 = light, 1 = dark. Stored in the same
 * SharedPreferences file the rest of the app uses so a single reset clears it.
 */
object ThemePreference {
    private const val PREFS = "bitchord_theme"
    private const val KEY_DARK_MODE = "dark_mode_override"

    fun get(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_DARK_MODE, -1)

    fun set(context: Context, value: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DARK_MODE, value.coerceIn(-1, 1))
            .apply()
    }
}
