package com.snaptask.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isSameDay(aMillis: Long, bMillis: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = aMillis }
        val calB = Calendar.getInstance().apply { timeInMillis = bMillis }
        return calA.get(Calendar.ERA) == calB.get(Calendar.ERA) &&
            calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    fun isOverdue(
        dueDate: Long?,
        isCompleted: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return dueDate != null && !isCompleted && dueDate < nowMillis
    }

    fun formatDueLabel(
        dueDate: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (isSameDay(dueDate, nowMillis)) {
            return "Hôm nay ${formatTime(dueDate)}"
        }

        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        if (isSameDay(dueDate, tomorrowCal.timeInMillis)) {
            return "Ngày mai ${formatTime(dueDate)}"
        }

        val dueCal = Calendar.getInstance().apply { timeInMillis = dueDate }
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val sameYear = dueCal.get(Calendar.ERA) == nowCal.get(Calendar.ERA) &&
            dueCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)

        val pattern = if (sameYear) "dd/MM · HH:mm" else "dd/MM/yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(dueDate))
    }

    /**
     * Formats the remaining offset until a reminder fires as a concise Vietnamese label
     * (e.g. `"sau 30 giây"`, `"sau 15 phút"`, `"sau 2 giờ 10 phút"`, `"sau 2 ngày (15/11/2026 09:00)"`),
     * returning `""` when the reminder is disabled, missing, or already in the past.
     */
    fun formatReminderOffsetLabel(
        reminderTime: Long?,
        hasReminder: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (!hasReminder || reminderTime == null) return ""
        val diffMs = reminderTime - nowMillis
        if (diffMs <= 0L) return ""

        val totalSeconds = (diffMs / 1000L).coerceAtLeast(1L)
        if (totalSeconds < 60L) {
            return "sau $totalSeconds giây"
        }

        val totalMinutes = totalSeconds / 60L
        if (totalMinutes < 60L) {
            return "sau $totalMinutes phút"
        }

        val totalHours = totalMinutes / 60L
        val remMinutes = totalMinutes % 60L
        if (totalHours < 24L) {
            return if (remMinutes > 0L) {
                "sau $totalHours giờ $remMinutes phút"
            } else {
                "sau $totalHours giờ"
            }
        }

        val days = totalHours / 24L
        return "sau $days ngày (${formatDateTime(reminderTime)})"
    }
}
