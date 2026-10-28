package com.snaptask.app.ui.screens.home.components

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.data.local.Converters
import com.snaptask.app.domain.model.SubTask
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.screens.editor.ReminderPickerDialog
import com.snaptask.app.ui.theme.AccentPrimary
import com.snaptask.app.ui.theme.AuroraGradient
import com.snaptask.app.ui.theme.BgElevated
import com.snaptask.app.ui.theme.BgPrimary
import com.snaptask.app.ui.theme.BgSurface
import com.snaptask.app.ui.theme.BorderHighlight
import com.snaptask.app.ui.theme.BorderSubtle
import com.snaptask.app.ui.theme.StatusAmber
import com.snaptask.app.ui.theme.StatusUrgent
import com.snaptask.app.ui.theme.TextMuted
import com.snaptask.app.ui.theme.TextPrimary
import com.snaptask.app.ui.theme.TextSecondary
import com.snaptask.app.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteBottomSheet(
    onDismissRequest: () -> Unit,
    onSave: (Task) -> Unit,
    existingFolders: List<String> = listOf("Cá nhân", "Công việc", "Học tập", "Mua sắm")
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subtasks by remember { mutableStateOf<List<SubTask>>(emptyList()) }
    var newSubtaskText by remember { mutableStateOf("") }

    var selectedFolder by remember { mutableStateOf(existingFolders.firstOrNull() ?: "Cá nhân") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var isPinned by remember { mutableStateOf(true) }

    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var reminderType by remember { mutableStateOf("NONE") }
    var showReminderDialog by remember { mutableStateOf(false) }

    val defaultFolderOptions = remember(existingFolders) {
        val merged = (listOf("Cá nhân", "Công việc", "Học tập", "Mua sắm") + existingFolders).distinct()
        merged
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = BgSurface,
        tonalElevation = 8.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(BorderHighlight, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AccentPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✍️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ghi chú nhanh",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .background(BgElevated, CircleShape)
                        .border(1.dp, BorderSubtle, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Đóng",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("Nhập việc cần làm...", color = TextMuted, fontSize = 16.sp)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = BorderSubtle
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description / Note Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = {
                    Text("Ghi chú chi tiết (không bắt buộc)...", color = TextMuted, fontSize = 13.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextSecondary,
                    fontSize = 13.sp
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = BorderSubtle
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Checklist / Sub-tasks Section
            Text(
                text = "Danh sách việc nhỏ (Checklist)",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Subtasks list
            subtasks.forEachIndexed { index, subtask ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(BgElevated, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (subtask.isDone) Icons.Filled.Check else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (subtask.isDone) AccentPrimary else TextMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                subtasks = subtasks.toMutableList().also {
                                    it[index] = subtask.copy(isDone = !subtask.isDone)
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtask.title,
                        color = if (subtask.isDone) TextMuted else TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            subtasks = subtasks.toMutableList().also { it.removeAt(index) }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Xoá",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Add Subtask Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Thêm bước thực hiện...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BgElevated,
                        unfocusedContainerColor = BgElevated,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            subtasks = subtasks + SubTask(id = java.util.UUID.randomUUID().toString(), title = newSubtaskText.trim(), isDone = false)
                            newSubtaskText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(AccentPrimary, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Thêm", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folder Selection Chips
            Text(
                text = "Thư mục phân loại",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(defaultFolderOptions) { folder ->
                    val isSelected = selectedFolder == folder
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentPrimary else BgElevated)
                            .border(1.dp, if (isSelected) AccentPrimary else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { selectedFolder = folder }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = folder,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Priority Selection
            Text(
                text = "Mức độ ưu tiên",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { p ->
                    val isSelected = priority == p
                    val label = when (p) {
                        TaskPriority.LOW -> "Thấp"
                        TaskPriority.MEDIUM -> "Bình thường"
                        TaskPriority.HIGH -> "Gấp"
                    }
                    val badgeColor = when (p) {
                        TaskPriority.LOW -> TextMuted
                        TaskPriority.MEDIUM -> StatusAmber
                        TaskPriority.HIGH -> StatusUrgent
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) badgeColor.copy(alpha = 0.25f) else BgElevated)
                            .border(1.dp, if (isSelected) badgeColor else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { priority = p }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) badgeColor else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reminder & Widget Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reminder Trigger Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (reminderTime != null) AccentPrimary.copy(alpha = 0.2f) else BgElevated)
                        .border(1.dp, if (reminderTime != null) AccentPrimary else BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { showReminderDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (reminderType == "ALARM") Icons.Filled.Alarm else Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = if (reminderTime != null) AccentPrimary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reminderTime?.let { DateTimeUtils.formatDateTime(it) } ?: "Hẹn giờ nhắc",
                            color = if (reminderTime != null) AccentPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Pin To Widget Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = if (isPinned) AccentPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ghim Widget",
                        color = if (isPinned) TextPrimary else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = BgElevated
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuroraGradient)
                    .clickable {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Vui lòng nhập tiêu đề task!", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        val newTask = Task(
                            title = title.trim(),
                            description = description.trim(),
                            beforeImageUri = "",
                            isCompleted = false,
                            isPinnedToWidget = isPinned,
                            isFavorite = false,
                            folderName = selectedFolder,
                            priority = priority.name,
                            subTasksJson = Converters().fromSubTaskList(subtasks),
                            reminderTime = reminderTime,
                            reminderType = reminderType
                        )
                        onSave(newTask)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lưu ghi chú",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showReminderDialog) {
        ReminderPickerDialog(
            initialReminderTime = reminderTime,
            initialReminderType = reminderType,
            onConfirm = { time, type ->
                reminderTime = time
                reminderType = type
                showReminderDialog = false
            },
            onDismiss = {
                showReminderDialog = false
            }
        )
    }
}
