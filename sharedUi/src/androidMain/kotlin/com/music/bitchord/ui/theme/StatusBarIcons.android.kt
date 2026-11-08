package com.music.bitchord.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun StatusBarIcons(dark: Boolean) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window ?: return
    DisposableEffect(window, dark) {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        onDispose { }
    }
}

@Composable
actual fun HideStatusBar(hidden: Boolean) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window ?: return
    DisposableEffect(window, hidden, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        if (hidden) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        onDispose { controller.show(WindowInsetsCompat.Type.statusBars()) }
    }
}

@Composable
actual fun stableStatusBarInsets(): WindowInsets = WindowInsets.statusBars

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
