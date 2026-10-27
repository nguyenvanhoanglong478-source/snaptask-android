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
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.MainActivity

class LocketFocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LocketFocusWidget()
}

class LocketFocusWidget : GlanceAppWidget() {

    private val bgDarkProvider       = ColorProvider(Color(0xFF121212))
    private val overlayGlassProvider = ColorProvider(Color(0xE6141414))
    private val whiteProvider        = ColorProvider(Color(0xFFFFFFFF))
    private val accentProvider       = ColorProvider(Color(0xFFFF6B4A))
    private val redProvider          = ColorProvider(Color(0xFFEF4444))
    private val pillDarkProvider     = ColorProvider(Color(0xCC000000))

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
            WidgetBitmapHelper.loadOptimizedBitmap(
                if (it.thumbnailPath.isNotBlank()) it.thumbnailPath else it.beforeImageUri,
                maxSize = 450
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
                .background(bgDarkProvider)
                .cornerRadius(28.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tất cả đã xong ✨",
                    style = TextStyle(
                        color = whiteProvider,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Button(
                    text = "📸 Chụp việc mới",
                    onClick = actionStartActivity<MainActivity>()
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
                .background(bgDarkProvider)
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
                        .background(ColorProvider(Color(0xFF262626)))
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
                            .background(pillDarkProvider)
                            .cornerRadius(10.dp)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isPinned) "📌 $tag" else tag,
                            style = TextStyle(
                                color = whiteProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    if (priority == TaskPriority.HIGH.name) {
                        Box(
                            modifier = GlanceModifier
                                .background(redProvider)
                                .cornerRadius(10.dp)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "GẤP 🔥",
                                style = TextStyle(
                                    color = whiteProvider,
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
                            .background(pillDarkProvider)
                            .cornerRadius(10.dp)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$taskIndex/$totalTasks",
                            style = TextStyle(
                                color = accentProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 3. Bottom Glassmorphism Bar: Title + Checkbox + Next Button
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(overlayGlassProvider)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title.ifBlank { "Việc cần làm" },
                        style = TextStyle(
                            color = whiteProvider,
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
                        Button(
                            text = "✓ Xong",
                            onClick = actionRunCallback<CompleteTaskActionCallback>(
                                actionParametersOf(taskIdParam to taskId)
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )

                        Spacer(modifier = GlanceModifier.width(6.dp))

                        Button(
                            text = "Tiếp ❯",
                            onClick = actionRunCallback<NextTaskActionCallback>(),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }
}
