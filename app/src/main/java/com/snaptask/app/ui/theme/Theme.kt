package com.snaptask.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SnapTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkSnapTaskColors else LightSnapTaskColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.flash,
            onPrimary = colors.onFlash,
            primaryContainer = colors.surface,
            onPrimaryContainer = colors.text,
            secondary = colors.flash,
            onSecondary = colors.onFlash,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.elevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border
        )
    } else {
        lightColorScheme(
            primary = colors.flash,
            onPrimary = colors.onFlash,
            primaryContainer = colors.surface,
            onPrimaryContainer = colors.text,
            secondary = colors.flash,
            onSecondary = colors.onFlash,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.elevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colors.bg.toArgb()
            window.navigationBarColor = colors.bg.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalSnapTaskColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SnapTaskTypography,
            content = content
        )
    }
}
