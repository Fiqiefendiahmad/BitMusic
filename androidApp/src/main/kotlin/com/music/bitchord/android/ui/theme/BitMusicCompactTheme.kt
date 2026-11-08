package com.music.bitchord.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Small-screen-first theme tokens for BitMusic Android.
 * Keeps contrast high, touch targets >= 48dp, and spacing compact for narrow viewports.
 */
object CompactDimens {
    val ScreenPadding = 16.dp
    val ScreenPaddingCompact = 12.dp
    val ItemSpacing = 10.dp
    val SectionSpacing = 20.dp
    val MinTouchTarget = 48.dp
    val MiniPlayerHeight = 68.dp
    val BottomNavHeight = 64.dp
    val ArtworkSmall = 52.dp
    val ArtworkMedium = 72.dp
    val ArtworkNowPlaying = 320.dp
    val CornerRadius = 14.dp
    val CornerRadiusSmall = 10.dp
}

private val BitDark = darkColorScheme(
    primary = Color(0xFF6EE7B7),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005138),
    secondary = Color(0xFFB4CCBE),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF151C18),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF89938C),
)

private val BitLight = lightColorScheme(
    primary = Color(0xFF006C4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF89F8C7),
    background = Color(0xFFF6FBF6),
    surface = Color(0xFFF6FBF6),
    surfaceVariant = Color(0xFFDBE5DD),
)

@Composable
fun BitMusicCompactTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BitDark else BitLight,
        content = content,
    )
}
