package com.snaptask.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.domain.model.TaskStatus
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

        // Re-render widget with the next top pending task
        SnapTaskWidget().update(context, glanceId)
    }
}
