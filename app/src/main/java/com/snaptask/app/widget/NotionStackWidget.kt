package com.snaptask.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.unit.ColorProvider
import com.snaptask.app.R
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.data.local.TaskEntity
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.MainActivity
import com.snaptask.app.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotionStackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotionStackWidget()
}

class NotionStackWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = withContext(Dispatchers.IO) {
            runCatching {
                val dao = AppDatabase.getInstance(context).taskDao()
                Triple(
                    dao.getPendingTasksForWidget(),
                    dao.getCompletedCount(),
                    dao.getTotalCount()
                )
            }.getOrDefault(Triple(emptyList(), 0, 0))
        }
        val pendingTasks = widgetData.first
        val completedCount = widgetData.second
        val totalCount = widgetData.third

        val topTask: TaskEntity? = pendingTasks.firstOrNull()
        val nextTasks: List<TaskEntity> = if (pendingTasks.size > 1) {
            pendingTasks.drop(1).take(3)
        } else {
            emptyList()
        }

        val bitmap: Bitmap? = withContext(Dispatchers.IO) {
            topTask?.let {
                val mainUri = if (it.isCompleted && !it.afterImageUri.isNullOrBlank()) it.afterImageUri else it.beforeImageUri
                val pipUri = if (it.isCompleted && !it.afterImageUri.isNullOrBlank()) it.beforeImageUri else null
                WidgetBitmapHelper.createWidgetCompositeBitmap(
                    mainImageUri = mainUri,
                    miniPipImageUri = pipUri,
                    isCompleted = it.isCompleted,
                    outputSize = 380
                )
            }
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
        val widgetSize = LocalSize.current
        val availableWidth = (widgetSize.width - 30.dp).coerceAtLeast(100.dp)
        val leftColumnWidth = availableWidth * 0.42f
        val rightColumnWidth = availableWidth * 0.58f
        val taskIdParam = ActionParameters.Key<Long>("taskId")
        val now = System.currentTimeMillis()

        val featuredClickAction = if (topTask != null) {
            actionRunCallback<OpenTaskActionCallback>(
                actionParametersOf(taskIdParam to topTask.id)
            )
        } else {
            actionStartActivity<MainActivity>()
        }

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.bgProvider)
                .cornerRadius(26.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(leftColumnWidth)
                    .fillMaxHeight()
                    .cornerRadius(20.dp)
                    .background(WidgetColors.elevatedProvider)
                    .clickable(featuredClickAction)
            ) {
                if (topTask != null) {
                    if (bitmap != null) {
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = topTask.title,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    }

                    val validReminder = topTask.reminderTime?.takeIf { it > 0L }
                    val validDue = topTask.dueDate?.takeIf { it > 0L }
                    val isHighPriority = runCatching {
                        TaskPriority.valueOf(topTask.priority) == TaskPriority.HIGH
                    }.getOrDefault(false)
                    val isReminderOverdue = validReminder != null && validReminder < now
                    val isDueOverdue = DateTimeUtils.isOverdue(validDue, isCompleted = false, nowMillis = now)
                    val isOverdue = isReminderOverdue || isDueOverdue
                    val badgeText: String? = when {
                        isReminderOverdue -> "Trễ"
                        isDueOverdue -> "Quá hạn"
                        validReminder != null -> DateTimeUtils.formatTime(validReminder)
                        validDue != null -> DateTimeUtils.formatDueLabel(validDue, now)
                        isHighPriority -> "Gấp"
                        else -> null
                    }
                    val isUrgentBadge = isOverdue || badgeText == "Gấp"

                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        if (badgeText != null) {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .background(if (isUrgentBadge) WidgetColors.urgentProvider else ColorProvider(Color(0xAD0F0F0E)))
                                        .cornerRadius(8.dp)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isHighPriority && !isUrgentBadge) {
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(6.dp)
                                                    .cornerRadius(3.dp)
                                                    .background(WidgetColors.urgentProvider)
                                            ) {}
                                            Spacer(modifier = GlanceModifier.width(4.dp))
                                        }
                                        Text(
                                            text = badgeText,
                                            style = TextStyle(
                                                color = if (isUrgentBadge) WidgetColors.onUrgentProvider else WidgetColors.textProvider,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ImageProvider(R.drawable.widget_scrim))
                                .padding(top = 24.dp, start = 9.dp, end = 8.dp, bottom = 8.dp)
                        ) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topTask.title.ifBlank { "Việc cần làm" },
                                    style = TextStyle(
                                        color = WidgetColors.textProvider,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_check_done),
                                    contentDescription = "Hoàn thành",
                                    modifier = GlanceModifier
                                        .size(32.dp)
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
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hết việc",
                            style = TextStyle(
                                color = WidgetColors.textProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            Column(
                modifier = GlanceModifier
                    .width(rightColumnWidth)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIẾP THEO",
                        style = TextStyle(
                            color = WidgetColors.textSecondaryProvider,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$completedCount/$totalCount",
                        style = TextStyle(
                            color = WidgetColors.accentProvider,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                if (nextTasks.isEmpty()) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không có việc chờ tiếp theo",
                            style = TextStyle(
                                color = WidgetColors.textMutedProvider,
                                fontSize = 12.sp
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
                            val itemHighPriority = runCatching {
                                TaskPriority.valueOf(item.priority) == TaskPriority.HIGH
                            }.getOrDefault(false)
                            val itemOverdue = (item.reminderTime != null && item.reminderTime in 1 until now) ||
                                DateTimeUtils.isOverdue(item.dueDate?.takeIf { it > 0L }, isCompleted = false, nowMillis = now)

                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable(
                                        actionRunCallback<CompleteTaskActionCallback>(
                                            actionParametersOf(taskIdParam to item.id)
                                        )
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.widget_checkbox_empty),
                                    contentDescription = "Chọn hoàn thành",
                                    modifier = GlanceModifier.size(20.dp)
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                if (itemHighPriority || itemOverdue) {
                                    Box(
                                        modifier = GlanceModifier
                                            .size(6.dp)
                                            .cornerRadius(3.dp)
                                            .background(WidgetColors.urgentProvider)
                                    ) {}
                                    Spacer(modifier = GlanceModifier.width(5.dp))
                                }
                                Text(
                                    text = item.title,
                                    style = TextStyle(
                                        color = WidgetColors.textProvider,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                Button(
                    text = "+ Chụp việc mới",
                    onClick = actionStartActivity<MainActivity>(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WidgetColors.accentProvider,
                        contentColor = WidgetColors.onAccentProvider
                    ),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .cornerRadius(12.dp)
                )
            }
        }
    }
}
