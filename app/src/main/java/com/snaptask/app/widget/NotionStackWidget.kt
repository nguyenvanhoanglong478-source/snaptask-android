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
import com.snaptask.app.ui.MainActivity

class NotionStackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotionStackWidget()
}

class NotionStackWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

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
        val widgetSize = LocalSize.current
        val availableWidth = (widgetSize.width - 30.dp).coerceAtLeast(100.dp)
        val leftColumnWidth = availableWidth * 0.42f
        val rightColumnWidth = availableWidth * 0.58f

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.bgProvider)
                .cornerRadius(24.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CỘT TRÁI (~42%): Ảnh bo 18, scrim gradient, tiêu đề 14sp Bold ở đáy + nút tick tròn 32dp
            Box(
                modifier = GlanceModifier
                    .width(leftColumnWidth)
                    .fillMaxHeight()
                    .cornerRadius(18.dp)
                    .background(WidgetColors.elevatedProvider)
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

                    // Scrim gradient thật: widget_scrim.xml
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ImageProvider(R.drawable.widget_scrim))
                                .padding(top = 28.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                        ) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topTask.title.ifBlank { "Việc cần làm" },
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFF5F1E8)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                val taskIdParam = ActionParameters.Key<Long>("taskId")
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

            // CỘT PHẢI (~58%): Header TIẾP THEO + danh sách việc + Nút "+ Chụp việc mới"
            Column(
                modifier = GlanceModifier
                    .width(rightColumnWidth)
                    .fillMaxHeight()
            ) {
                // Header: "TIẾP THEO" on Left, "đã xong/tổng" on Right in Flash Yellow
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

                // Checklist Items (Tối đa 3 hàng)
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
                                color = ColorProvider(Color(0xFF8F897B)),
                                fontSize = 13.sp
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
                            val taskIdParam = ActionParameters.Key<Long>("taskId")
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
                                // Ô vuông 20dp bo 7 viền 1.5dp #8F897B
                                Image(
                                    provider = ImageProvider(R.drawable.widget_checkbox_empty),
                                    contentDescription = "Chọn hoàn thành",
                                    modifier = GlanceModifier.size(20.dp)
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Text(
                                    text = item.title,
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFF5F1E8)),
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Nút "+ Chụp việc mới": nền flash, chữ onFlash 13sp Bold, cao 36, bo 12
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
