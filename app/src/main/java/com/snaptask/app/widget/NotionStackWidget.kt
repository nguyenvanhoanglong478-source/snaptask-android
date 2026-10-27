package com.snaptask.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
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
import androidx.glance.layout.width
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.data.local.TaskEntity
import com.snaptask.app.ui.MainActivity

class NotionStackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotionStackWidget()
}

class NotionStackWidget : GlanceAppWidget() {

    private val bgDarkProvider       = ColorProvider(Color(0xFF121212))
    private val surfaceDarkProvider  = ColorProvider(Color(0xFF1E1E1E))
    private val overlayGlassProvider = ColorProvider(Color(0xE6141414))
    private val whiteProvider        = ColorProvider(Color(0xFFFFFFFF))
    private val secondaryProvider    = ColorProvider(Color(0xFFA6A6A6))
    private val accentProvider       = ColorProvider(Color(0xFFFF6B4A))
    private val pillDarkProvider     = ColorProvider(Color(0xCC000000))

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
            WidgetBitmapHelper.loadOptimizedBitmap(
                if (it.thumbnailPath.isNotBlank()) it.thumbnailPath else it.beforeImageUri,
                maxSize = 400
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
                .background(bgDarkProvider)
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
                            .background(surfaceDarkProvider)
                    ) {
                        if (bitmap != null) {
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = topTask.title,
                                contentScale = ContentScale.Crop,
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        }

                        // Top Pill on photo: Progress Done/Total
                        Box(
                            modifier = GlanceModifier
                                .padding(6.dp)
                                .background(pillDarkProvider)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🎯 $completedCount/$totalCount xong",
                                style = TextStyle(
                                    color = accentProvider,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }

                        // Bottom Title Bar
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(overlayGlassProvider)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = topTask.title,
                                style = TextStyle(
                                    color = whiteProvider,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // 1-Tap Done Button for Priority Task
                    val taskIdParam = ActionParameters.Key<Long>("taskId")
                    Button(
                        text = "✓ Hoàn thành",
                        onClick = actionRunCallback<CompleteTaskActionCallback>(
                            actionParametersOf(taskIdParam to topTask.id)
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(surfaceDarkProvider)
                            .cornerRadius(18.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hết việc! ✨",
                            style = TextStyle(
                                color = whiteProvider,
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
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHECKLIST TIẾP",
                        style = TextStyle(
                            color = secondaryProvider,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
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
                                color = secondaryProvider,
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
                                    text = "▫ ",
                                    style = TextStyle(
                                        color = accentProvider,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = item.title,
                                    style = TextStyle(
                                        color = whiteProvider,
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

                // Quick Add Photo Button
                Button(
                    text = "📸 Chụp việc mới",
                    onClick = actionStartActivity<MainActivity>(),
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }
}
