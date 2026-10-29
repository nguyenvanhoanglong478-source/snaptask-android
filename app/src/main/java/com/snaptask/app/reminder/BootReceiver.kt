package com.snaptask.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snaptask.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val now = System.currentTimeMillis()
                    val pendingWithReminder = db.taskDao().getPendingTasksWithFutureReminder(now)
                    for (entity in pendingWithReminder) {
                        TaskReminderScheduler.scheduleReminder(context, entity.toDomain())
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
