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
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.MainActivity
import com.snaptask.app.util.DateTimeUtils

@Composable
fun EmptyWidgetContent2x2() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.bgProvider)
            .cornerRadius(26.dp)
            .clickable(actionStartActivity<MainActivity>())
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
    dueDate: Long? = null,
    priority: String = TaskPriority.MEDIUM.name,
    bitmap: Bitmap?,
    taskIndex: Int,
    totalTasks: Int
) {
    val taskIdParam = ActionParameters.Key<Long>("taskId")
    val now = System.currentTimeMillis()
    val validReminder = reminderTime?.takeIf { it > 0L }
    val validDue = dueDate?.takeIf { it > 0L }
    val isHighPriority = runCatching { TaskPriority.valueOf(priority) == TaskPriority.HIGH }.getOrDefault(false)
    val isReminderOverdue = validReminder != null && validReminder < now
    val isDueOverdue = DateTimeUtils.isOverdue(validDue, isCompleted = false, nowMillis = now)
    val isOverdue = isReminderOverdue || isDueOverdue

    val statusText: String? = when {
        isReminderOverdue && validReminder != null -> "Trễ ${formatShortOverdue(now - validReminder)}"
        isDueOverdue -> "Quá hạn"
        validReminder != null -> DateTimeUtils.formatTime(validReminder)
        validDue != null -> DateTimeUtils.formatDueLabel(validDue, now)
        isHighPriority -> "Gấp"
        else -> null
    }
    val isUrgentBadge = isOverdue || statusText == "Gấp"

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.bgProvider)
            .cornerRadius(26.dp)
            .clickable(
                actionRunCallback<OpenTaskActionCallback>(
                    actionParametersOf(taskIdParam to taskId)
                )
            )
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
                if (statusText != null) {
                    Box(
                        modifier = GlanceModifier
                            .background(if (isUrgentBadge) WidgetColors.urgentProvider else ColorProvider(Color(0xAD0F0F0E)))
                            .cornerRadius(9.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isHighPriority && !isUrgentBadge) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(6.dp)
                                        .cornerRadius(3.dp)
                                        .background(WidgetColors.urgentProvider)
                                ) {}
                                Spacer(modifier = GlanceModifier.width(5.dp))
                            }
                            Text(
                                text = statusText,
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
                        val subLabel = when {
                            remaining > 0 -> "Còn $remaining việc"
                            validReminder != null && validDue != null -> "Hạn ${DateTimeUtils.formatDueLabel(validDue, now)}"
                            else -> null
                        }
                        if (subLabel != null) {
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = subLabel,
                                style = TextStyle(
                                    color = if (isDueOverdue && remaining == 0) WidgetColors.urgentProvider else WidgetColors.textSecondaryProvider,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

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

private fun formatShortOverdue(diffMs: Long): String {
    val mins = (diffMs / 60_000L).coerceAtLeast(1L)
    return when {
        mins < 60L -> "${mins}′"
        mins < 1440L -> "${mins / 60L}h"
        else -> "${mins / 1440L}d"
    }
}
