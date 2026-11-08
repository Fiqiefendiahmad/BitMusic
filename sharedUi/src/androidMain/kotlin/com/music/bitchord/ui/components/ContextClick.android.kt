package com.music.bitchord.ui.components

import androidx.compose.ui.Modifier

/**
 * A phone has no secondary button. Long press is wired by
 * [longPressMenuClickable] on every row that has a menu, so there is nothing
 * left for this modifier to do.
 */
actual fun Modifier.contextClick(onClick: (() -> Unit)?): Modifier = this
