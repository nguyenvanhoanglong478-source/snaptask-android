package com.snaptask.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.snaptask.app.R
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.data.local.TaskEntity
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.MainActivity

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
                    EmptyContent()
                } else {
                    FocusContent(
                        taskId = task.id,
                        title = task.title,
                        tag = task.tag,
                        priority = task.priority,
                        isPinned = task.isPinnedToWidget,
                        bitmap = bitmap,
                        taskIndex = (offset % pendingTasks.size) + 1,
                        totalTasks = pendingTasks.size
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.bgProvider)
                .cornerRadius(28.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tất cả đã xong",
                    style = TextStyle(
                        color = WidgetColors.textProvider,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Button(
                    text = "Chụp việc mới",
                    onClick = actionStartActivity<MainActivity>(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WidgetColors.accentProvider,
                        contentColor = WidgetColors.onAccentProvider
                    )
                )
            }
        }
    }

    @Composable
    private fun FocusContent(
        taskId: Long,
        title: String,
        tag: String,
        priority: String,
        isPinned: Boolean,
        bitmap: Bitmap?,
        taskIndex: Int,
        totalTasks: Int
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.bgProvider)
                .cornerRadius(28.dp)
        ) {
            // 1. Photo filling 1:1 squircle
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetColors.elevatedProvider)
                ) {}
            }

            // 2. Overlaid Elements
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Top Pills: Tag + Counter / Priority
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(WidgetColors.pillProvider)
                            .cornerRadius(10.dp)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isPinned) "[Ghim] $tag" else tag,
                            style = TextStyle(
                                color = WidgetColors.textProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    if (priority == TaskPriority.HIGH.name) {
                        Box(
                            modifier = GlanceModifier
                                .background(WidgetColors.urgentProvider)
                                .cornerRadius(10.dp)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "GẤP",
                                style = TextStyle(
                                    color = WidgetColors.textProvider,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(4.dp))
                    }

                    // Task Counter Pill (e.g. 1/3)
                    Box(
                        modifier = GlanceModifier
                            .background(WidgetColors.pillProvider)
                            .cornerRadius(10.dp)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$taskIndex/$totalTasks",
                            style = TextStyle(
                                color = WidgetColors.accentProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 3. Bottom Glassmorphism Bar: Title + Checkmark Button + Next Button
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(WidgetColors.glassProvider)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title.ifBlank { "Việc cần làm" },
                        style = TextStyle(
                            color = WidgetColors.textProvider,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val taskIdParam = ActionParameters.Key<Long>("taskId")
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_check_done),
                            contentDescription = "Hoàn thành",
                            modifier = GlanceModifier
                                .size(34.dp)
                                .clickable(
                                    actionRunCallback<CompleteTaskActionCallback>(
                                        actionParametersOf(taskIdParam to taskId)
                                    )
                                )
                        )

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        Button(
                            text = "Tiếp",
                            onClick = actionRunCallback<NextTaskActionCallback>(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = WidgetColors.elevatedProvider,
                                contentColor = WidgetColors.textProvider
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }
}
