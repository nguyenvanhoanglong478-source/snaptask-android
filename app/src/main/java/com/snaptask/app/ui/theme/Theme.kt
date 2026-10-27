package com.snaptask.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark Theme: Obsidian Ink base with Velvet Curfew surface & Almond Hearth accents
private val DarkColorScheme = darkColorScheme(
    primary = AlmondHearth,
    onPrimary = ObsidianInk,
    primaryContainer = VelvetCurfew,
    onPrimaryContainer = AlmondHearth,
    background = ObsidianInk,
    onBackground = AlmondHearth,
    surface = ObsidianElevated,
    onSurface = AlmondHearth,
    surfaceVariant = VelvetCurfew,
    onSurfaceVariant = AlmondHearth,
    outline = VelvetCurfewLight
)

// Light / Warm Theme: Almond Hearth background with Velvet Curfew elements
private val LightColorScheme = lightColorScheme(
    primary = VelvetCurfew,
    onPrimary = AlmondHearth,
    primaryContainer = AlmondHearth,
    onPrimaryContainer = ObsidianInk,
    background = Color(0xFFFAF4EE),
    onBackground = ObsidianInk,
    surface = Color.White,
    onSurface = ObsidianInk,
    surfaceVariant = AlmondHearth,
    onSurfaceVariant = ObsidianInk,
    outline = AlmondHearthMuted
)

@Composable
fun SnapTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = ObsidianInk.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            window.navigationBarColor = ObsidianInk.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SnapTaskTypography,
        content = content
    )
}
