package com.snaptask.app.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.widget.SnapTaskWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE_TASK = "com.snaptask.app.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.snaptask.app.ACTION_SNOOZE_TASK"
        const val ACTION_STOP_ALARM = "com.snaptask.app.ACTION_STOP_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (intent.action) {
            ACTION_STOP_ALARM -> {
                val wasActive = runCatching {
                    notificationManager?.activeNotifications?.any { it.id == taskId.toInt() } ?: true
                }.getOrDefault(true)
                notificationManager?.cancel(taskId.toInt())
                if (wasActive) {
                    Toast.makeText(appContext, "Đã tắt chuông báo thức!", Toast.LENGTH_SHORT).show()
                }
            }

            ACTION_COMPLETE_TASK -> {
                notificationManager?.cancel(taskId.toInt())
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(appContext)
                        val existing = db.taskDao().getTaskById(taskId)
                        val newlyCompleted = existing != null && !existing.isCompleted
                        if (newlyCompleted) {
                            db.taskDao().toggleCompleted(taskId, true, System.currentTimeMillis())
                        }
                        TaskReminderScheduler.cancelReminder(appContext, taskId)

                        // Refresh all home screen widgets
                        SnapTaskWidgetUpdater.updateAllWidgets(appContext)

                        if (newlyCompleted) {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(appContext, "Đã hoàn thành công việc!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (_: Exception) {
                        // Guard background broadcast receiver coroutine
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE_TASK -> {
                notificationManager?.cancel(taskId.toInt())
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(appContext)
                        val taskEntity = db.taskDao().getTaskById(taskId)
                        if (taskEntity == null || taskEntity.isCompleted) {
                            return@launch
                        }

                        val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000L // +10 minutes
                        val fallbackType = intent.getStringExtra(TaskReminderScheduler.EXTRA_REMINDER_TYPE) ?: "NOTIFICATION"
                        val type = if (!taskEntity.reminderType.equals("NONE", ignoreCase = true)) {
                            taskEntity.reminderType
                        } else {
                            fallbackType
                        }

                        db.taskDao().updateTaskReminder(taskId, snoozeTime, type)

                        val updatedTask = taskEntity.copy(reminderTime = snoozeTime, reminderType = type).toDomain()
                        TaskReminderScheduler.scheduleReminder(appContext, updatedTask)
                        SnapTaskWidgetUpdater.updateAllWidgets(appContext)

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(appContext, "Sẽ nhắc lại sau 10 phút!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        // Guard background broadcast receiver coroutine
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
