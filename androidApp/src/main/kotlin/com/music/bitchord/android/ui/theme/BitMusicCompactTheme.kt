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
    val MiniPlayerHeight = 64.dp
    val BottomNavHeight = 64.dp
    val ArtworkSmall = 44.dp
    val ArtworkMedium = 72.dp
    val ArtworkNowPlaying = 320.dp
    val CornerRadius = 14.dp
    val CornerRadiusSmall = 10.dp
}

// Design tokens from the supplied mobile UI guide (dark Apple-Music-inspired).
private val GuideBackground = Color(0xFF08080B)
private val GuideSurface = Color(0xFF17171D)
private val GuideSurfaceElevated = Color(0xFF24242C)
private val GuideTextPrimary = Color(0xFFFFFFFF)
private val GuideTextSecondary = Color(0xFFA6A6B0)
private val GuideAccent = Color(0xFFF52B83)
private val GuideAccentSecondary = Color(0xFF8B5CF6)
private val GuideSeparator = Color(0xFF303039)
private val GuideSuccess = Color(0xFF35C879)
private val GuideDanger = Color(0xFFFF5A67)

private val BitDark = darkColorScheme(
    primary = GuideAccent,
    onPrimary = GuideTextPrimary,
    primaryContainer = GuideSurfaceElevated.copy(alpha = 0.72f),
    onPrimaryContainer = GuideTextPrimary,
    secondary = GuideAccentSecondary,
    onSecondary = GuideTextPrimary,
    background = GuideBackground,
    onBackground = GuideTextPrimary,
    // Translucent so glass surfaces blend with the gradient behind them.
    surface = GuideSurface.copy(alpha = 0.72f),
    onSurface = GuideTextPrimary,
    surfaceVariant = GuideSurfaceElevated.copy(alpha = 0.62f),
    onSurfaceVariant = GuideTextSecondary,
    outline = GuideSeparator,
    error = GuideDanger,
    onError = GuideTextPrimary,
)

private val BitLight = lightColorScheme(
    primary = GuideAccent,
    onPrimary = GuideTextPrimary,
    primaryContainer = Color(0xFFFFD9E2).copy(alpha = 0.72f),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = GuideAccentSecondary,
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFFFFFFF).copy(alpha = 0.82f),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFECECF2).copy(alpha = 0.72f),
    onSurfaceVariant = Color(0xFF5B5B66),
    outline = Color(0xFFD6D6DF),
    error = GuideDanger,
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
