package com.snaptask.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.theme.snapColors
import java.util.Calendar

@Composable
fun WeekStripRow(
    tasks: List<Task>,
    todayKey: Long,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors

    val weekDays = remember(todayKey) {
        val cal = Calendar.getInstance().apply { timeInMillis = todayKey }
        val offsetFromMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -offsetFromMonday)
        List(7) { idx ->
            if (idx > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
            val key = SnapDateHelper.dayKey(cal.timeInMillis)
            Triple(
                key,
                SnapDateHelper.vietnameseWeekdayShort(cal.get(Calendar.DAY_OF_WEEK)),
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    val countsByDay = remember(tasks) {
        tasks.groupBy { SnapDateHelper.dayKey(SnapDateHelper.taskAnchorMillis(it)) }
            .mapValues { (_, list) -> list.size }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekDays.forEach { (dayKeyMillis, wdShort, dayNum) ->
            val isToday = dayKeyMillis == todayKey
            val count = (countsByDay[dayKeyMillis] ?: 0).coerceAtMost(3)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isToday) colors.flash else Color.Transparent)
                    .clickable(role = Role.Button) { onSelectDay(dayKeyMillis) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = wdShort,
                    color = if (isToday) colors.onFlash else colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dayNum.toString(),
                    color = if (isToday) colors.onFlash else colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.height(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(count) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isToday) colors.onFlash else colors.hm4)
                        )
                    }
                }
            }
        }
    }
}
