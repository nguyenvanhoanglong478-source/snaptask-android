package com.snaptask.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

object WidgetColors {
    val bg = Color(0xFF1B1A17)
    val elevated = Color(0xFF26241F)
    val text = Color(0xFFF5F1E8)
    val textSecondary = Color(0xFFB3AC9E)
    val accent = Color(0xFFFFD23F) // flash
    val onAccent = Color(0xFF17140A) // onFlash
    val urgent = Color(0xFFFF7A6B)

    val bgProvider = ColorProvider(bg)
    val elevatedProvider = ColorProvider(elevated)
    val textProvider = ColorProvider(text)
    val textSecondaryProvider = ColorProvider(textSecondary)
    val accentProvider = ColorProvider(accent)
    val onAccentProvider = ColorProvider(onAccent)
    val urgentProvider = ColorProvider(urgent)
}
