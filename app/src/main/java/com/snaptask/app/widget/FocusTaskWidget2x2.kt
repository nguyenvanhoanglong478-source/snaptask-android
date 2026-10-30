package com.snaptask.app.widget

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.unit.ColorProvider
import com.snaptask.app.R
import com.snaptask.app.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmptyWidgetContent2x2() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.bgProvider)
            .cornerRadius(26.dp)
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
            Spacer(modifier = GlanceModifier.height(10.dp))
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
fun FocusContent2x2(
    taskId: Long,
    title: String,
    reminderTime: Long?,
    bitmap: Bitmap?,
    taskIndex: Int,
    totalTasks: Int
) {
    val now = System.currentTimeMillis()
    val isOverdue = reminderTime != null && reminderTime in 1 until now

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.bgProvider)
            .cornerRadius(26.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
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

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (reminderTime != null && reminderTime > 0) {
                    val label = if (isOverdue) {
                        val overdueMins = ((now - reminderTime) / 60000L).coerceAtLeast(1L)
                        if (overdueMins < 60L) "Trễ ${overdueMins}′" else "Trễ ${overdueMins / 60L}h"
                    } else {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminderTime))
                    }
                    Box(
                        modifier = GlanceModifier
                            .background(if (isOverdue) WidgetColors.urgentProvider else ColorProvider(Color(0xAD0F0F0E)))
                            .cornerRadius(9.dp)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                color = if (isOverdue) WidgetColors.onUrgentProvider else WidgetColors.textProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                if (totalTasks > 1) {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0xAD0F0F0E)))
                            .cornerRadius(9.dp)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .clickable(actionRunCallback<NextTaskActionCallback>())
                    ) {
                        Text(
                            text = "$taskIndex/$totalTasks",
                            style = TextStyle(
                                color = WidgetColors.accentProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ImageProvider(R.drawable.widget_scrim))
                    .padding(top = 36.dp, start = 12.dp, end = 10.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = title.ifBlank { "Việc cần làm" },
                            style = TextStyle(
                                color = WidgetColors.textProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            maxLines = 2
                        )

                        val remaining = maxOf(0, totalTasks - taskIndex)
                        if (remaining > 0) {
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = "Còn $remaining việc",
                                style = TextStyle(
                                    color = WidgetColors.textSecondaryProvider,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    val taskIdParam = ActionParameters.Key<Long>("taskId")
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_check_done),
                        contentDescription = "Hoàn thành",
                        modifier = GlanceModifier
                            .size(40.dp)
                            .clickable(
                                actionRunCallback<CompleteTaskActionCallback>(
                                    actionParametersOf(taskIdParam to taskId)
                                )
                            )
                    )
                }
            }
        }
    }
}
