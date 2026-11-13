package com.snaptask.app.ui.screens.calendar

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.components.DockTab
import com.snaptask.app.ui.components.FloatingBottomDock
import com.snaptask.app.ui.components.SnapCheckbox
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.components.SnapSegmentedControl
import com.snaptask.app.ui.components.SquareIconButton
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    isLoading: Boolean,
    initialSelectedDateMillis: Long,
    onOpenHeatmap: () -> Unit,
    onOpenCamera: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleTask: (Task) -> Unit,
    onSelectDockTab: (DockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val todayKey = remember { SnapDateHelper.dayKey(System.currentTimeMillis()) }

    val initCal = remember(initialSelectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = initialSelectedDateMillis }
    }
    var selectedDayKey by rememberSaveable {
        mutableLongStateOf(SnapDateHelper.dayKey(initialSelectedDateMillis))
    }
    var displayYear by rememberSaveable {
        mutableIntStateOf(initCal.get(Calendar.YEAR))
    }
    var displayMonth by rememberSaveable {
        mutableIntStateOf(initCal.get(Calendar.MONTH))
    }
    var viewMode by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(initialSelectedDateMillis) {
        val targetKey = SnapDateHelper.dayKey(initialSelectedDateMillis)
        selectedDayKey = targetKey
        val c = Calendar.getInstance().apply { timeInMillis = targetKey }
        displayYear = c.get(Calendar.YEAR)
        displayMonth = c.get(Calendar.MONTH)
    }

    fun shiftMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        displayYear = cal.get(Calendar.YEAR)
        displayMonth = cal.get(Calendar.MONTH)
        val todayCal = Calendar.getInstance().apply { timeInMillis = todayKey }
        val targetDay = if (
            todayCal.get(Calendar.YEAR) == displayYear &&
            todayCal.get(Calendar.MONTH) == displayMonth
        ) {
            todayCal.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }
        cal.set(Calendar.DAY_OF_MONTH, targetDay)
        selectedDayKey = SnapDateHelper.dayKey(cal.timeInMillis)
    }

    val tasksByDay = remember(tasks) {
        tasks.groupBy { SnapDateHelper.dayKey(SnapDateHelper.taskAnchorMillis(it)) }
    }

    val monthCells = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val leadingEmpty = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalSlots = ((leadingEmpty + daysInMonth + 6) / 7) * 7
        List(totalSlots) { index ->
            val dayNum = index - leadingEmpty + 1
            if (dayNum in 1..daysInMonth) {
                cal.set(Calendar.DAY_OF_MONTH, dayNum)
                Pair(dayNum, SnapDateHelper.dayKey(cal.timeInMillis))
            } else {
                null
            }
        }
    }

    val selectedDayTasks = remember(tasksByDay, selectedDayKey) {
        (tasksByDay[selectedDayKey] ?: emptyList()).sortedWith(
            compareBy<Task> { it.isCompleted }
                .thenBy { SnapDateHelper.taskAnchorMillis(it) }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 136.dp)
        ) {
            item(key = "calendar_top") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lịch",
                        color = colors.text,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp
                    )

                    Box(
                        modifier = Modifier
                            .width(124.dp)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(colors.elevated)
                            .border(1.dp, colors.border, CircleShape)
                            .clickable(role = Role.Button, onClick = onOpenHeatmap),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.HeatMap,
                                contentDescription = null,
                                tint = colors.text,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Heat map",
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SquareIconButton(
                        icon = SnapTaskIcons.ChevronLeft,
                        contentDescription = "Tháng trước",
                        onClick = { shiftMonth(-1) },
                        size = 40.dp,
                        iconSize = 18.dp
                    )
                    Text(
                        text = "Tháng ${displayMonth + 1}, $displayYear",
                        color = colors.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    SquareIconButton(
                        icon = SnapTaskIcons.ChevronRight,
                        contentDescription = "Tháng sau",
                        onClick = { shiftMonth(1) },
                        size = 40.dp,
                        iconSize = 18.dp
                    )
                }

                SnapSegmentedControl(
                    options = listOf("Ảnh kỷ niệm", "Chấm tiến độ"),
                    selectedIndex = viewMode,
                    onSelect = { viewMode = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 6.dp)
                ) {
                    listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { wd ->
                        Text(
                            text = wd,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "calendar_grid") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    monthCells.chunked(7).forEach { weekRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            weekRow.forEach { cell ->
                                if (cell == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f / 1.08f)
                                    )
                                } else {
                                    val (dayNum, dayKeyMillis) = cell
                                    val dayTasks = tasksByDay[dayKeyMillis] ?: emptyList()
                                    CalendarDayCell(
                                        dayNumber = dayNum,
                                        dayTasks = dayTasks,
                                        isToday = dayKeyMillis == todayKey,
                                        isSelected = dayKeyMillis == selectedDayKey,
                                        photoMode = viewMode == 0,
                                        onClick = { selectedDayKey = dayKeyMillis },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "selected_day_agenda") {
                SelectedDayAgendaCard(
                    selectedDayKey = selectedDayKey,
                    todayKey = todayKey,
                    dayTasks = selectedDayTasks,
                    isLoading = isLoading,
                    onAddTask = onOpenCamera,
                    onTaskClick = onTaskClick,
                    onToggleTask = onToggleTask,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(108.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.bg.copy(alpha = 0.94f), colors.bg)
                    )
                )
        )

        FloatingBottomDock(
            activeTab = DockTab.CALENDAR,
            onSelectTab = onSelectDockTab,
            onShutterClick = onOpenCamera,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    dayTasks: List<Task>,
    isToday: Boolean,
    isSelected: Boolean,
    photoMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val doneCount = dayTasks.count { it.isCompleted }
    val openCount = dayTasks.size - doneCount

    val bestPhotoFile = remember(dayTasks, photoMode) {
        if (!photoMode) null
        else {
            val candidatePaths = dayTasks.flatMap {
                listOfNotNull(it.afterImageUri, it.beforeImageUri)
            }.filter { it.isNotBlank() }
            candidatePaths.map { File(it) }.firstOrNull { it.exists() }
        }
    }

    val shape = RoundedCornerShape(14.dp)
    val borderWidth = when {
        isSelected -> 2.5.dp
        isToday -> 2.dp
        else -> 1.dp
    }
    val borderColor = when {
        isSelected -> colors.text
        isToday -> colors.hm4
        else -> colors.border
    }

    Box(
        modifier = modifier
            .aspectRatio(1f / 1.08f)
            .clip(shape)
            .background(colors.surface)
            .border(borderWidth, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        if (photoMode && bestPhotoFile != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(bestPhotoFile)
                    .crossfade(true)
                    .build(),
                contentDescription = "Ngày $dayNumber",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0x4D0A0A09),
                            1.0f to Color(0xCC0A0A09)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(21.dp)
                    .clip(CircleShape)
                    .background(if (isToday) colors.flash else Color(0xBF0F0F0E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNumber.toString(),
                    color = if (isToday) colors.onFlash else Color(0xFFFAFAF6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (doneCount > 0) {
                Text(
                    text = "$doneCount",
                    color = Color(0xFFFAFAF6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            } else if (openCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.urgent)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 5.dp, horizontal = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(23.dp)
                        .clip(CircleShape)
                        .background(if (isToday) colors.flash else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNumber.toString(),
                        color = if (isToday) colors.onFlash else colors.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                when {
                    doneCount > 0 -> {
                        Box(
                            modifier = Modifier
                                .height(18.dp)
                                .clip(CircleShape)
                                .background(colors.flash)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = doneCount.toString(),
                                color = colors.onFlash,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    openCount > 0 -> {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 3.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.urgent)
                        )
                    }
                    else -> Spacer(modifier = Modifier.height(7.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectedDayAgendaCard(
    selectedDayKey: Long,
    todayKey: Long,
    dayTasks: List<Task>,
    isLoading: Boolean,
    onAddTask: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val doneCount = dayTasks.count { it.isCompleted }
    val openCount = dayTasks.size - doneCount
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val subtitle = when {
        isLoading -> "ĐANG TẢI DỮ LIỆU"
        dayTasks.isEmpty() -> "CHƯA CÓ VIỆC"
        selectedDayKey == todayKey -> "HÔM NAY · $doneCount XONG, $openCount CẦN LÀM"
        else -> "$doneCount ĐÃ XONG · $openCount CẦN LÀM"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SnapDateHelper.formatVietnameseDate(selectedDayKey),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(colors.flash)
                    .clickable(role = Role.Button, onClick = onAddTask),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Thêm việc",
                    color = colors.onFlash,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (dayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.elevated)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Chưa có ảnh hay lịch nhắc nào trong ngày này. Bấm + Thêm việc để chụp nhanh.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dayTasks.forEach { task ->
                    val photoFile = remember(task.beforeImageUri, task.afterImageUri) {
                        val path = if (!task.afterImageUri.isNullOrBlank()) task.afterImageUri else task.beforeImageUri
                        if (!path.isNullOrBlank()) File(path) else null
                    }
                    val anchorTime = timeFormat.format(Date(SnapDateHelper.taskAnchorMillis(task)))
                    val detailSub = buildString {
                        if (task.isCompleted) {
                            append("Xong $anchorTime · ${SnapDateHelper.durationMinutes(task)} phút")
                        } else if (task.reminderTime != null) {
                            append("Nhắc $anchorTime")
                        } else {
                            append("Tạo $anchorTime")
                        }
                        append(" · ${task.folderName}")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.elevated)
                            .clickable { onTaskClick(task) }
                            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (photoFile != null && photoFile.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoFile)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(13.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = SnapTaskIcons.TextAa,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                color = if (task.isCompleted) colors.textMuted else colors.text,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = detailSub,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Checkbox,
                                    onClickLabel = "Đổi trạng thái hoàn thành",
                                    onClick = { onToggleTask(task) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            SnapCheckbox(checked = task.isCompleted)
                        }
                    }
                }
            }
        }
    }
}
