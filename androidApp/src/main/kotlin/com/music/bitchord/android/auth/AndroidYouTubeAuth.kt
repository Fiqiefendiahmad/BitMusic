package com.music.bitchord.android.auth

import android.content.Context
import android.webkit.CookieManager
import com.music.bitchord.data.innertube.Innertube
import com.music.bitchord.data.innertube.StreamResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Minimal Android YouTube Music sign-in state.
 *
 * Captures the session cookie from a WebView login and adopts it into the
 * shared [Innertube] client. The cookie is persisted in app-private prefs;
 * sign-out clears it and returns to guest browsing.
 */
object AndroidYouTubeAuth {
    private const val PREFS = "bitchord"
    private const val KEY_COOKIE = "ytmusic_cookie"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _cookie = MutableStateFlow<String?>(null)
    val cookie: StateFlow<String?> = _cookie.asStateFlow()
    val isSignedIn: Boolean get() = !_cookie.value.isNullOrBlank()

    fun restore(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_COOKIE, null)
            ?.takeIf { it.isNotBlank() }
        _cookie.value = saved
        if (saved != null) adopt(saved, persist = false, context = context)
    }

    fun adopt(cookie: String, persist: Boolean = true, context: Context? = null) {
        val normalized = cookie.trim().takeIf { it.isNotBlank() }
        _cookie.value = normalized
        Innertube.cookie = normalized
        Innertube.selectChannel(null, null, null)
        StreamResolver.onSessionChanged()
        if (persist && context != null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_COOKIE, normalized)
                .apply()
        }
        if (normalized != null) {
            scope.launch {
                runCatching { Innertube.refreshSessionScope() }
            }
        }
    }

    fun signOut(context: Context) {
        _cookie.value = null
        Innertube.cookie = null
        Innertube.selectChannel(null, null, null)
        StreamResolver.onSessionChanged()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_COOKIE)
            .apply()
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
    }

    /** True when the cookie jar contains a value that can sign YouTube requests. */
    fun hasSigningSecret(cookie: String): Boolean {
        val jar = cookie.split(';').mapNotNull { entry ->
            val name = entry.substringBefore('=').trim()
            val value = entry.substringAfter('=', "").trim()
            if (name.isEmpty() || value.isEmpty()) null else name to value
        }.toMap()
        return SIGNING_COOKIES.any { jar[it] != null }
    }

    private val SIGNING_COOKIES = listOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")
}
