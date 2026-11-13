package com.snaptask.app.reminder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskReminderSchedulerTest {

    private val fixedNow = 1_700_000_000_000L

    @Test
    fun `shouldScheduleReminder returns false when time is null past or equal to now`() {
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = null,
                isCompleted = false,
                reminderType = "NOTIFICATION",
                nowMillis = fixedNow
            )
        )
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = fixedNow - 1L,
                isCompleted = false,
                reminderType = "NOTIFICATION",
                nowMillis = fixedNow
            )
        )
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = fixedNow,
                isCompleted = false,
                reminderType = "NOTIFICATION",
                nowMillis = fixedNow
            )
        )
    }

    @Test
    fun `shouldScheduleReminder returns false when task is completed`() {
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = fixedNow + 60_000L,
                isCompleted = true,
                reminderType = "NOTIFICATION",
                nowMillis = fixedNow
            )
        )
    }

    @Test
    fun `shouldScheduleReminder returns false when reminderType is null blank or NONE`() {
        val futureTime = fixedNow + 60_000L

        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = null,
                nowMillis = fixedNow
            )
        )
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = "",
                nowMillis = fixedNow
            )
        )
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = "   ",
                nowMillis = fixedNow
            )
        )
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = "NONE",
                nowMillis = fixedNow
            )
        )
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = "none",
                nowMillis = fixedNow
            )
        )
        assertFalse(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = "  None  ",
                nowMillis = fixedNow
            )
        )
    }

    @Test
    fun `shouldScheduleReminder returns true for incomplete task with future active reminder`() {
        val futureTime = fixedNow + 60_000L

        assertTrue(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = "NOTIFICATION",
                nowMillis = fixedNow
            )
        )
        assertTrue(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = "ALARM",
                nowMillis = fixedNow
            )
        )
        assertTrue(
            TaskReminderScheduler.shouldScheduleReminder(
                reminderTime = futureTime,
                isCompleted = false,
                reminderType = " notification ",
                nowMillis = fixedNow
            )
        )
    }
}
