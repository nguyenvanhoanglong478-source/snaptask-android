package com.snaptask.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.data.local.TaskEntity

class LocketFocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LocketFocusWidget()
}

class LocketFocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val pendingTasks = database.taskDao().getPendingTasksForWidget()

        val prefs = context.getSharedPreferences("snaptask_widget_prefs", Context.MODE_PRIVATE)
        val offset = prefs.getInt("locket_offset", 0)

        val task: TaskEntity? = if (pendingTasks.isNotEmpty()) {
            val validIndex = offset.coerceIn(0, pendingTasks.size - 1)
            pendingTasks[validIndex]
        } else {
            null
        }

        val bitmap: Bitmap? = task?.let {
            val mainUri = if (it.isCompleted && !it.afterImageUri.isNullOrBlank()) it.afterImageUri else it.beforeImageUri
            val pipUri = if (it.isCompleted && !it.afterImageUri.isNullOrBlank()) it.beforeImageUri else null
            WidgetBitmapHelper.createWidgetCompositeBitmap(
                mainImageUri = mainUri,
                miniPipImageUri = pipUri,
                isCompleted = it.isCompleted,
                outputSize = 420
            )
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
                        bitmap = bitmap,
                        taskIndex = (offset % pendingTasks.size) + 1,
                        totalTasks = pendingTasks.size
                    )
                }
            }
        }
    }
}
