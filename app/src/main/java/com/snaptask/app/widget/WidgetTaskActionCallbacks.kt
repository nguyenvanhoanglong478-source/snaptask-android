package com.snaptask.app.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.reminder.TaskReminderScheduler
import com.snaptask.app.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenTaskActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskIdKey = ActionParameters.Key<Long>("taskId")
        val taskId = parameters[taskIdKey]
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (taskId != null && taskId > 0L) {
                putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
            }
        }
        runCatching { context.startActivity(intent) }
    }
}

class CompleteTaskActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskIdKey = ActionParameters.Key<Long>("taskId")
        val taskId = parameters[taskIdKey] ?: return

        withContext(Dispatchers.IO) {
            runCatching {
                val database = AppDatabase.getInstance(context)
                database.taskDao().toggleCompleted(taskId, true, System.currentTimeMillis())
                TaskReminderScheduler.cancelReminder(context, taskId)
            }
        }

        SnapTaskWidgetUpdater.updateAllWidgets(context)
    }
}

class NextTaskActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val database = AppDatabase.getInstance(context)
                val pendingTasks = database.taskDao().getPendingTasksForWidget()
                if (pendingTasks.isNotEmpty()) {
                    val prefs = context.getSharedPreferences("snaptask_widget_prefs", Context.MODE_PRIVATE)
                    val currentOffset = prefs.getInt("locket_offset", 0)
                    val nextOffset = (currentOffset + 1) % pendingTasks.size
                    prefs.edit().putInt("locket_offset", nextOffset).apply()
                }
            }
        }

        runCatching { LocketFocusWidget().updateAll(context) }
        runCatching { SnapTaskWidget().updateAll(context) }
    }
}
