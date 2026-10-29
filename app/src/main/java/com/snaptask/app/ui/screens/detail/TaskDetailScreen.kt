package com.snaptask.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.data.local.Converters
import com.snaptask.app.domain.model.SubTask
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.screens.editor.ReminderPickerDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun TaskDetailScreen(
    task: Task,
    onBack: () -> Unit,
    onSaveTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onOpenAfterCamera: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val isDark = isSystemInDarkTheme()
    val converters = remember { Converters() }
    val scrollState = rememberScrollState()

    var currentTitle by remember { mutableStateOf(task.title) }
    var currentDescription by remember { mutableStateOf(task.description) }
    var currentCompletionCaption by remember { mutableStateOf(task.completionCaption ?: "") }
    var isCompleted by remember { mutableStateOf(task.isCompleted) }
    var isPinned by remember { mutableStateOf(task.isPinnedToWidget) }
    var reminderTime by remember { mutableStateOf(task.reminderTime) }
    var reminderType by remember { mutableStateOf(task.reminderType) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var subtasks by remember { mutableStateOf(task.parsedSubtasks) }
    var newSubtaskText by remember { mutableStateOf("") }

    val completedCount = subtasks.count { it.isDone }
    val progressFraction = if (subtasks.isEmpty()) {
        if (isCompleted) 1f else 0f
    } else {
        completedCount.toFloat() / subtasks.size.toFloat()
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.text,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Chi tiết công việc",
                    color = colors.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Ghim ra widget",
                            tint = if (isPinned) colors.flash else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDeleteTask(task) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Xóa task",
                            tint = colors.urgent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            // 1. Before & After Photo Comparison
            Text(
                text = "HÌNH ẢNH KHOẢNH KHẮC",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            val hasAfter = !task.afterImageUri.isNullOrBlank() && File(task.afterImageUri).exists()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Before Photo (Square 1:1)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val beforeFile = File(task.beforeImageUri)
                        if (beforeFile.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(beforeFile)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Before Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Chưa có ảnh", color = colors.textMuted, fontSize = 12.sp)
                            }
                        }

                        // Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color(0xD9000000), RoundedCornerShape(8.dp)) // Dark scrim overlay for timestamp on photo
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "BEFORE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // After Photo (Square 1:1) or Snap Action
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clickable { onOpenAfterCamera(task) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasAfter) colors.done else colors.border
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (hasAfter) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(File(task.afterImageUri!!))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "After Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(if (isDark) colors.done.copy(alpha = 0.2f) else colors.done, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "AFTER", color = if (isDark) colors.done else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Chụp sau",
                                    tint = colors.flash,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Chụp kết quả\n(After)",
                                    color = colors.text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Task Title Input
            OutlinedTextField(
                value = currentTitle,
                onValueChange = { currentTitle = it },
                label = { Text("Tiêu đề", color = colors.textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.flash,
                    unfocusedBorderColor = colors.border
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Notion Metadata Row (Status + Priority + Tag)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Status Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCompleted = !isCompleted },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Trạng thái", color = colors.textSecondary, fontSize = 13.sp)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isCompleted) (if (isDark) colors.done.copy(alpha = 0.16f) else colors.done) else colors.warning.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isCompleted) (if (isDark) colors.done else Color.White) else colors.warning,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "Đã hoàn thành" else "Đang thực hiện",
                                color = if (isCompleted) (if (isDark) colors.done else Color.White) else colors.warning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Priority Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Ưu tiên", color = colors.textSecondary, fontSize = 13.sp)
                        Text(
                            text = task.priorityEnum.label,
                            color = when (task.priorityEnum) {
                                TaskPriority.LOW -> colors.done
                                TaskPriority.MEDIUM -> colors.warning
                                TaskPriority.HIGH -> colors.urgent
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reminder Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReminderPicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Nhắc nhở / Báo thức", color = colors.textSecondary, fontSize = 13.sp)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (reminderTime != null) colors.flash else colors.elevated,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (reminderTime != null) colors.flash else colors.border,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (reminderTime != null) "${if (reminderType == "ALARM") "Báo thức" else "Nhắc"}: ${DateTimeUtils.formatDateTime(reminderTime!!)}" else "Chưa đặt",
                                color = if (reminderTime != null) colors.onFlash else colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Created At Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Tạo lúc", color = colors.textSecondary, fontSize = 13.sp)
                        Text(
                            text = dateFormat.format(Date(task.createdAt)),
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Notion Sub-tasks / Checklist Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHECKLIST TIẾN ĐỘ",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${(progressFraction * 100).toInt()}% (${completedCount}/${subtasks.size})",
                    color = if (isDark) colors.flash else colors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isDark) colors.flash else colors.text,
                trackColor = colors.elevated
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-tasks Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (subtasks.isEmpty()) {
                        Text(
                            text = "Chưa có checklist con. Thêm các bước cụ thể bên dưới:",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        subtasks.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val updated = subtasks.toMutableList()
                                        updated[index] = item.copy(isDone = !item.isDone)
                                        subtasks = updated
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = "Toggle Subtask",
                                        tint = if (item.isDone) colors.done else colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.title,
                                    color = if (item.isDone) colors.textMuted else colors.text,
                                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val updated = subtasks.toMutableList()
                                        updated.removeAt(index)
                                        subtasks = updated
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Xóa subtask",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Add Sub-task row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text("Thêm bước việc cần làm...", color = colors.textMuted, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text,
                                focusedContainerColor = colors.bg,
                                unfocusedContainerColor = colors.bg,
                                focusedBorderColor = colors.flash,
                                unfocusedBorderColor = colors.border
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks = subtasks + SubTask(
                                        id = UUID.randomUUID().toString(),
                                        title = newSubtaskText.trim(),
                                        isDone = false
                                    )
                                    newSubtaskText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(colors.flash, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Thêm", tint = colors.onFlash)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Notes & Completion Caption
            Text(
                text = "GHI CHÚ & CẢM NGHĨ HOÀN THÀNH",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = currentCompletionCaption,
                onValueChange = { currentCompletionCaption = it },
                placeholder = { Text("Ghi chú tiến độ hoặc cảm nhận khi hoàn thành...", color = colors.textMuted, fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.flash,
                    unfocusedBorderColor = colors.border
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Save & Update Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.flash)
                    .clickable {
                        val updatedTask = task.copy(
                            title = currentTitle.ifBlank { "Việc cần làm" },
                            description = currentDescription,
                            completionCaption = currentCompletionCaption,
                            isCompleted = isCompleted,
                            isPinnedToWidget = isPinned,
                            reminderTime = reminderTime,
                            reminderType = reminderType,
                            subTasksJson = converters.fromSubTaskList(subtasks),
                            completedAt = if (isCompleted && task.completedAt == null) System.currentTimeMillis() else task.completedAt
                        )
                        onSaveTask(updatedTask)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lưu & Cập nhật Task",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showReminderPicker) {
        ReminderPickerDialog(
            initialReminderTime = reminderTime,
            initialReminderType = reminderType,
            onConfirm = { time, type ->
                reminderTime = time
                reminderType = type
                showReminderPicker = false
            },
            onDismiss = { showReminderPicker = false }
        )
    }
}
