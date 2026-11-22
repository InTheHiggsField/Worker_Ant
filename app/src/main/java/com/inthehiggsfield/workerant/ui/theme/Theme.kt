package com.inthehiggsfield.workerant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Applies the chosen [theme]'s colors, fonts, and corner shapes to everything inside it. */
@Composable
fun WorkerAntTheme(
    theme: AppTheme = AppTheme.Wallpaper,
    content: @Composable () -> Unit,
) {
    val typography = remember(theme) { theme.typography() }
    val shapes = remember(theme) { theme.shapes() }
    MaterialTheme(
        colorScheme = theme.colorScheme(isSystemInDarkTheme()),
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
