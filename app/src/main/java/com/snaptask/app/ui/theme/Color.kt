package com.snaptask.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Velvet & Obsidian Theme (Inspired by user gradient palette)
val ObsidianInk  = Color(0xFF151311) // Rich deep obsidian
val VelvetCurfew = Color(0xFF4B262F) // Deep wine / plum burgundy
val AlmondHearth = Color(0xFFEED3BA) // Warm creamy almond

// Complementary shades
val VelvetCurfewLight = Color(0xFF6B3843)
val AlmondHearthMuted = Color(0xFFD8BC9F)
val ObsidianElevated  = Color(0xFF221F1C)

// Theme Mappings
val Neutral900 = ObsidianInk
val Neutral800 = ObsidianElevated
val Neutral700 = VelvetCurfew
val Neutral500 = AlmondHearthMuted
val Neutral300 = AlmondHearthMuted
val Neutral200 = VelvetCurfewLight
val Neutral100 = Color(0xFF2E171D)
val Neutral50  = AlmondHearth

// Gradients
val SnapTaskVerticalGradient = Brush.verticalGradient(
    colors = listOf(
        ObsidianInk,
        VelvetCurfew,
        AlmondHearth
    )
)

val SnapTaskCardGradient = Brush.linearGradient(
    colors = listOf(
        VelvetCurfew,
        ObsidianInk
    )
)

val BrandAmber = Color(0xFFD97706)
val BrandGreen = Color(0xFF15803D)
val BrandRed   = Color(0xFFDC2626)
