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
fun FocusContent2x2(
    taskId: Long,
    title: String,
    reminderTime: Long?,
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

        // 2. Overlaid Column: Top Chips + Spacer + Bottom Scrim Box
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Top Chips Row (at position 12, 12)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left: CHỈ chip giờ nhắc ở vị trí (12,12), nền rgba(18,17,16,0.72), chữ flash 11sp Bold, padding ngang 8 dọc 3, bo 7
                if (reminderTime != null && reminderTime > 0) {
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminderTime))
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0xB8121110)))
                            .cornerRadius(7.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = timeStr,
                            style = TextStyle(
                                color = WidgetColors.accentProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Top-Right: Chip đếm "n/m" CHỈ hiện khi totalTasks > 1 (để giữ chức năng chuyển việc)
                if (totalTasks > 1) {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0xB8121110)))
                            .cornerRadius(7.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clickable(actionRunCallback<NextTaskActionCallback>())
                    ) {
                        Text(
                            text = "$taskIndex/$totalTasks",
                            style = TextStyle(
                                color = WidgetColors.accentProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Bottom Scrim: Linear gradient #00121110 -> #EB121110, padding trên 40, ngang 14, dưới 14
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ImageProvider(R.drawable.widget_scrim))
                    .padding(top = 40.dp, start = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = title.ifBlank { "Việc cần làm" },
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFF5F1E8)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            maxLines = 2
                        )

                        val remaining = maxOf(0, totalTasks - taskIndex)
                        if (remaining > 0) {
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = "Còn $remaining việc nữa",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFE4DFD2)),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp
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
                            .size(36.dp)
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
