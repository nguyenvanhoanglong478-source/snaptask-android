package com.snaptask.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.MainActivity
import java.io.File

class SnapTaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val topTaskEntity = database.taskDao().getTopPendingTask()

        provideContent {
            GlanceTheme {
                if (topTaskEntity == null) {
                    EmptyWidgetContent(context)
                } else {
                    FocusTaskWidgetContent(
                        context = context,
                        taskId = topTaskEntity.id,
                        title = topTaskEntity.title,
                        thumbPath = topTaskEntity.thumbnailPath,
                        priority = topTaskEntity.priority
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyWidgetContent(context: Context) {
        // Obsidian Ink background with Almond Hearth text
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(0xFF151311))
                .cornerRadius(20.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Không có việc tồn đọng",
                    style = TextStyle(
                        color = ColorProvider(0xFFEED3BA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Button(
                    text = "+ Chụp việc mới",
                    onClick = actionStartActivity<MainActivity>()
                )
            }
        }
    }

    @Composable
    private fun FocusTaskWidgetContent(
        context: Context,
        taskId: Long,
        title: String,
        thumbPath: String,
        priority: Int
    ) {
        // Velvet Curfew & Obsidian Theme Widget
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(0xFF151311))
                .cornerRadius(20.dp)
                .padding(14.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Header Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIỆC CẦN LÀM",
                        style = TextStyle(
                            color = ColorProvider(0xFFEED3BA),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )

                    if (priority == TaskPriority.HIGH.level) {
                        Text(
                            text = "GẤP",
                            style = TextStyle(
                                color = ColorProvider(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Task Title in Almond Hearth
                Text(
                    text = title.ifBlank { "Việc cần làm" },
                    style = TextStyle(
                        color = ColorProvider(0xFFEED3BA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    maxLines = 2
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 1-Tap Done Button in Almond Hearth
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val taskIdParam = ActionParameters.Key<Long>("taskId")
                    Button(
                        text = "✓ Xong ngay",
                        onClick = actionRunCallback<ToggleTaskActionCallback>(
                            actionParametersOf(taskIdParam to taskId)
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
