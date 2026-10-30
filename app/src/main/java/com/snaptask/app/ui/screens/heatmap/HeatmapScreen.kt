package com.snaptask.app.ui.screens.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.components.DockTab
import com.snaptask.app.ui.components.FloatingBottomDock
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.components.SnapSegmentedControl
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import java.io.File
import java.util.Calendar

private data class HeatWeekColumn(
    val monthHeader: String,
    val dayKeys: List<Long?>
)

private data class TimeSlotBucket(
    val label: String,
    val count: Int,
    val percent: Int
)

@Composable
fun HeatmapScreen(
    tasks: List<Task>,
    isLoading: Boolean,
    folders: List<CustomFolder>,
    onOpenCalendar: (Long) -> Unit,
    onOpenCamera: () -> Unit,
    onSelectDockTab: (DockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val todayKey = remember { SnapDateHelper.dayKey(System.currentTimeMillis()) }
    val currentYear = remember(todayKey) {
        Calendar.getInstance().apply { timeInMillis = todayKey }.get(Calendar.YEAR)
    }

    var rangeIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedFolderFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedDayKey by rememberSaveable { mutableLongStateOf(todayKey) }

    val effectiveFolderFilter = remember(selectedFolderFilter, folders) {
        if (selectedFolderFilter == "ALL" || folders.any { it.name.equals(selectedFolderFilter, ignoreCase = true) }) {
            selectedFolderFilter
        } else {
            "ALL"
        }
    }

    val filteredTasks = remember(tasks, effectiveFolderFilter) {
        if (effectiveFolderFilter == "ALL") tasks
        else tasks.filter { it.folderName.equals(effectiveFolderFilter, ignoreCase = true) }
    }

    val completedFiltered = remember(filteredTasks) {
        filteredTasks.filter { it.isCompleted }
    }

    val completedByDay = remember(completedFiltered) {
        completedFiltered.groupBy { SnapDateHelper.dayKey(it.completedAt ?: it.createdAt) }
    }

    val (currentStreak, bestStreak) = remember(completedFiltered) {
        SnapDateHelper.computeStreaks(completedFiltered)
    }

    val weekColumns = remember(rangeIndex, todayKey, currentYear) {
        buildHeatColumns(rangeIndex = rangeIndex, todayKey = todayKey, currentYear = currentYear)
    }

    val periodDayKeys = remember(weekColumns, todayKey) {
        weekColumns.flatMap { it.dayKeys }.filterNotNull().filter { it <= todayKey }
    }

    val totalDoneInPeriod = remember(periodDayKeys, completedByDay) {
        periodDayKeys.sumOf { completedByDay[it]?.size ?: 0 }
    }

    val activeRatePct = remember(periodDayKeys, completedByDay) {
        if (periodDayKeys.isEmpty()) 0
        else {
            val activeDays = periodDayKeys.count { (completedByDay[it]?.size ?: 0) > 0 }
            ((activeDays * 100f) / periodDayKeys.size).toInt()
        }
    }

    val selectedDayDone = remember(completedByDay, selectedDayKey) {
        completedByDay[selectedDayKey] ?: emptyList()
    }

    val timeSlots = remember(completedFiltered) {
        computeTimeSlotBuckets(completedFiltered)
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
            item(key = "heatmap_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Heat map",
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
                            .clickable(role = Role.Button) { onOpenCalendar(selectedDayKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Calendar,
                                contentDescription = null,
                                tint = colors.text,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Lịch tháng",
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                SnapSegmentedControl(
                    options = listOf("12 tuần gần đây", "Cả năm $currentYear"),
                    selectedIndex = rangeIndex,
                    onSelect = { rangeIndex = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "folder_all") {
                        val selected = effectiveFolderFilter == "ALL"
                        Box(
                            modifier = Modifier
                                .widthIn(min = 92.dp)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(if (selected) colors.text else colors.elevated)
                                .border(1.dp, if (selected) colors.text else colors.border, CircleShape)
                                .clickable(role = Role.Tab) { selectedFolderFilter = "ALL" }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tất cả",
                                color = if (selected) colors.bg else colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(folders, key = { it.id }) { folder ->
                        val selected = effectiveFolderFilter.equals(folder.name, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .widthIn(min = 98.dp)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(if (selected) colors.text else colors.elevated)
                                .border(1.dp, if (selected) colors.text else colors.border, CircleShape)
                                .clickable(role = Role.Tab) { selectedFolderFilter = folder.name }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(folder.colorHex))
                                )
                                Text(
                                    text = folder.name,
                                    color = if (selected) colors.bg else colors.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item(key = "rhythm_stats") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.flash)
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Flame,
                                contentDescription = null,
                                tint = colors.onFlash,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CHUỖI",
                                color = colors.onFlash,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$currentStreak ngày",
                            color = colors.onFlash,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Kỷ lục $bestStreak",
                            color = colors.onFlash.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "ĐÃ XONG",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = totalDoneInPeriod.toString(),
                            color = colors.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (rangeIndex == 0) "12 tuần qua" else "năm $currentYear",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "ĐỀU ĐẶN",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$activeRatePct%",
                            color = colors.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "ngày có việc",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item(key = "matrix_card") {
                val cellSize: Dp = if (rangeIndex == 0) 19.dp else 13.dp
                val cellGap: Dp = if (rangeIndex == 0) 4.dp else 3.dp
                val matrixScroll = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 12.dp)
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
                        Text(
                            text = "Mỗi ô là 1 ngày (bấm để xem)",
                            color = colors.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (effectiveFolderFilter == "ALL") "Tất cả thư mục" else effectiveFolderFilter,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(cellGap)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            listOf("T2", "", "T4", "", "T6", "", "CN").forEach { label ->
                                Box(
                                    modifier = Modifier.height(cellSize),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = label,
                                        color = colors.textMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(matrixScroll),
                            horizontalArrangement = Arrangement.spacedBy(cellGap)
                        ) {
                            weekColumns.forEach { col ->
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(cellGap)
                                ) {
                                    Box(
                                        modifier = Modifier.height(16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (col.monthHeader.isNotEmpty()) {
                                            Text(
                                                text = col.monthHeader,
                                                color = colors.textMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    col.dayKeys.forEach { dayKeyOrNull ->
                                        if (dayKeyOrNull == null) {
                                            Spacer(modifier = Modifier.size(cellSize))
                                        } else {
                                            val count = completedByDay[dayKeyOrNull]?.size ?: 0
                                            val isFuture = dayKeyOrNull > todayKey
                                            val isSelected = dayKeyOrNull == selectedDayKey
                                            val fill = when {
                                                count <= 0 -> colors.hm0
                                                count <= 2 -> colors.hm1
                                                count <= 4 -> colors.hm2
                                                count <= 6 -> colors.hm3
                                                else -> colors.hm4
                                            }
                                            val cellShape = RoundedCornerShape(if (rangeIndex == 0) 5.dp else 3.5.dp)

                                            Box(
                                                modifier = Modifier
                                                    .size(cellSize)
                                                    .alpha(if (isFuture) 0.32f else 1f)
                                                    .clip(cellShape)
                                                    .background(fill)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = when {
                                                            isSelected -> colors.text
                                                            count == 0 -> colors.hmBorder
                                                            else -> Color.Transparent
                                                        },
                                                        shape = cellShape
                                                    )
                                                    .clickable(enabled = !isFuture) {
                                                        selectedDayKey = dayKeyOrNull
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Số việc xong / ngày",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "0",
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            listOf(colors.hm0, colors.hm1, colors.hm2, colors.hm3, colors.hm4).forEachIndexed { idx, c ->
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(RoundedCornerShape(3.5.dp))
                                        .background(c)
                                        .border(
                                            width = if (idx == 0) 1.dp else 0.dp,
                                            color = if (idx == 0) colors.hmBorder else Color.Transparent,
                                            shape = RoundedCornerShape(3.5.dp)
                                        )
                                )
                            }
                            Text(
                                text = "7+",
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(key = "selected_day_detail") {
                val avgMins = remember(selectedDayDone) {
                    if (selectedDayDone.isEmpty()) 0
                    else selectedDayDone.map { SnapDateHelper.durationMinutes(it) }.average().toInt()
                }
                val dayThumbs = remember(selectedDayDone) {
                    selectedDayDone.mapNotNull { t ->
                        val p = if (!t.afterImageUri.isNullOrBlank()) t.afterImageUri else t.beforeImageUri
                        if (!p.isNullOrBlank()) File(p).takeIf { it.exists() } else null
                    }.take(4)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SnapDateHelper.formatVietnameseDate(selectedDayKey),
                                color = colors.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    isLoading -> "Đang tải dữ liệu…"
                                    selectedDayDone.isEmpty() -> "0 việc hoàn thành · Ngày nghỉ ngơi"
                                    else -> "${selectedDayDone.size} việc hoàn thành · Trung bình $avgMins phút"
                                },
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(colors.elevated)
                                .border(1.dp, colors.border, CircleShape)
                                .clickable(role = Role.Button) { onOpenCalendar(selectedDayKey) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Xem lịch",
                                color = colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (dayThumbs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dayThumbs.forEach { file ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(file)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            item(key = "time_of_day_chart") {
                val peak = timeSlots.maxByOrNull { it.count }
                val hasCompletedAny = completedFiltered.isNotEmpty()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Khung giờ hay xong việc nhất",
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (hasCompletedAny && peak != null) {
                            "Bạn hoàn thành nhiều nhất vào khung ${peak.label} (${peak.percent}%)."
                        } else {
                            "Hoàn thành việc đầu tiên để xem khung giờ hiệu quả của bạn."
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        timeSlots.forEach { slot ->
                            val isPeak = hasCompletedAny && peak?.label == slot.label && slot.count > 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = slot.label,
                                    color = if (isPeak) colors.text else colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isPeak) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(88.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(colors.elevated)
                                ) {
                                    if (slot.percent > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((slot.percent / 100f).coerceIn(0.06f, 1f))
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(if (isPeak) colors.hm4 else colors.hm2)
                                        )
                                    }
                                }

                                Text(
                                    text = "${slot.percent}%",
                                    color = if (isPeak) colors.text else colors.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(38.dp)
                                )
                            }
                        }
                    }
                }
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

private fun buildHeatColumns(
    rangeIndex: Int,
    todayKey: Long,
    currentYear: Int
): List<HeatWeekColumn> {
    val cal = Calendar.getInstance()
    if (rangeIndex == 0) {
        cal.timeInMillis = todayKey
        val offsetFromMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -offsetFromMonday)
        cal.add(Calendar.WEEK_OF_YEAR, -11)

        var prevMonth = -1
        return List(12) {
            val days = List(7) { dayIdx ->
                if (dayIdx > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
                SnapDateHelper.dayKey(cal.timeInMillis)
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val firstDayCal = Calendar.getInstance().apply { timeInMillis = days.first() }
            val m = firstDayCal.get(Calendar.MONTH) + 1
            val header = if (m != prevMonth) {
                prevMonth = m
                "T$m"
            } else ""
            HeatWeekColumn(monthHeader = header, dayKeys = days)
        }
    } else {
        val jan1 = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dec31 = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dec31Key = SnapDateHelper.dayKey(dec31.timeInMillis)

        val cursor = Calendar.getInstance().apply {
            timeInMillis = jan1.timeInMillis
            val offsetFromMonday = (get(Calendar.DAY_OF_WEEK) + 5) % 7
            add(Calendar.DAY_OF_YEAR, -offsetFromMonday)
        }

        val cols = mutableListOf<HeatWeekColumn>()
        var prevMonth = -1
        while (SnapDateHelper.dayKey(cursor.timeInMillis) <= dec31Key) {
            var monthLabel = ""
            val days = List(7) {
                val yr = cursor.get(Calendar.YEAR)
                val key = if (yr == currentYear) SnapDateHelper.dayKey(cursor.timeInMillis) else null
                if (yr == currentYear && cursor.get(Calendar.DAY_OF_MONTH) <= 7) {
                    val m = cursor.get(Calendar.MONTH) + 1
                    if (m != prevMonth) {
                        prevMonth = m
                        monthLabel = "T$m"
                    }
                }
                cursor.add(Calendar.DAY_OF_YEAR, 1)
                key
            }
            cols.add(HeatWeekColumn(monthHeader = monthLabel, dayKeys = days))
        }
        return cols
    }
}

private fun computeTimeSlotBuckets(completedTasks: List<Task>): List<TimeSlotBucket> {
    var morning = 0
    var afternoon = 0
    var evening = 0
    var lateNight = 0
    val cal = Calendar.getInstance()
    completedTasks.forEach { t ->
        cal.timeInMillis = t.completedAt ?: t.createdAt
        when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 6..11 -> morning++
            in 12..17 -> afternoon++
            in 18..21 -> evening++
            else -> lateNight++
        }
    }
    val total = completedTasks.size.coerceAtLeast(1)
    val hasAny = completedTasks.isNotEmpty()
    return listOf(
        TimeSlotBucket("Sáng 6-12h", morning, if (hasAny) ((morning * 100f) / total).toInt() else 0),
        TimeSlotBucket("Chiều 12-18h", afternoon, if (hasAny) ((afternoon * 100f) / total).toInt() else 0),
        TimeSlotBucket("Tối 18-22h", evening, if (hasAny) ((evening * 100f) / total).toInt() else 0),
        TimeSlotBucket("Khuya 22h+", lateNight, if (hasAny) ((lateNight * 100f) / total).toInt() else 0)
    )
}
