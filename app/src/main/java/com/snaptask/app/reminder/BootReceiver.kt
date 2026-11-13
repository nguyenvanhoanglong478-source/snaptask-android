package com.snaptask.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.widget.SnapTaskWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appContext = context.applicationContext
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(appContext)
                    val now = System.currentTimeMillis()
                    val pendingWithReminder = db.taskDao().getPendingTasksWithFutureReminder(now)
                    for (entity in pendingWithReminder) {
                        TaskReminderScheduler.scheduleReminder(appContext, entity.toDomain())
                    }
                    SnapTaskWidgetUpdater.updateAllWidgets(appContext)
                } catch (_: Exception) {
                    // Guard boot receiver against transient storage/alarm failures
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
