package com.snaptask.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Notion Minimalist Dark + Locket Warm Accent
val NotionDarkBg       = Color(0xFF121212) // Main Background
val NotionCardSurface  = Color(0xFF1E1E1E) // Card Surface
val NotionBorder       = Color(0xFF2E2E2E) // Subtle Border
val LocketAccent       = Color(0xFFFF6B4A) // Locket Coral Orange Accent
val LocketAccentMuted  = Color(0xFFD45538)
val GlassmorphismDark  = Color(0xE6141414) // 90% opacity #141414 for robust contrast on photo overlays

// High-Contrast Typography (Verified with WCAG AA 4.5:1+)
val TextPrimary        = Color(0xFFFFFFFF) // 18.2:1 on #121212
val TextSecondary      = Color(0xFFA6A6A6) // 7.9:1 on #121212, 7.3:1 on #1E1E1E
val TextMuted          = Color(0xFF8E8E93) // 5.6:1 on #121212, 5.1:1 on #1E1E1E

// Chips & Badges
val ChipActiveBg       = Color(0xFFFF6B4A)
val ChipActiveText     = Color(0xFF121212) // 6.65:1 on #FF6B4A (PASS AA & AAA)
val ChipInactiveBg     = Color(0xFF242424)
val ChipInactiveText   = Color(0xFFD6D6D6) // 10.68:1 on #242424 (PASS AA & AAA)
val ChipInactiveBorder = Color(0xFF383838)

// Status Colors
val StatusSuccess      = Color(0xFF22C55E)
val StatusWarning      = Color(0xFFF59E0B)
val StatusAmber        = StatusWarning
val StatusDanger       = Color(0xFFEF4444)

// Backwards Compatibility Aliases
val ObsidianInk            = NotionDarkBg
val ObsidianElevated       = NotionCardSurface
val VelvetCurfew           = NotionCardSurface
val VelvetCurfewLight      = NotionCardSurface
val AlmondHearth           = LocketAccent
val TextHighContrast       = TextPrimary
val TextMediumContrast     = TextSecondary
val Neutral900             = TextPrimary
val Neutral800             = NotionCardSurface
val Neutral700             = TextSecondary
val Neutral500             = TextMuted
val Neutral100             = ChipInactiveBg
val Neutral50              = LocketAccent
val BrandAmber             = StatusWarning
val BrandGreen             = StatusSuccess
val BrandRed               = StatusDanger
val ChipActiveBackground   = ChipActiveBg
val ChipActiveContent      = ChipActiveText
val ChipInactiveBackground = ChipInactiveBg
val ChipInactiveContent    = ChipInactiveText

// Gradients
val NotionLocketGradient = Brush.verticalGradient(
    colors = listOf(
        NotionDarkBg,
        Color(0xFF1A1A1A),
        NotionDarkBg
    )
)

val SnapTaskVerticalGradient = NotionLocketGradient
