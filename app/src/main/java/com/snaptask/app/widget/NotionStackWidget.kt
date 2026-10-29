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
import androidx.glance.layout.fillMaxHeight
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
import com.snaptask.app.ui.MainActivity

class NotionStackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotionStackWidget()
}

class NotionStackWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val pendingTasks = database.taskDao().getPendingTasksForWidget()
        val completedCount = database.taskDao().getCompletedCount()
        val totalCount = database.taskDao().getTotalCount()

        val topTask: TaskEntity? = pendingTasks.firstOrNull()
        val nextTasks: List<TaskEntity> = if (pendingTasks.size > 1) {
            pendingTasks.drop(1).take(3)
        } else {
            emptyList()
        }

        val bitmap: Bitmap? = topTask?.let {
            val mainUri = if (it.isCompleted && !it.afterImageUri.isNullOrBlank()) it.afterImageUri else it.beforeImageUri
            val pipUri = if (it.isCompleted && !it.afterImageUri.isNullOrBlank()) it.beforeImageUri else null
            WidgetBitmapHelper.createWidgetCompositeBitmap(
                mainImageUri = mainUri,
                miniPipImageUri = pipUri,
                isCompleted = it.isCompleted,
                outputSize = 380
            )
        }

        provideContent {
            GlanceTheme {
                StackWidgetContent(
                    topTask = topTask,
                    bitmap = bitmap,
                    nextTasks = nextTasks,
                    completedCount = completedCount,
                    totalCount = totalCount
                )
            }
        }
    }

    @Composable
    private fun StackWidgetContent(
        topTask: TaskEntity?,
        bitmap: Bitmap?,
        nextTasks: List<TaskEntity>,
        completedCount: Int,
        totalCount: Int
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.bgProvider)
                .cornerRadius(24.dp)
                .padding(10.dp)
        ) {
            // LEFT COLUMN (~45%): 1:1 Photo + Top Task Action + Daily Progress
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (topTask != null) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .cornerRadius(18.dp)
                            .background(WidgetColors.elevatedProvider)
                    ) {
                        if (bitmap != null) {
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = topTask.title,
                                contentScale = ContentScale.Crop,
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        }

                        Column(modifier = GlanceModifier.fillMaxSize()) {
                            // Top Pill on photo: Progress Done/Total
                            Box(
                                modifier = GlanceModifier
                                    .padding(6.dp)
                                    .background(WidgetColors.pillProvider)
                                    .cornerRadius(8.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$completedCount/$totalCount xong",
                                    style = TextStyle(
                                        color = WidgetColors.accentProvider,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            // Bottom Title Bar with Done Checkmark Image Button
                            val taskIdParam = ActionParameters.Key<Long>("taskId")
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(WidgetColors.glassProvider)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topTask.title,
                                    style = TextStyle(
                                        color = WidgetColors.textProvider,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_check_done),
                                    contentDescription = "Hoàn thành",
                                    modifier = GlanceModifier
                                        .size(28.dp)
                                        .clickable(
                                            actionRunCallback<CompleteTaskActionCallback>(
                                                actionParametersOf(taskIdParam to topTask.id)
                                            )
                                        )
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(WidgetColors.elevatedProvider)
                            .cornerRadius(18.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hết việc!",
                            style = TextStyle(
                                color = WidgetColors.textProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // RIGHT COLUMN (~55%): Notion-style Checklist + Camera Shortcut
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            ) {
                // Header (Page 7): "TIẾP THEO" on Left, "3/7" on Right
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIẾP THEO",
                        style = TextStyle(
                            color = WidgetColors.textSecondaryProvider,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$completedCount/$totalCount",
                        style = TextStyle(
                            color = WidgetColors.accentProvider,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Checklist Items (Up to 3)
                if (nextTasks.isEmpty()) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (topTask == null) "Đã hoàn thành mọi việc!" else "Không có việc chờ tiếp theo",
                            style = TextStyle(
                                color = WidgetColors.textSecondaryProvider,
                                fontSize = 11.sp
                            )
                        )
                    }
                } else {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                    ) {
                        nextTasks.forEach { item ->
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "◯ ",
                                    style = TextStyle(
                                        color = WidgetColors.accentProvider,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = item.title,
                                    style = TextStyle(
                                        color = WidgetColors.textProvider,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Quick Add Photo Button in 4x2 widget: #FFD23F background, #17140A text
                Button(
                    text = "+ Chụp việc mới",
                    onClick = actionStartActivity<MainActivity>(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WidgetColors.accentProvider,
                        contentColor = WidgetColors.onAccentProvider
                    ),
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }
}
