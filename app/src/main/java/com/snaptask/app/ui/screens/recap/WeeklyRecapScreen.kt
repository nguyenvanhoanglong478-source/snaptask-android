package com.snaptask.app.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.components.RoundIconButton
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WeeklyRecapScreen(
    tasks: List<Task>,
    onBack: () -> Unit,
    onOpenHeatmap: () -> Unit,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val todayKey = remember { SnapDateHelper.dayKey(System.currentTimeMillis()) }

    val (weekStartKey, weekEndKey, weekNumber) = remember(todayKey) {
        val cal = Calendar.getInstance().apply { timeInMillis = todayKey }
        val wk = cal.get(Calendar.WEEK_OF_YEAR)
        val offsetFromMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -offsetFromMonday)
        val start = SnapDateHelper.dayKey(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val end = SnapDateHelper.dayKey(cal.timeInMillis)
        Triple(start, end, wk)
    }

    val rangeFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val weekRangeLabel = remember(weekStartKey, weekEndKey) {
        "${rangeFormatter.format(Date(weekStartKey))} - ${rangeFormatter.format(Date(weekEndKey))}"
    }

    val weekDoneTasks = remember(tasks, weekStartKey, weekEndKey) {
        tasks.filter { task ->
            if (!task.isCompleted) false
            else {
                val d = SnapDateHelper.dayKey(task.completedAt ?: task.createdAt)
                d in weekStartKey..weekEndKey
            }
        }.sortedByDescending { it.completedAt ?: it.createdAt }
    }

    val (currentStreak, _) = remember(tasks) {
        SnapDateHelper.computeStreaks(tasks)
    }

    val avgMinutes = remember(weekDoneTasks) {
        if (weekDoneTasks.isEmpty()) 0
        else weekDoneTasks.map { SnapDateHelper.durationMinutes(it) }.average().toInt()
    }

    val busiestWeekday = remember(weekDoneTasks) {
        if (weekDoneTasks.isEmpty()) "Chưa có"
        else {
            val cal = Calendar.getInstance()
            val top = weekDoneTasks.groupBy {
                cal.timeInMillis = it.completedAt ?: it.createdAt
                cal.get(Calendar.DAY_OF_WEEK)
            }.maxByOrNull { it.value.size }?.key
            if (top != null) SnapDateHelper.vietnameseWeekdayShort(top) else "Chưa có"
        }
    }

    val bestProofTask = remember(weekDoneTasks, tasks) {
        val inWeek = weekDoneTasks.firstOrNull { t ->
            t.hasProof && File(t.beforeImageUri).exists() && !t.afterImageUri.isNullOrBlank() && File(t.afterImageUri).exists()
        }
        inWeek ?: tasks.filter { it.isCompleted }.firstOrNull { t ->
            t.hasProof && File(t.beforeImageUri).exists() && !t.afterImageUri.isNullOrBlank() && File(t.afterImageUri).exists()
        }
    }

    val weekPhotoItems = remember(weekDoneTasks) {
        val cal = Calendar.getInstance()
        weekDoneTasks.mapNotNull { t ->
            val path = if (!t.afterImageUri.isNullOrBlank()) t.afterImageUri else t.beforeImageUri
            val f = if (!path.isNullOrBlank()) File(path) else null
            if (f != null && f.exists()) {
                cal.timeInMillis = t.completedAt ?: t.createdAt
                Triple(t, f, SnapDateHelper.vietnameseWeekdayShort(cal.get(Calendar.DAY_OF_WEEK)))
            } else {
                null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item(key = "recap_topbar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(
                    icon = SnapTaskIcons.ChevronLeft,
                    contentDescription = "Quay lại",
                    onClick = onBack
                )
                Text(
                    text = weekRangeLabel,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                RoundIconButton(
                    icon = SnapTaskIcons.HeatMap,
                    contentDescription = "Mở bản đồ nhiệt",
                    onClick = onOpenHeatmap
                )
            }
        }

        item(key = "recap_headline") {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    text = "NHÌN LẠI TUẦN $weekNumber",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${weekDoneTasks.size} việc xong,\nchuỗi $currentStreak ngày.",
                    color = colors.text,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp
                )
            }
        }

        item(key = "recap_stats") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.flash)
                        .padding(14.dp)
                ) {
                    Text(
                        text = weekDoneTasks.size.toString(),
                        color = colors.onFlash,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "việc xong",
                        color = colors.onFlash.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (avgMinutes > 0) "${avgMinutes}p" else "0p",
                        color = colors.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "trung bình",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = busiestWeekday,
                        color = colors.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "chăm nhất",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = onOpenHeatmap)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.HeatMap,
                            contentDescription = null,
                            tint = colors.text,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Mở bản đồ nhiệt 12 tuần & cả năm",
                            color = colors.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = SnapTaskIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item(key = "recap_highlight") {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp)) {
                Text(
                    text = "TRƯỚC / SAU ĐÁNG NHỚ",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )

                if (bestProofTask != null) {
                    val beforeFile = remember(bestProofTask.beforeImageUri) { File(bestProofTask.beforeImageUri) }
                    val afterFile = remember(bestProofTask.afterImageUri) { File(bestProofTask.afterImageUri ?: "") }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(204.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
                            .clickable { onTaskClick(bestProofTask) }
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(beforeFile).crossfade(true).build(),
                                contentDescription = "Trước",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFFFAFAF6))
                            )
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(afterFile).crossfade(true).build(),
                                contentDescription = "Sau",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.5f to Color.Transparent,
                                        1.0f to Color(0xCC0A0A09)
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(colors.bubble)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TRƯỚC",
                                color = Color(0xFFFAFAF6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(colors.flash)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SAU",
                                color = colors.onFlash,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .background(colors.bubble)
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${bestProofTask.title} · ${SnapDateHelper.durationMinutes(bestProofTask)} phút",
                                color = Color(0xFFFAFAF6),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Chưa có cặp ảnh Trước / Sau nào. Khi xong một việc, hãy chọn \"Chụp ảnh kết quả\" để lưu khoảnh khắc thay đổi.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        item(key = "recap_week_photos") {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp)) {
                Text(
                    text = "CẢ TUẦN · ${weekPhotoItems.size} ẢNH",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )

                if (weekPhotoItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Tuần này chưa có ảnh công việc nào hoàn thành.",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        weekPhotoItems.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (colIdx in 0 until 3) {
                                    if (colIdx < row.size) {
                                        val (taskItem, file, wdLabel) = row[colIdx]
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(colors.surface)
                                                .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                                                .clickable { onTaskClick(taskItem) }
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(file)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = taskItem.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xBF0F0F0E))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = wdLabel,
                                                    color = Color(0xFFFAFAF6),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
