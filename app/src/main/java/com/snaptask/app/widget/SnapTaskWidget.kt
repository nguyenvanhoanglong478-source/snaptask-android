package com.snaptask.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.glance.appwidget.action.ActionCallback
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.MainActivity
import java.io.File

class SnapTaskWidget : GlanceAppWidget() {

    private val obsidianProvider  = ColorProvider(Color(0xFF14161F))
    private val almondProvider    = ColorProvider(Color(0xFF6366F1))
    private val whiteProvider     = ColorProvider(Color(0xFFF8FAFC))
    private val redProvider       = ColorProvider(Color(0xFFF43F5E))
    private val overlayProvider   = ColorProvider(Color(0xE614161F)) // Frosted dark glass overlay

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val topTaskEntity = database.taskDao().getTopPendingTask()

        provideContent {
            GlanceTheme {
                if (topTaskEntity == null) {
                    EmptyWidgetContent(context)
                } else {
                    val bitmap = loadOptimizedBitmap(topTaskEntity.thumbnailPath, topTaskEntity.imagePath)
                    FocusTaskWidgetContent(
                        context = context,
                        taskId = topTaskEntity.id,
                        title = topTaskEntity.title,
                        bitmap = bitmap,
                        priority = topTaskEntity.priorityInt
                    )
                }
            }
        }
    }

    private fun loadOptimizedBitmap(thumbPath: String, imagePath: String): Bitmap? {
        val targetPath = when {
            thumbPath.isNotBlank() && File(thumbPath).exists() -> thumbPath
            imagePath.isNotBlank() && File(imagePath).exists() -> imagePath
            else -> return null
        }
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(targetPath, options)

            // Scale down to max 380x380 for widget IPC performance
            var sample = 1
            while (options.outWidth / sample > 400 || options.outHeight / sample > 400) {
                sample *= 2
            }
            options.inSampleSize = sample
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeFile(targetPath, options)
        } catch (_: Throwable) {
            null
        }
    }

    @Composable
    private fun EmptyWidgetContent(context: Context) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(obsidianProvider)
                .cornerRadius(24.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Không có việc tồn đọng ✨",
                    style = TextStyle(
                        color = whiteProvider,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
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
    private fun FocusTaskWidgetContent(
        context: Context,
        taskId: Long,
        title: String,
        bitmap: Bitmap?,
        priority: Int
    ) {
        // True Locket Experience: Photo fills the entire widget with rounded corners
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(obsidianProvider)
                .cornerRadius(24.dp)
        ) {
            // 1. The Captured Photo (Fills 100% of widget like Locket!)
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize()
                )
            } else {
                // Gradient fallback if photo is not ready
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF1E212D)))
                ) {}
            }

            // 2. Overlay Structure: Top Badge + Push to Bottom + Bottom Frosted Card
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Top Bar Pill
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0xB3151311)))
                            .cornerRadius(8.dp)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "VIỆC CẦN LÀM",
                            style = TextStyle(
                                color = almondProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    if (priority == TaskPriority.HIGH.level) {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Box(
                            modifier = GlanceModifier
                                .background(redProvider)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GẤP",
                                style = TextStyle(
                                    color = whiteProvider,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                // Spacer pushing the bottom card down
                Spacer(modifier = GlanceModifier.defaultWeight())

                // 3. Bottom Frosted Glass Card with Task Title & 1-Tap Done Button
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(overlayProvider)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title.ifBlank { "Nhiệm vụ không tên" },
                        style = TextStyle(
                            color = whiteProvider,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = GlanceModifier.height(5.dp))

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
