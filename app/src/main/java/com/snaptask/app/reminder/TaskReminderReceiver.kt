package com.snaptask.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.snaptask.app.R
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.ui.MainActivity
import com.snaptask.app.widget.WidgetBitmapHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_NOTIFICATION = "snaptask_channel_notification"
        const val CHANNEL_ALARM = "snaptask_channel_alarm"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // When device reboots, restore all pending reminders
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val pending = db.taskDao().getPendingTasksForWidget()
                for (entity in pending) {
                    val task = entity.toDomain()
                    if (task.hasReminder) {
                        TaskReminderScheduler.scheduleReminder(context, task)
                    }
                }
            }
            return
        }

        val taskId = intent.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val taskTitle = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TITLE) ?: "Việc cần làm"
        val taskImage = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_IMAGE) ?: ""
        val reminderType = intent.getStringExtra(TaskReminderScheduler.EXTRA_REMINDER_TYPE) ?: "NOTIFICATION"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val isAlarmMode = reminderType == "ALARM"
        val channelId = if (isAlarmMode) CHANNEL_ALARM else CHANNEL_NOTIFICATION

        // 1. Content Intent: Click notification opens app to task
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (taskId * 10).toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Complete Task Directly
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: Snooze 10 Minutes
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderScheduler.EXTRA_TASK_TITLE, taskTitle)
            putExtra(TaskReminderScheduler.EXTRA_TASK_IMAGE, taskImage)
            putExtra(TaskReminderScheduler.EXTRA_REMINDER_TYPE, reminderType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Load Task Before Photo for BigPictureStyle
        val photoBitmap = WidgetBitmapHelper.loadOptimizedBitmap(taskImage, maxSize = 600)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(if (isAlarmMode) "⏰ BÁO THỨC: $taskTitle" else "🔔 Đến giờ làm: $taskTitle")
            .setContentText("Chạm để chụp ảnh hoàn thành (After Proof)")
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "✅ Hoàn thành nhanh", completePendingIntent)
            .addAction(0, "💤 Báo lại sau 10p", snoozePendingIntent)

        if (photoBitmap != null) {
            builder.setLargeIcon(photoBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(photoBitmap)
                    .setSummaryText(if (isAlarmMode) "⏰ Báo thức công việc" else "📸 Nhiệm vụ SnapTask")
            )
        }

        if (isAlarmMode) {
            builder.priority = NotificationCompat.PRIORITY_MAX
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setOngoing(true)
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            builder.setVibrate(longArrayOf(0, 600, 300, 600, 300, 800))
        } else {
            builder.priority = NotificationCompat.PRIORITY_HIGH
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
        }

        notificationManager.notify(taskId.toInt(), builder.build())
    }

    private fun createChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Notification Channel
            val notifChannel = NotificationChannel(
                CHANNEL_NOTIFICATION,
                "Nhắc nhở công việc (Notification)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở nhẹ nhàng về các nhiệm vụ cần thực hiện"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(notifChannel)

            // Alarm Channel
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Chuông báo thức công việc (Alarm)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chuông báo thức lớn nhắc nhở nhiệm vụ khẩn cấp"
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 800)
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }
}
