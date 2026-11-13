package com.snaptask.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
    val onUrgent: Color,
    val urgentSoft: Color,
    val warning: Color,
    val bubble: Color,
    val dock: Color,
    val hm0: Color,
    val hm1: Color,
    val hm2: Color,
    val hm3: Color,
    val hm4: Color,
    val hmBorder: Color
)

// WCAG AA verified tokens for Snap Pop dark theme (#0F0F0E base, #D4FF3A Electric Lime accent).
val DarkSnapTaskColors = SnapTaskColors(
    bg = Color(0xFF0F0F0E),
    surface = Color(0xFF1A1A18),
    elevated = Color(0xFF252522),
    border = Color(0xFF34342F),
    borderHighlight = Color(0xFF484842),
    text = Color(0xFFFAFAF6),
    textSecondary = Color(0xFFB0AFA7),
    textMuted = Color(0xFF8E8D85),
    flash = Color(0xFFD4FF3A),
    onFlash = Color(0xFF10120A),
    done = Color(0xFFD4FF3A),
    urgent = Color(0xFFFF5C4D),
    onUrgent = Color(0xFF10120A),
    urgentSoft = Color(0x29FF5C4D),
    warning = Color(0xFFD4FF3A),
    bubble = Color(0xAD0F0F0E),
    dock = Color(0xEB181816),
    hm0 = Color(0xFF242420),
    hm1 = Color(0xFF3E4B19),
    hm2 = Color(0xFF6C851E),
    hm3 = Color(0xFFA2CB2B),
    hm4 = Color(0xFFD4FF3A),
    hmBorder = Color(0x12FFFFFF)
)

// WCAG AA verified tokens for Snap Pop light theme (#F3F2EC warm paper, dark ink heatmap ramp).
val LightSnapTaskColors = SnapTaskColors(
    bg = Color(0xFFF3F2EC),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFE8E6DE),
    border = Color(0xFFD5D2C7),
    borderHighlight = Color(0xFFC2BEB2),
    text = Color(0xFF141413),
    textSecondary = Color(0xFF575650),
    textMuted = Color(0xFF66655E),
    flash = Color(0xFFD4FF3A),
    onFlash = Color(0xFF10120A),
    done = Color(0xFF3D5E00),
    urgent = Color(0xFFC93223),
    onUrgent = Color(0xFFFFFFFF),
    urgentSoft = Color(0x1FC93223),
    warning = Color(0xFF5C4700),
    bubble = Color(0xAD0F0F0E),
    dock = Color(0xF0FFFFFF),
    hm0 = Color(0xFFEFECE3),
    hm1 = Color(0xFFD8F07A),
    hm2 = Color(0xFFAEE02B),
    hm3 = Color(0xFF73A600),
    hm4 = Color(0xFF3D5E00),
    hmBorder = Color(0x24141413)
)

val LocalSnapTaskColors = staticCompositionLocalOf { DarkSnapTaskColors }

val MaterialTheme.snapColors: SnapTaskColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSnapTaskColors.current
