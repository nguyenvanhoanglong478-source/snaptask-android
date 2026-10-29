package com.snaptask.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// INK & FLASH DESIGN SYSTEM (Light + Dark)
// Single Source of Truth
// ==========================================

data class SnapTaskColors(
    val bg: Color,
    val surface: Color,
    val elevated: Color,
    val border: Color,
    val borderHighlight: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val flash: Color,
    val onFlash: Color,
    val done: Color,
    val urgent: Color,
    val warning: Color,
    val scrim: Brush,
    val glass: Color
)

val DarkSnapTaskColors = SnapTaskColors(
    bg = Color(0xFF121110),
    surface = Color(0xFF1B1A17),
    elevated = Color(0xFF26241F),
    border = Color(0xFF34312B),
    borderHighlight = Color(0xFF4A463E),
    text = Color(0xFFF5F1E8),
    textSecondary = Color(0xFFB3AC9E),
    textMuted = Color(0xFF8F897B),
    flash = Color(0xFFFFD23F),
    onFlash = Color(0xFF17140A),
    done = Color(0xFF4ADE8A),
    urgent = Color(0xFFFF7A6B),
    warning = Color(0xFFFFD23F), // warning uses flash #FFD23F in dark mode
    scrim = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x66121110),
            Color(0xF2121110)
        )
    ),
    glass = Color(0xE61B1A17) // 90% opacity #1B1A17
)

val LightSnapTaskColors = SnapTaskColors(
    bg = Color(0xFFF7F4EC),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFEFEBE0),
    border = Color(0xFFDDD7C8),
    borderHighlight = Color(0xFFC9C2B0),
    text = Color(0xFF17140A),
    textSecondary = Color(0xFF5B5648),
    textMuted = Color(0xFF6B665A),
    flash = Color(0xFFFFD23F), // Used for button/chip backgrounds; text on it is always #17140A
    onFlash = Color(0xFF17140A),
    done = Color(0xFF1B7F4B),
    urgent = Color(0xFFC23A28),
    warning = Color(0xFF8A5A00), // Text and border warning for high contrast on light bg
    scrim = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x6617140A),
            Color(0xE617140A)
        )
    ),
    glass = Color(0xE6FFFFFF) // 90% opacity #FFFFFF
)

val LocalSnapTaskColors = staticCompositionLocalOf { DarkSnapTaskColors }

val MaterialTheme.snapColors: SnapTaskColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSnapTaskColors.current
