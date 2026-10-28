package com.snaptask.app.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.glance.appwidget.updateAll
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.domain.model.Task
import com.snaptask.app.widget.LocketFocusWidget
import com.snaptask.app.widget.NotionStackWidget
import com.snaptask.app.widget.SnapTaskWidget
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Always silence any ringing alarm sound/vibration immediately
        AlarmSoundPlayer.stop()

        when (intent.action) {
            ACTION_STOP_ALARM -> {
                notificationManager.cancel(taskId.toInt())
                Toast.makeText(context, "Đã tắt chuông báo thức! 🔕", Toast.LENGTH_SHORT).show()
            }

            ACTION_COMPLETE_TASK -> {
                notificationManager.cancel(taskId.toInt())
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        db.taskDao().toggleCompleted(taskId, true, System.currentTimeMillis())
                        TaskReminderScheduler.cancelReminder(context, taskId)

                        // Refresh all home screen widgets
                        try {
                            SnapTaskWidget().updateAll(context)
                            LocketFocusWidget().updateAll(context)
                            NotionStackWidget().updateAll(context)
                        } catch (_: Exception) {}

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Đã hoàn thành công việc! ✅", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE_TASK -> {
                notificationManager.cancel(taskId.toInt())

                val title = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TITLE) ?: "Việc cần làm"
                val desc = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_DESC) ?: ""
                val image = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_IMAGE) ?: ""
                val type = intent.getStringExtra(TaskReminderScheduler.EXTRA_REMINDER_TYPE) ?: "NOTIFICATION"

                val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000L // +10 minutes

                val snoozedTask = Task(
                    id = taskId,
                    title = title,
                    description = desc,
                    beforeImageUri = image,
                    reminderTime = snoozeTime,
                    reminderType = type
                )

                TaskReminderScheduler.scheduleReminder(context, snoozedTask)
                Toast.makeText(context, "Sẽ nhắc lại sau 10 phút! 💤", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
