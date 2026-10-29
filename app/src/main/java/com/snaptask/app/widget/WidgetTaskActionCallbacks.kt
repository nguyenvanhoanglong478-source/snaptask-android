package com.snaptask.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.reminder.TaskReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompleteTaskActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskIdKey = ActionParameters.Key<Long>("taskId")
        val taskId = parameters[taskIdKey] ?: return

        withContext(Dispatchers.IO) {
            val database = AppDatabase.getInstance(context)
            database.taskDao().toggleCompleted(taskId, true, System.currentTimeMillis())
        }

        TaskReminderScheduler.cancelReminder(context, taskId)

        // Refresh all widgets to reflect completion
        SnapTaskWidget().updateAll(context)
        LocketFocusWidget().updateAll(context)
        NotionStackWidget().updateAll(context)
    }
}

class NextTaskActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getInstance(context)
            val pendingTasks = database.taskDao().getPendingTasksForWidget()
            if (pendingTasks.isNotEmpty()) {
                val prefs = context.getSharedPreferences("snaptask_widget_prefs", Context.MODE_PRIVATE)
                val currentOffset = prefs.getInt("locket_offset", 0)
                val nextOffset = (currentOffset + 1) % pendingTasks.size
                prefs.edit().putInt("locket_offset", nextOffset).apply()
            }
        }

        LocketFocusWidget().update(context, glanceId)
    }
}
