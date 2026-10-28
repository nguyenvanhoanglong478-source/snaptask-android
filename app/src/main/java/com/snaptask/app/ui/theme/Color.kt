package com.snaptask.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// MIDNIGHT GLASS & AURORA DESIGN SYSTEM
// Inspired by Linear, Craft & MyMind
// ==========================================

// 1. Core Background & Surface Hierarchy (Deep Obsidian)
val BgPrimary       = Color(0xFF0B0C10) // Main Screen Background
val BgSurface       = Color(0xFF14161F) // Cards, Sheets & Modal Containers
val BgElevated      = Color(0xFF1E212D) // Chips, Input fields, Secondary elements
val BorderSubtle    = Color(0xFF282C3A) // Crisp 1dp Architectural Borders
val BorderHighlight = Color(0xFF3B4154) // Focus & Active Borders

// 2. Aurora & Electric Indigo Accents
val AccentPrimary   = Color(0xFF6366F1) // Electric Indigo (Primary Brand Color)
val AccentSecondary = Color(0xFF8B5CF6) // Violet (Gradient Partner)
val AccentSubtle    = Color(0x336366F1) // 20% Indigo Tint for Active Chips

// Gradients
val AuroraGradient = Brush.linearGradient(
    colors = listOf(AccentPrimary, AccentSecondary)
)

val AuroraDockGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF6366F1), Color(0xFF9333EA))
)

val CardBottomScrim = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color(0x660B0C10),
        Color(0xF20B0C10)
    )
)

val GlassmorphismDark = Color(0xE614161F) // 90% Opacity #14161F

// 3. High-Contrast Typography
val TextPrimary   = Color(0xFFF8FAFC) // 18.5:1 Contrast on #0B0C10
val TextSecondary = Color(0xFF94A3B8) // 7.8:1 Contrast on #0B0C10
val TextMuted     = Color(0xFF64748B) // 4.8:1 Contrast on #0B0C10

// 4. Semantic Status Accents
val StatusSuccess = Color(0xFF10B981) // Emerald Mint (Completed & After proof)
val StatusWarning = Color(0xFFF59E0B) // Amber Gold (Medium Priority & Reminders)
val StatusAmber   = StatusWarning
val StatusUrgent  = Color(0xFFF43F5E) // Rose Crimson (High Priority & Alarms)
val StatusDanger  = StatusUrgent

// 5. Chips & Interactive States
val ChipActiveBg       = AccentPrimary
val ChipActiveText     = Color(0xFFFFFFFF)
val ChipInactiveBg     = BgElevated
val ChipInactiveBorder = BorderSubtle
val ChipInactiveText   = TextSecondary

// ==========================================
// BACKWARD COMPATIBILITY ALIASES
// (Ensures legacy screens compile smoothly)
// ==========================================
val NotionDarkBg           = BgPrimary
val NotionCardSurface      = BgSurface
val NotionBorder           = BorderSubtle
val LocketAccent           = AccentPrimary
val LocketAccentMuted      = AccentSecondary
val ObsidianInk            = BgPrimary
val ObsidianElevated       = BgSurface
val VelvetCurfew           = BgSurface
val VelvetCurfewLight      = BgElevated
val AlmondHearth           = AccentPrimary
val TextHighContrast       = TextPrimary
val TextMediumContrast     = TextSecondary
val Neutral900             = TextPrimary
val Neutral800             = BgSurface
val Neutral700             = TextSecondary
val Neutral500             = TextMuted
val Neutral100             = BgElevated
val Neutral50              = AccentPrimary
val BrandAmber             = StatusWarning
val BrandGreen             = StatusSuccess
val BrandRed               = StatusUrgent
val ChipActiveBackground   = ChipActiveBg
val ChipActiveContent      = ChipActiveText
val ChipInactiveBackground = ChipInactiveBg
val ChipInactiveContent    = ChipInactiveText
val NotionLocketGradient   = AuroraGradient
val SnapTaskVerticalGradient = AuroraGradient
