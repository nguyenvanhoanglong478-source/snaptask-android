package com.snaptask.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateTimeUtilsTest {

    private fun localMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millisecond: Int = 0
    ): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, second)
            set(Calendar.MILLISECOND, millisecond)
        }.timeInMillis
    }

    @Test
    fun `isSameDay handles start and end of day next day and different year`() {
        val startOfDay = localMillis(2026, Calendar.JUNE, 15, 0, 0, 0, 0)
        val endOfDay = localMillis(2026, Calendar.JUNE, 15, 23, 59, 59, 999)
        val nextDayStart = localMillis(2026, Calendar.JUNE, 16, 0, 0, 0, 0)
        val nextYearSameDate = localMillis(2027, Calendar.JUNE, 15, 12, 0, 0, 0)

        assertTrue(DateTimeUtils.isSameDay(startOfDay, endOfDay))
        assertFalse(DateTimeUtils.isSameDay(endOfDay, nextDayStart))
        assertFalse(DateTimeUtils.isSameDay(startOfDay, nextYearSameDate))
    }

    @Test
    fun `isOverdue returns true only for past due incomplete tasks`() {
        val now = localMillis(2026, Calendar.JUNE, 15, 10, 0)
        val pastDue = now - 60_000L
        val futureDue = now + 60_000L

        assertTrue(DateTimeUtils.isOverdue(dueDate = pastDue, isCompleted = false, nowMillis = now))
        assertFalse(DateTimeUtils.isOverdue(dueDate = pastDue, isCompleted = true, nowMillis = now))
        assertFalse(DateTimeUtils.isOverdue(dueDate = now, isCompleted = false, nowMillis = now))
        assertFalse(DateTimeUtils.isOverdue(dueDate = futureDue, isCompleted = false, nowMillis = now))
        assertFalse(DateTimeUtils.isOverdue(dueDate = null, isCompleted = false, nowMillis = now))
        assertFalse(DateTimeUtils.isOverdue(dueDate = null, isCompleted = true, nowMillis = now))
    }

    @Test
    fun `formatDueLabel formats today tomorrow same year and different year labels`() {
        val now = localMillis(2026, Calendar.JUNE, 15, 9, 30)

        val todayEvening = localMillis(2026, Calendar.JUNE, 15, 18, 5)
        val todayMidnight = localMillis(2026, Calendar.JUNE, 15, 0, 0)
        val tomorrowMorning = localMillis(2026, Calendar.JUNE, 16, 8, 45)
        val sameYearLater = localMillis(2026, Calendar.JUNE, 18, 14, 20)
        val sameYearYesterday = localMillis(2026, Calendar.JUNE, 14, 20, 0)
        val nextYear = localMillis(2027, Calendar.JANUARY, 5, 11, 0)
        val previousYear = localMillis(2025, Calendar.DECEMBER, 25, 16, 0)

        assertEquals("Hôm nay 18:05", DateTimeUtils.formatDueLabel(todayEvening, now))
        assertEquals("Hôm nay 00:00", DateTimeUtils.formatDueLabel(todayMidnight, now))
        assertEquals("Ngày mai 08:45", DateTimeUtils.formatDueLabel(tomorrowMorning, now))
        assertEquals("18/06 · 14:20", DateTimeUtils.formatDueLabel(sameYearLater, now))
        assertEquals("14/06 · 20:00", DateTimeUtils.formatDueLabel(sameYearYesterday, now))
        assertEquals("05/01/2027", DateTimeUtils.formatDueLabel(nextYear, now))
        assertEquals("25/12/2025", DateTimeUtils.formatDueLabel(previousYear, now))
    }

    @Test
    fun `formatDueLabel handles year rollover for tomorrow on New Years Eve`() {
        val dec31 = localMillis(2026, Calendar.DECEMBER, 31, 22, 0)
        val jan1NextYear = localMillis(2027, Calendar.JANUARY, 1, 7, 15)

        assertEquals("Ngày mai 07:15", DateTimeUtils.formatDueLabel(jan1NextYear, dec31))
    }

    @Test
    fun `formatTime and formatDateTime format consistently`() {
        val morning = localMillis(2026, Calendar.MARCH, 5, 7, 9)
        val night = localMillis(2026, Calendar.MARCH, 5, 23, 59)

        assertEquals("07:09", DateTimeUtils.formatTime(morning))
        assertEquals("23:59", DateTimeUtils.formatTime(night))
        assertEquals("05/03/2026 07:09", DateTimeUtils.formatDateTime(morning))
        assertEquals("05/03/2026 23:59", DateTimeUtils.formatDateTime(night))
        assertEquals("Hôm nay ${DateTimeUtils.formatTime(morning)}", DateTimeUtils.formatDueLabel(morning, morning))
        assertTrue(DateTimeUtils.formatDateTime(morning).endsWith(" ${DateTimeUtils.formatTime(morning)}"))
    }

    @Test
    fun `formatReminderOffsetLabel covers disabled past seconds minutes hours and days`() {
        val fixedNow = localMillis(2026, Calendar.JUNE, 15, 9, 30)

        // Disabled, null, equal, or past -> empty string
        assertEquals("", DateTimeUtils.formatReminderOffsetLabel(fixedNow + 60_000L, hasReminder = false, nowMillis = fixedNow))
        assertEquals("", DateTimeUtils.formatReminderOffsetLabel(null, hasReminder = true, nowMillis = fixedNow))
        assertEquals("", DateTimeUtils.formatReminderOffsetLabel(fixedNow, hasReminder = true, nowMillis = fixedNow))
        assertEquals("", DateTimeUtils.formatReminderOffsetLabel(fixedNow - 1_000L, hasReminder = true, nowMillis = fixedNow))

        // Seconds (< 60s), including sub-second rounding up to 1s minimum
        assertEquals("sau 1 giây", DateTimeUtils.formatReminderOffsetLabel(fixedNow + 500L, hasReminder = true, nowMillis = fixedNow))
        assertEquals("sau 45 giây", DateTimeUtils.formatReminderOffsetLabel(fixedNow + 45_000L, hasReminder = true, nowMillis = fixedNow))

        // Minutes (< 60m)
        assertEquals("sau 1 phút", DateTimeUtils.formatReminderOffsetLabel(fixedNow + 60_000L, hasReminder = true, nowMillis = fixedNow))
        assertEquals("sau 15 phút", DateTimeUtils.formatReminderOffsetLabel(fixedNow + 15 * 60_000L, hasReminder = true, nowMillis = fixedNow))

        // Exact hours (< 24h) and hours + minutes
        assertEquals("sau 3 giờ", DateTimeUtils.formatReminderOffsetLabel(fixedNow + 3 * 3_600_000L, hasReminder = true, nowMillis = fixedNow))
        assertEquals(
            "sau 2 giờ 10 phút",
            DateTimeUtils.formatReminderOffsetLabel(fixedNow + 2 * 3_600_000L + 10 * 60_000L, hasReminder = true, nowMillis = fixedNow)
        )

        // >= 24h -> includes formatted target date/time
        val twoDaysTarget = localMillis(2026, Calendar.JUNE, 17, 9, 30)
        val twoDaysBase = twoDaysTarget - 2L * 24L * 3_600_000L
        assertEquals(
            "sau 2 ngày (17/06/2026 09:30)",
            DateTimeUtils.formatReminderOffsetLabel(twoDaysTarget, hasReminder = true, nowMillis = twoDaysBase)
        )
    }
}
