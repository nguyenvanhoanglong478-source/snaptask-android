package com.snaptask.app.ui.screens.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
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
    val context = LocalContext.current
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
    var showMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    val completedCount = subtasks.count { it.isDone }
    val progressFraction = if (subtasks.isEmpty()) {
        if (isCompleted) 1f else 0f
    } else {
        completedCount.toFloat() / subtasks.size.toFloat()
    }

    val beforeFile = remember(task.beforeImageUri) { File(task.beforeImageUri) }
    val hasBefore = beforeFile.exists()
    val afterFile = remember(task.afterImageUri) {
        if (!task.afterImageUri.isNullOrBlank()) File(task.afterImageUri) else null
    }
    val hasAfter = afterFile != null && afterFile.exists()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            // 1. Top Bar: [<] on Left, Status Pill in Center, [...] on Right (Page 6)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.text,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center: Status Pill (Green "✓ Đã xong" or "Chưa xong")
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.done.copy(alpha = 0.16f))
                            .border(1.dp, colors.done.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable {
                                isCompleted = false
                                onSaveTask(task.copy(isCompleted = false))
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = colors.done,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Đã xong",
                                color = colors.done,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.elevated)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .clickable {
                                isCompleted = true
                                onSaveTask(task.copy(isCompleted = true))
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Chưa xong",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right: More Options Button (...)
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Tùy chọn",
                            tint = colors.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isPinned) "Bỏ ghim widget" else "Ghim ra widget", color = colors.text) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    tint = if (isPinned) colors.flash else colors.textSecondary
                                )
                            },
                            onClick = {
                                isPinned = !isPinned
                                onSaveTask(task.copy(isPinnedToWidget = isPinned))
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Chỉnh sửa", color = colors.text) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = colors.textSecondary) },
                            onClick = {
                                isEditing = !isEditing
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Xoá việc", color = colors.urgent) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = colors.urgent) },
                            onClick = {
                                showMenu = false
                                onDeleteTask(task)
                            }
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
            // 2. Title & Timestamp Meta (Page 6)
            if (isEditing) {
                OutlinedTextField(
                    value = currentTitle,
                    onValueChange = { currentTitle = it },
                    label = { Text("Tiêu đề công việc", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedContainerColor = colors.elevated,
                        unfocusedContainerColor = colors.elevated,
                        focusedBorderColor = colors.flash,
                        unfocusedBorderColor = colors.border
                    )
                )
            } else {
                Text(
                    text = currentTitle,
                    color = colors.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 34.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val timeMeta = if (isCompleted && task.completedAt != null) {
                val startStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.createdAt))
                val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.completedAt))
                val durationMin = maxOf(1, ((task.completedAt - task.createdAt) / 60000).toInt())
                "Bắt đầu $startStr · Xong $endStr · mất $durationMin phút"
            } else if (task.reminderTime != null) {
                "Hạn ${DateTimeUtils.formatDateTime(task.reminderTime)} · ${task.folderName}"
            } else "Hôm nay · ${task.folderName}"

            Text(
                text = timeMeta,
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Before / After Split Photo Comparison View (Page 6)
            if (hasBefore && hasAfter) {
                // Split Before & After Unified Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left: BEFORE Photo
                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(beforeFile).crossfade(true).build(),
                                    contentDescription = "Trước",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // TRƯỚC Pill
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                        .background(Color(0xD9000000), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "TRƯỚC", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Divider line
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxSize()
                                    .background(colors.border)
                            )

                            // Right: AFTER Photo
                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(afterFile).crossfade(true).build(),
                                    contentDescription = "Sau",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // SAU Pill (Flash Yellow background, #17140A text as in Page 6)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .background(colors.flash, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "SAU", color = colors.onFlash, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Center Split Handle Icon (< >)
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.5.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CompareArrows,
                                contentDescription = "So sánh",
                                tint = colors.text,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else if (hasBefore) {
                // Only Before Photo Present -> Prompt for After photo
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clickable { onOpenAfterCamera(task) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(beforeFile).crossfade(true).build(),
                            contentDescription = "Trước",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color(0xD9000000), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "TRƯỚC", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        // Bottom CTA pill over image
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.flash)
                                .clickable { onOpenAfterCamera(task) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Chụp ảnh kết quả (SAU)",
                                color = colors.onFlash,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Caption Quote Box: “Xong rồi, giờ có chỗ đặt laptop thoải mái.” (Page 6)
            val quoteText = currentCompletionCaption.ifBlank {
                if (isCompleted) "“Xong rồi, hoàn thành xuất sắc!”" else task.description
            }
            if (quoteText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.elevated)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = quoteText,
                        color = colors.text,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Metadata Tags Row: [Cao] [Nhà] [Hạn 29/10 · 18:00] (Page 6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Pill
                val priorityBg = when (task.priorityEnum) {
                    TaskPriority.HIGH -> colors.urgent.copy(alpha = 0.2f)
                    TaskPriority.MEDIUM -> colors.warning.copy(alpha = 0.2f)
                    TaskPriority.LOW -> colors.elevated
                }
                val priorityText = when (task.priorityEnum) {
                    TaskPriority.HIGH -> colors.urgent
                    TaskPriority.MEDIUM -> colors.warning
                    TaskPriority.LOW -> colors.textSecondary
                }
                Box(
                    modifier = Modifier
                        .background(priorityBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(text = task.priorityEnum.label, color = priorityText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Folder Pill
                Box(
                    modifier = Modifier
                        .background(colors.elevated, RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(text = task.folderName, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Due Date Pill
                val dueStr = if (task.reminderTime != null) {
                    "Hạn ${DateTimeUtils.formatDateTime(task.reminderTime)}"
                } else "Hôm nay"
                Box(
                    modifier = Modifier
                        .background(colors.elevated, RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(text = dueStr, color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Subtasks Checklist (Page 6)
            if (subtasks.isNotEmpty() || isEditing) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    subtasks.forEachIndexed { index, st ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = subtasks.toMutableList()
                                    updated[index] = st.copy(isDone = !st.isDone)
                                    subtasks = updated
                                    onSaveTask(task.copy(subTasksJson = converters.fromSubTaskList(updated)))
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (st.isDone) colors.done else colors.elevated)
                                    .border(1.dp, if (st.isDone) colors.done else colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (st.isDone) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF17140A),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = st.title,
                                color = if (st.isDone) colors.textMuted else colors.text,
                                textDecoration = if (st.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Add Subtask Row if editing or has checklist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text("Thêm bước nhỏ...", color = colors.textMuted, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text,
                                focusedContainerColor = colors.elevated,
                                unfocusedContainerColor = colors.elevated,
                                focusedBorderColor = colors.flash,
                                unfocusedBorderColor = colors.border
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.flash)
                                .clickable {
                                    if (newSubtaskText.isNotBlank()) {
                                        val updated = subtasks + SubTask(UUID.randomUUID().toString(), newSubtaskText.trim(), false)
                                        subtasks = updated
                                        newSubtaskText = ""
                                        onSaveTask(task.copy(subTasksJson = converters.fromSubTaskList(updated)))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Thêm", tint = colors.onFlash, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 7. Bottom Action Bar: [Chỉnh sửa] & [Chia sẻ Before / After] (Page 6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Secondary Button [Chỉnh sửa]
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.elevated)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable {
                            isEditing = !isEditing
                            if (!isEditing) {
                                onSaveTask(task.copy(title = currentTitle, description = currentDescription))
                                Toast.makeText(context, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEditing) "Lưu xong" else "Chỉnh sửa",
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Right: Primary CTA [Chia sẻ Before / After] or [Chụp kết quả]
                val ctaText = if (hasAfter) "Chia sẻ Before / After" else "Chụp ảnh kết quả (SAU)"
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.flash)
                        .clickable {
                            if (hasAfter) {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Đã hoàn thành '${task.title}' với SnapTask! Before / After minh chứng.")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ thành quả"))
                            } else {
                                onOpenAfterCamera(task)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ctaText,
                        color = colors.onFlash,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
