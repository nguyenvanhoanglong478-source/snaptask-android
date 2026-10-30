package com.snaptask.app.ui.screens.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.data.local.Converters
import com.snaptask.app.domain.model.SubTask
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.screens.editor.ReminderPickerDialog
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private class SplitLeftClipShape(private val fraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width * fraction, 0f)
            lineTo(size.width * fraction, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

private class SplitRightClipShape(private val fraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(size.width * fraction, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width * fraction, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

private fun formatDueDate(reminderTime: Long?): String {
    if (reminderTime == null) return "Hôm nay"
    val cal = Calendar.getInstance()
    val curYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = reminderTime
    val remYear = cal.get(Calendar.YEAR)

    val pattern = if (curYear == remYear) "dd/MM · HH:mm" else "dd/MM/yyyy · HH:mm"
    val formatted = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(reminderTime))
    return "Hạn $formatted"
}

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

    val beforeFile = remember(task.beforeImageUri) { File(task.beforeImageUri) }
    val hasBefore = beforeFile.exists()
    val afterFile = remember(task.afterImageUri) {
        if (!task.afterImageUri.isNullOrBlank()) File(task.afterImageUri) else null
    }
    val hasAfter = afterFile != null && afterFile.exists()

    fun shareTaskDetails() {
        val textBuilder = StringBuilder()
        textBuilder.append("SnapTask: ${task.title}\n")
        if (task.folderName.isNotBlank()) textBuilder.append("Thư mục: ${task.folderName}\n")
        if (isCompleted && task.completedAt != null) {
            val startStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.createdAt))
            val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.completedAt))
            val durationMin = maxOf(1, ((task.completedAt - task.createdAt) / 60000).toInt())
            textBuilder.append("Thời gian: $startStr - $endStr ($durationMin phút)\n")
        }
        if (currentCompletionCaption.isNotBlank()) {
            textBuilder.append("Ghi chú: “$currentCompletionCaption”\n")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ Before / After"))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(bottom = 110.dp) // Cushion so bottom docked bar doesn't overlap
        ) {
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
                            text = { Text("Chia sẻ", color = colors.text) },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = colors.textSecondary) },
                            onClick = {
                                showMenu = false
                                shareTaskDetails()
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

            // Body container
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                // 2. Title & Timestamp Meta
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
                    "${formatDueDate(task.reminderTime)} · ${task.folderName}"
                } else "Hôm nay · ${task.folderName}"

                Text(
                    text = timeMeta,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Before / After Interactive Split Photo Comparison View (Page 6)
                // C1: Khung Before/After: aspectRatio(350f / 300f), bo 24.
                if (hasBefore && hasAfter) {
                    var splitFraction by remember { mutableFloatStateOf(0.5f) }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(350f / 300f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                            .background(colors.surface)
                    ) {
                        val totalWidth = maxWidth
                        val widthPx = constraints.maxWidth.toFloat()
                        val splitOffset = totalWidth * splitFraction

                        // Left Image: Before Photo clipped to splitFraction
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(SplitLeftClipShape(splitFraction))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(beforeFile).crossfade(true).build(),
                                contentDescription = "Trước",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Right Image: After Photo clipped from splitFraction to end
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(SplitRightClipShape(splitFraction))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(afterFile).crossfade(true).build(),
                                contentDescription = "Sau",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // C2 & C3: Vạch chia và tay nắm vẽ TRƯỚC hai chip
                        // Vạch chia rộng 3dp màu #F5F1E8
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = splitOffset - 1.5.dp)
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFF5F1E8))
                        )

                        // Tay nắm: hình tròn đặc 36dp màu #F5F1E8, KHÔNG viền đen, bên trong hai chevron "<" ">" 20dp màu #17140A
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = splitOffset - 18.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5F1E8))
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        splitFraction = (splitFraction + dragAmount / widthPx).coerceIn(0.06f, 0.94f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Hai chevron "<" ">" 20dp vẽ bằng Canvas vector
                            Canvas(modifier = Modifier.size(20.dp)) {
                                val stroke = 2.2.dp.toPx()
                                val chevColor = Color(0xFF17140A)

                                // Left chevron <
                                val pathLeft = Path().apply {
                                    moveTo(size.width * 0.42f, size.height * 0.22f)
                                    lineTo(size.width * 0.18f, size.height * 0.50f)
                                    lineTo(size.width * 0.42f, size.height * 0.78f)
                                }
                                drawPath(pathLeft, chevColor, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

                                // Right chevron >
                                val pathRight = Path().apply {
                                    moveTo(size.width * 0.58f, size.height * 0.22f)
                                    lineTo(size.width * 0.82f, size.height * 0.50f)
                                    lineTo(size.width * 0.58f, size.height * 0.78f)
                                }
                                drawPath(pathRight, chevColor, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }

                        // C2 & C4: Hai chip "TRƯỚC" và "SAU" vẽ SAU CÙNG để luôn nằm trên vạch chia
                        // Chip "TRƯỚC": nền rgba(18,17,16,0.72), chữ #F5F1E8 12sp SemiBold, cao 24, bo 8, cách mép 12
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xB8121110))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TRƯỚC",
                                color = Color(0xFFF5F1E8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Chip "SAU": nền flash, chữ onFlash 12sp Bold, cao 24, bo 8, cách mép 12
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.flash)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SAU",
                                color = colors.onFlash,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (hasBefore) {
                    // Only Before Photo Present -> Prompt for After photo
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(350f / 300f)
                            .clickable { onOpenAfterCamera(task) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
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
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xB8121110))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "TRƯỚC",
                                    color = Color(0xFFF5F1E8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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

                // C5. Thẻ chú thích: bỏ FontStyle.Italic (chữ thẳng 15/22 Regular), giữ dấu nháy kép
                val rawQuote = currentCompletionCaption.ifBlank {
                    if (isCompleted) "Xong rồi, giờ có chỗ đặt laptop thoải mái." else task.description
                }
                if (rawQuote.isNotBlank()) {
                    val formattedQuote = if (rawQuote.startsWith("“") || rawQuote.startsWith("\"")) {
                        rawQuote
                    } else "“$rawQuote”"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.elevated)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = formattedQuote,
                            color = colors.text,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // C6. Hàng meta: chip cao 28, bo 8, padding ngang 12, chữ 12sp SemiBold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Pill: "Vừa" = viền 1dp màu warning + chữ warning, nền trong suốt; "Cao" = nền urgent alpha 0.16 chữ urgent
                    when (task.priorityEnum) {
                        TaskPriority.HIGH -> {
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.urgent.copy(alpha = 0.16f))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = task.priorityEnum.label,
                                    color = colors.urgent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        TaskPriority.MEDIUM -> {
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Transparent)
                                    .border(1.dp, colors.warning, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = task.priorityEnum.label,
                                    color = colors.warning,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        TaskPriority.LOW -> {
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.elevated)
                                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = task.priorityEnum.label,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Folder Pill: cao 28, bo 8, padding ngang 12
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.elevated)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = task.folderName,
                            color = colors.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Due Date Pill: "Hạn dd/MM · HH:mm" (bỏ năm nếu là năm hiện tại)
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.elevated)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatDueDate(task.reminderTime),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Checklist danh sách công việc con
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
            }
        }

        // C7. Thanh hai nút neo đáy với dải mờ cao 120dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Dải mờ 120dp từ trong suốt sang colors.bg
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colors.bg)
                        )
                    )
            )

            // Thanh hai nút: padding ngang 20, đáy 24, khoảng cách 10
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Chỉnh sửa": nền TRONG SUỐT + viền 1dp border, weight 1, cao 52, bo 16
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
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

                // "Chia sẻ Before / After": nền flash, chữ onFlash 15sp Bold, weight 1.4, cao 52, bo 16
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.flash)
                        .clickable {
                            shareTaskDetails()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chia sẻ Before / After",
                        color = colors.onFlash,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showReminderPicker) {
        ReminderPickerDialog(
            initialReminderTime = reminderTime,
            initialReminderType = reminderType,
            onDismiss = { showReminderPicker = false },
            onConfirm = { time, type ->
                reminderTime = time
                reminderType = type
                showReminderPicker = false
                onSaveTask(task.copy(reminderTime = time, reminderType = type))
            }
        )
    }
}

@ComposePreview(widthDp = 390, heightDp = 844)
@Composable
fun PreviewTaskDetailScreen() {
    SnapTaskTheme(darkTheme = true) {
        TaskDetailScreen(
            task = Task(
                id = 1,
                title = "Dọn bàn làm việc",
                description = "Xong rồi, giờ có chỗ đặt laptop thoải mái.",
                completionCaption = "Xong rồi, giờ có chỗ đặt laptop thoải mái.",
                beforeImageUri = "",
                afterImageUri = "",
                isCompleted = true,
                folderName = "Nhà",
                priority = "MEDIUM",
                createdAt = System.currentTimeMillis() - 35 * 60 * 1000,
                completedAt = System.currentTimeMillis(),
                reminderTime = System.currentTimeMillis() + 3600 * 1000
            ),
            onBack = {},
            onSaveTask = {},
            onDeleteTask = {},
            onOpenAfterCamera = {}
        )
    }
}
