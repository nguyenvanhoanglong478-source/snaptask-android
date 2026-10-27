package com.snaptask.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Velvet & Obsidian Theme (User's Exact Palette)
val ObsidianInk  = Color(0xFF151311) // Rich deep obsidian
val VelvetCurfew = Color(0xFF4B262F) // Deep wine / plum burgundy
val AlmondHearth = Color(0xFFEED3BA) // Warm creamy almond

// Complementary & Accessible High-Contrast Colors
val VelvetCurfewLight = Color(0xFF6B3843)
val VelvetCurfewDark  = Color(0xFF2E171D)
val AlmondHearthMuted = Color(0xFFD8BC9F)
val ObsidianElevated  = Color(0xFF1F1A1C)

// Ultra-High-Contrast Text Palette (WCAG AAA Compliant)
val TextHighContrast = Color(0xFFFFFDF9) // Crisp bright white-cream (14:1 contrast ratio)
val TextMediumContrast = Color(0xFFEBDBCF) // Almond cream for subtitles
val TextMuted = Color(0xFFBFAAA0) // Soft readable muted text

// Chips & Containers
val ChipActiveBackground = Color(0xFFEED3BA)
val ChipActiveContent = Color(0xFF151311)
val ChipInactiveBackground = Color(0xFF381F26)
val ChipInactiveContent = Color(0xFFFAF0E6)
val ChipInactiveBorder = Color(0xFF633A44)

// Backwards Compatibility Aliases
val Neutral900 = TextHighContrast
val Neutral800 = ObsidianElevated
val Neutral700 = TextMediumContrast
val Neutral500 = TextMuted
val Neutral300 = AlmondHearthMuted
val Neutral200 = VelvetCurfewLight
val Neutral100 = ChipInactiveBackground
val Neutral50  = AlmondHearth

// Status & Accents
val BrandAmber = Color(0xFFF59E0B)
val BrandGreen = Color(0xFF22C55E)
val BrandRed   = Color(0xFFEF4444)

// Gradients
val SnapTaskVerticalGradient = Brush.verticalGradient(
    colors = listOf(
        ObsidianInk,
        VelvetCurfew,
        Color(0xFF2C161D)
    )
)

val SnapTaskCardGradient = Brush.linearGradient(
    colors = listOf(
        VelvetCurfew,
        ObsidianInk
    )
)
