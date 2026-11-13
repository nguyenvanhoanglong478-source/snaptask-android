package com.snaptask.app.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.MainActivity

object TaskReminderScheduler {

    const val ACTION_TRIGGER_REMINDER_PREFIX = "com.snaptask.app.ACTION_TRIGGER_REMINDER_"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_DESC = "extra_task_desc"
    const val EXTRA_TASK_IMAGE = "extra_task_image"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"

    fun getActionForTask(taskId: Long): String = "$ACTION_TRIGGER_REMINDER_PREFIX$taskId"

    fun shouldScheduleReminder(
        reminderTime: Long?,
        isCompleted: Boolean,
        reminderType: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (reminderTime == null || reminderTime <= nowMillis) return false
        if (isCompleted) return false
        val normalizedType = reminderType?.trim().orEmpty()
        return normalizedType.isNotEmpty() && !normalizedType.equals("NONE", ignoreCase = true)
    }

    fun scheduleReminder(context: Context, task: Task) {
        val triggerTime = task.reminderTime
        if (triggerTime == null || !shouldScheduleReminder(triggerTime, task.isCompleted, task.reminderType)) {
            cancelReminder(context, task.id)
            return
        }

        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Intent targeted explicitly at TaskReminderReceiver with unique action
        val intent = Intent(appContext, TaskReminderReceiver::class.java).apply {
            action = getActionForTask(task.id)
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_DESC, task.description)
            putExtra(EXTRA_TASK_IMAGE, task.beforeImageUri)
            putExtra(EXTRA_REMINDER_TYPE, task.reminderType)
        }

        val receiverPendingIntent = PendingIntent.getBroadcast(
            appContext,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show App Intent: Opened when user taps the alarm icon on system lock screen
        val showAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val showAppPendingIntent = PendingIntent.getActivity(
            appContext,
            (task.id * 100 + 1).toInt(),
            showAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showAppPendingIntent)

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)

        if (canScheduleExact) {
            val scheduledExact = try {
                // Priority #1: setAlarmClock (bypasses Doze mode, power saving, and manufacturer task killers)
                alarmManager.setAlarmClock(alarmClockInfo, receiverPendingIntent)
                true
            } catch (_: SecurityException) {
                try {
                    // Priority #2 Fallback: setExactAndAllowWhileIdle
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, receiverPendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, receiverPendingIntent)
                    }
                    true
                } catch (_: SecurityException) {
                    false
                }
            }
            if (scheduledExact) return
        }

        // Priority #3 Fallback: Doze-compatible inexact alarm when exact alarm permission is denied/revoked
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, receiverPendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, receiverPendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, receiverPendingIntent)
            } catch (_: SecurityException) {
                // Ignore if device policy completely blocks alarms
            }
        }
    }

    fun cancelReminder(context: Context, taskId: Long) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(appContext, TaskReminderReceiver::class.java).apply {
            action = getActionForTask(taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager?.cancel(it)
            it.cancel()
        }

        // Dismiss active notifications for this task
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(taskId.toInt())
    }
}
