package com.snaptask.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import java.util.Calendar

enum class DockTab(val label: String) {
    TODAY("Hôm nay"),
    CALENDAR("Lịch & Nhiệt độ"),
    BOARD("Bảng tiến độ"),
    FOLDERS("Thư mục")
}

// Symmetrical 5-slot dock (324x72dp): 13 + 48 + 12 + 48 + 12 + 58 + 12 + 48 + 12 + 48 + 13 = 324dp.
// Center shutter sits at exact X = 162dp (50%) and tabs stay locked at 48x48dp on switch.
@Composable
fun FloatingBottomDock(
    activeTab: DockTab,
    onSelectTab: (DockTab) -> Unit,
    onShutterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors

    Box(
        modifier = modifier
            .width(324.dp)
            .height(72.dp)
            .shadow(16.dp, CircleShape, ambientColor = Color(0x40000000), spotColor = Color(0x40000000))
            .clip(CircleShape)
            .background(colors.dock)
            .border(1.dp, colors.border, CircleShape)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockIconTab(
                icon = SnapTaskIcons.Today,
                label = DockTab.TODAY.label,
                selected = activeTab == DockTab.TODAY,
                onClick = { onSelectTab(DockTab.TODAY) }
            )
            DockIconTab(
                icon = SnapTaskIcons.Calendar,
                label = DockTab.CALENDAR.label,
                selected = activeTab == DockTab.CALENDAR,
                onClick = { onSelectTab(DockTab.CALENDAR) }
            )

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .border(3.dp, colors.flash, CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Chụp việc mới",
                        onClick = onShutterClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.flash)
                )
            }

            DockIconTab(
                icon = SnapTaskIcons.Board,
                label = DockTab.BOARD.label,
                selected = activeTab == DockTab.BOARD,
                onClick = { onSelectTab(DockTab.BOARD) }
            )
            DockIconTab(
                icon = SnapTaskIcons.Folder,
                label = DockTab.FOLDERS.label,
                selected = activeTab == DockTab.FOLDERS,
                onClick = { onSelectTab(DockTab.FOLDERS) }
            )
        }
    }
}

@Composable
private fun DockIconTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.snapColors
    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.text else Color.Transparent,
        label = "DockTabBg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.bg else colors.textSecondary,
        label = "DockTabIcon"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

// Equal-column segmented control so switching tabs never changes button width.
@Composable
fun SnapSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.elevated)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) colors.surface else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(idx) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) colors.text else colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color = MaterialTheme.snapColors.elevated,
    contentColor: Color = MaterialTheme.snapColors.text,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 16.dp,
    containerColor: Color = MaterialTheme.snapColors.elevated,
    contentColor: Color = MaterialTheme.snapColors.text,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.32f)
            .clip(RoundedCornerShape(11.dp))
            .background(containerColor)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun SnapSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        label = "SnapSwitchThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.flash else colors.elevated,
        label = "SnapSwitchTrack"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.onFlash else colors.textSecondary,
        label = "SnapSwitchKnob"
    )

    Box(
        modifier = modifier
            .width(46.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, if (checked) Color.Transparent else colors.border, CircleShape)
            .clickable(
                role = Role.Switch,
                onClickLabel = contentDescription,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
fun SnapCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) colors.flash else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (checked) colors.flash else colors.textMuted,
                shape = RoundedCornerShape(7.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = SnapTaskIcons.Check,
                contentDescription = null,
                tint = colors.onFlash,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

object SnapDateHelper {

    fun dayKey(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun taskAnchorMillis(task: Task): Long {
        return when {
            task.isCompleted && task.completedAt != null -> task.completedAt
            task.reminderTime != null -> task.reminderTime
            task.dueDate != null -> task.dueDate
            else -> task.createdAt
        }
    }

    fun isSameDay(aMillis: Long, bMillis: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = aMillis }
        val b = Calendar.getInstance().apply { timeInMillis = bMillis }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun vietnameseWeekdayFull(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.MONDAY -> "Thứ Hai"
        Calendar.TUESDAY -> "Thứ Ba"
        Calendar.WEDNESDAY -> "Thứ Tư"
        Calendar.THURSDAY -> "Thứ Năm"
        Calendar.FRIDAY -> "Thứ Sáu"
        Calendar.SATURDAY -> "Thứ Bảy"
        Calendar.SUNDAY -> "Chủ Nhật"
        else -> "Hôm nay"
    }

    fun vietnameseWeekdayShort(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.MONDAY -> "T2"
        Calendar.TUESDAY -> "T3"
        Calendar.WEDNESDAY -> "T4"
        Calendar.THURSDAY -> "T5"
        Calendar.FRIDAY -> "T6"
        Calendar.SATURDAY -> "T7"
        Calendar.SUNDAY -> "CN"
        else -> "T2"
    }

    fun formatVietnameseDate(timeMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val wd = vietnameseWeekdayFull(cal.get(Calendar.DAY_OF_WEEK))
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        return "$wd, $day tháng $month"
    }

    fun durationMinutes(task: Task): Int {
        val end = task.completedAt ?: return 1
        return (((end - task.createdAt) / 60000L).toInt()).coerceAtLeast(1)
    }

    // Returns Pair(currentStreakDays, bestStreakDays) from completed tasks.
    fun computeStreaks(tasks: List<Task>): Pair<Int, Int> {
        val completedDays = tasks
            .filter { it.isCompleted }
            .map { dayKey(it.completedAt ?: it.createdAt) }
            .toSortedSet()

        if (completedDays.isEmpty()) return Pair(0, 0)

        val oneDayMs = 86_400_000L
        var best = 1
        var run = 1
        val sorted = completedDays.toList()
        for (i in 1 until sorted.size) {
            val diffDays = Math.round((sorted[i] - sorted[i - 1]).toDouble() / oneDayMs)
            if (diffDays == 1L) {
                run++
                if (run > best) best = run
            } else if (diffDays > 1L) {
                run = 1
            }
        }

        val todayKey = dayKey(System.currentTimeMillis())
        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = todayKey
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayKey = dayKey(yesterdayCal.timeInMillis)

        var cursor = when {
            todayKey in completedDays -> todayKey
            yesterdayKey in completedDays -> yesterdayKey
            else -> return Pair(0, best)
        }

        var current = 0
        val cal = Calendar.getInstance()
        while (cursor in completedDays) {
            current++
            cal.timeInMillis = cursor
            cal.add(Calendar.DAY_OF_YEAR, -1)
            cursor = dayKey(cal.timeInMillis)
        }

        return Pair(current, maxOf(best, current))
    }
}
