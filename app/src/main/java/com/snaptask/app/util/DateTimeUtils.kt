package com.snaptask.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun formatTaskDuration(createdAt: Long, completedAt: Long?): String {
        val end = completedAt ?: System.currentTimeMillis()
        val diffMs = (end - createdAt).coerceAtLeast(0)
        val minutes = diffMs / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> {
                val remHours = hours % 24
                if (remHours > 0) "${days}d ${remHours}h" else "${days}d"
            }
            hours > 0 -> {
                val remMinutes = minutes % 60
                if (remMinutes > 0) "${hours}h ${remMinutes}m" else "${hours}h"
            }
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
