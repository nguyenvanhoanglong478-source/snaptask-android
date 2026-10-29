package com.snaptask.app.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_NOTIFICATION = "snaptask_channel_notification"
        const val CHANNEL_ALARM = "snaptask_channel_alarm_v3"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReminder(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context, intent: Intent) {
        // Read task reminder data directly from intent extras
        val taskId = intent.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        // Verify task exists and is not completed before alerting
        val db = AppDatabase.getInstance(context)
        val taskEntity = db.taskDao().getTaskById(taskId)
        if (taskEntity == null || taskEntity.isCompleted) {
            return
        }

        val taskTitle = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TITLE) ?: "Việc cần làm"
        val taskDesc = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_DESC) ?: ""
        val taskImage = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_IMAGE) ?: ""
        val reminderType = intent.getStringExtra(TaskReminderScheduler.EXTRA_REMINDER_TYPE) ?: "NOTIFICATION"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val isAlarmMode = reminderType == "ALARM"
        val channelId = if (isAlarmMode) CHANNEL_ALARM else CHANNEL_NOTIFICATION

        // 1. Content Intent: Click notification opens app directly to this task
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (taskId * 100 + 2).toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Stop Alarm sound (Only for ALARM mode)
        val stopAlarmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP_ALARM
            putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        val stopAlarmPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 100 + 3).toInt(),
            stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: Complete Task Directly
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 100 + 4).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Action: Snooze 10 Minutes
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderScheduler.EXTRA_TASK_TITLE, taskTitle)
            putExtra(TaskReminderScheduler.EXTRA_TASK_DESC, taskDesc)
            putExtra(TaskReminderScheduler.EXTRA_TASK_IMAGE, taskImage)
            putExtra(TaskReminderScheduler.EXTRA_REMINDER_TYPE, reminderType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 100 + 5).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Optional: BigPicture before photo preview
        val photoBitmap = WidgetBitmapHelper.loadOptimizedBitmap(taskImage, maxSize = 600)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_snaptask)
            .setColor(0xFFFFD23F.toInt())
            .setContentTitle(if (isAlarmMode) "Báo thức: $taskTitle" else "Đến giờ làm: $taskTitle")
            .setContentText(if (taskDesc.isNotBlank()) taskDesc else "Chạm để chụp ảnh hoàn thành (After Proof)")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (photoBitmap != null) {
            builder.setLargeIcon(photoBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(photoBitmap)
                    .setSummaryText(if (isAlarmMode) "Báo thức công việc" else "Nhiệm vụ SnapTask")
            )
        }

        if (isAlarmMode) {
            // Priority & FullScreen for Alarm
            builder.priority = NotificationCompat.PRIORITY_MAX
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setOngoing(true)
            builder.setAutoCancel(false)
            builder.setFullScreenIntent(contentPendingIntent, true)
            builder.setTimeoutAfter(60_000)

            builder.addAction(0, "Tắt chuông", stopAlarmPendingIntent)
            builder.addAction(0, "Hoàn thành", completePendingIntent)
            builder.addAction(0, "Báo lại 10p", snoozePendingIntent)
        } else {
            builder.priority = NotificationCompat.PRIORITY_HIGH
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
            builder.setAutoCancel(true)

            builder.addAction(0, "Hoàn thành nhanh", completePendingIntent)
            builder.addAction(0, "Báo lại sau 10p", snoozePendingIntent)
        }

        val notification = builder.build()
        if (isAlarmMode) {
            notification.flags = notification.flags or Notification.FLAG_INSISTENT
        }
        notificationManager.notify(taskId.toInt(), notification)
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
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(notifChannel)

            // Delete legacy alarm channels
            notificationManager.deleteNotificationChannel("snaptask_channel_alarm")
            notificationManager.deleteNotificationChannel("snaptask_channel_alarm_v2")

            // Alarm Channel V3: System-managed loud alarm audio with USAGE_ALARM
            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Chuông báo thức công việc (Alarm)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chuông báo thức lớn nhắc nhở nhiệm vụ khẩn cấp"
                setSound(alarmSoundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 1000)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }
}
