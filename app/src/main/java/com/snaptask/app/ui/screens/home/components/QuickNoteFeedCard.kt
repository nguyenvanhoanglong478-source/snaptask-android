package com.snaptask.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuickNoteFeedCard(
    task: Task,
    folder: CustomFolder?,
    nowMillis: Long,
    onCardClick: () -> Unit,
    onFolderClick: () -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val validReminder = task.reminderTime?.takeIf { it > 0L }
    val validDue = task.dueDate?.takeIf { it > 0L }
    val isHighPriority = task.priorityEnum == TaskPriority.HIGH
    val isReminderOverdue = validReminder != null && validReminder < nowMillis
    val isDueOverdue = DateTimeUtils.isOverdue(validDue, task.isCompleted, nowMillis)
    val isOverdue = isReminderOverdue || isDueOverdue

    val statusChipLabel = when {
        isReminderOverdue && validReminder != null -> "Trễ ${formatOverdueDuration(nowMillis - validReminder)}"
        isDueOverdue && validDue != null -> "Quá hạn · ${DateTimeUtils.formatDueLabel(validDue, nowMillis)}"
        validReminder != null -> "Nhắc ${timeFormat.format(Date(validReminder))}"
        validDue != null -> "Hạn ${DateTimeUtils.formatDueLabel(validDue, nowMillis)}"
        isHighPriority -> "Gấp"
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(26.dp))
            .clickable(onClick = onCardClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = SnapTaskIcons.TextAa,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "GHI CHÚ NHANH",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            if (statusChipLabel != null) {
                val isUrgentBadge = isOverdue || isHighPriority
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(if (isUrgentBadge) colors.urgentSoft else colors.elevated)
                        .border(
                            1.dp,
                            if (isUrgentBadge) colors.urgent.copy(alpha = 0.45f) else colors.border,
                            CircleShape
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusChipLabel,
                        color = if (isUrgentBadge) colors.urgent else colors.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = task.title,
            color = colors.text,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        if (task.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.description,
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (validDue != null && validReminder != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hạn chót: ${DateTimeUtils.formatDueLabel(validDue, nowMillis)}",
                color = if (isDueOverdue) colors.urgent else colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val subs = task.parsedSubtasks
        if (subs.isNotEmpty()) {
            val doneCount = subs.count { it.isDone }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Checklist: $doneCount/${subs.size} bước nhỏ",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 96.dp)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(colors.elevated)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(role = Role.Button, onClick = onFolderClick)
                    .padding(horizontal = 12.dp),
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
                            .background(Color(folder?.colorHex ?: 0xFFD4FF3A))
                    )
                    Text(
                        text = task.folderName,
                        color = colors.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.flash)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Hoàn thành ghi chú",
                        onClick = onCompleteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SnapTaskIcons.Check,
                    contentDescription = "Hoàn thành",
                    tint = colors.onFlash,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
