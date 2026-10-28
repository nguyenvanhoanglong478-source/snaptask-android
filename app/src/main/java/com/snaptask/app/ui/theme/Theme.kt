package com.snaptask.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MidnightDarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = BgSurface,
    onPrimaryContainer = TextPrimary,
    secondary = AccentSecondary,
    onSecondary = Color.White,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun SnapTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = MidnightDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = BgPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            window.navigationBarColor = BgPrimary.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SnapTaskTypography,
        content = content
    )
}
