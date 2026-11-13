package com.snaptask.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.data.local.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SnapTaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val pendingTasks = withContext(Dispatchers.IO) {
            runCatching {
                AppDatabase.getInstance(context).taskDao().getPendingTasksForWidget()
            }.getOrDefault(emptyList())
        }

        val prefs = context.getSharedPreferences("snaptask_widget_prefs", Context.MODE_PRIVATE)
        val offset = prefs.getInt("locket_offset", 0)

        val task: TaskEntity?
        val taskIndex: Int
        if (pendingTasks.isNotEmpty()) {
            val validIndex = ((offset % pendingTasks.size) + pendingTasks.size) % pendingTasks.size
            task = pendingTasks[validIndex]
            taskIndex = validIndex + 1
        } else {
            task = null
            taskIndex = 0
        }

        val bitmap: Bitmap? = withContext(Dispatchers.IO) {
            task?.let {
                val mainUri = if (it.isCompleted && !it.afterImageUri.isNullOrBlank()) it.afterImageUri else it.beforeImageUri
                val pipUri = if (it.isCompleted && !it.afterImageUri.isNullOrBlank()) it.beforeImageUri else null
                WidgetBitmapHelper.createWidgetCompositeBitmap(
                    mainImageUri = mainUri,
                    miniPipImageUri = pipUri,
                    isCompleted = it.isCompleted,
                    outputSize = 360
                )
            }
        }

        provideContent {
            GlanceTheme {
                if (task == null) {
                    EmptyWidgetContent2x2()
                } else {
                    FocusContent2x2(
                        taskId = task.id,
                        title = task.title,
                        reminderTime = task.reminderTime,
                        dueDate = task.dueDate,
                        priority = task.priority,
                        bitmap = bitmap,
                        taskIndex = taskIndex,
                        totalTasks = pendingTasks.size
                    )
                }
            }
        }
    }
}
