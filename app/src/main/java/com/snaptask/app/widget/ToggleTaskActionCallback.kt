package com.snaptask.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.domain.model.TaskStatus
import com.snaptask.app.reminder.TaskReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToggleTaskActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskIdKey = ActionParameters.Key<Long>("taskId")
        val taskId = parameters[taskIdKey] ?: return

        withContext(Dispatchers.IO) {
            val database = AppDatabase.getInstance(context)
            database.taskDao().updateTaskStatus(taskId, TaskStatus.DONE.name)
        }

        TaskReminderScheduler.cancelReminder(context, taskId)

        // Refresh all 3 widgets to reflect completion
        SnapTaskWidget().updateAll(context)
        LocketFocusWidget().updateAll(context)
        NotionStackWidget().updateAll(context)
    }
}
