package com.snaptask.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

object WidgetColors {
    val bg = Color(0xFF1A1A18)
    val elevated = Color(0xFF252522)
    val text = Color(0xFFFAFAF6)
    val textSecondary = Color(0xFFB0AFA7)
    val textMuted = Color(0xFF8E8D85)
    val accent = Color(0xFFD4FF3A)
    val onAccent = Color(0xFF10120A)
    val urgent = Color(0xFFFF5C4D)
    val onUrgent = Color(0xFF10120A)

    val bgProvider = ColorProvider(bg)
    val elevatedProvider = ColorProvider(elevated)
    val textProvider = ColorProvider(text)
    val textSecondaryProvider = ColorProvider(textSecondary)
    val textMutedProvider = ColorProvider(textMuted)
    val accentProvider = ColorProvider(accent)
    val onAccentProvider = ColorProvider(onAccent)
    val urgentProvider = ColorProvider(urgent)
    val onUrgentProvider = ColorProvider(onUrgent)
}
