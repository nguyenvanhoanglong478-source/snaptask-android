package com.snaptask.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NotionDarkColorScheme = darkColorScheme(
    primary = LocketAccent,
    onPrimary = TextPrimary,
    primaryContainer = NotionCardSurface,
    onPrimaryContainer = TextPrimary,
    background = NotionDarkBg,
    onBackground = TextPrimary,
    surface = NotionCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = ChipInactiveBg,
    onSurfaceVariant = TextSecondary,
    outline = NotionBorder
)

@Composable
fun SnapTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = NotionDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = NotionDarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            window.navigationBarColor = NotionDarkBg.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SnapTaskTypography,
        content = content
    )
}
