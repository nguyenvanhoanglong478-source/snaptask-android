package com.snaptask.app.ui.screens.detail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.data.local.Converters
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.domain.model.SubTask
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.components.RoundIconButton
import com.snaptask.app.ui.components.SnapCheckbox
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.components.SnapSegmentedControl
import com.snaptask.app.ui.screens.editor.ReminderPickerDialog
import com.snaptask.app.ui.theme.SnapTaskIcons
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

private val ExpandPhotoIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ExpandPhoto",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(15f, 4f); lineTo(20f, 4f); lineTo(20f, 9f)
        moveTo(9f, 20f); lineTo(4f, 20f); lineTo(4f, 15f)
        moveTo(20f, 4f); lineTo(14f, 10f)
        moveTo(4f, 20f); lineTo(10f, 14f)
    }.build()
}

@Composable
fun TaskDetailScreen(
    task: Task,
    folders: List<CustomFolder>,
    onBack: () -> Unit,
    onSaveTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onOpenAfterCamera: (Task) -> Unit,
    onOpenFolders: () -> Unit,
    onOpenShareCard: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val converters = remember { Converters() }
    val scrollState = rememberScrollState()

    val originalSubtasks = remember(task.id, task.subTasksJson) { task.parsedSubtasks }

    var currentTitle by remember(task.id, task.title) { mutableStateOf(task.title) }
    var currentDescription by remember(task.id, task.description) { mutableStateOf(task.description) }
    var currentCompletionCaption by remember(task.id, task.completionCaption) { mutableStateOf(task.completionCaption ?: "") }
    var currentPriority by remember(task.id, task.priority) { mutableStateOf(task.priorityEnum) }
    var currentFolder by remember(task.id, task.folderName) { mutableStateOf(task.folderName) }
    var currentDueDate by remember(task.id, task.dueDate) { mutableStateOf(task.dueDate) }
    var isCompleted by remember(task.id, task.isCompleted) { mutableStateOf(task.isCompleted) }
    var isPinned by remember(task.id, task.isPinnedToWidget) { mutableStateOf(task.isPinnedToWidget) }
    var reminderTime by remember(task.id, task.reminderTime) { mutableStateOf(task.reminderTime) }
    var reminderType by remember(task.id, task.reminderType) { mutableStateOf(task.reminderType) }
    var subtasks by remember(task.id, task.subTasksJson) { mutableStateOf(originalSubtasks) }

    var newSubtaskText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var fullscreenShowingAfter by remember { mutableStateOf<Boolean?>(null) }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var compareMode by rememberSaveable { mutableIntStateOf(0) }
    var isCardFlipped by rememberSaveable { mutableStateOf(false) }
    var splitFraction by rememberSaveable { mutableFloatStateOf(0.52f) }

    val beforeFile = remember(task.beforeImageUri) {
        if (task.beforeImageUri.isNotBlank()) File(task.beforeImageUri) else null
    }
    val afterFile = remember(task.afterImageUri) {
        if (!task.afterImageUri.isNullOrBlank()) File(task.afterImageUri) else null
    }
    val hasBefore = beforeFile != null && beforeFile.exists()
    val hasAfter = afterFile != null && afterFile.exists()

    val matchedFolder = remember(folders, currentFolder) {
        folders.firstOrNull { it.name.equals(currentFolder, ignoreCase = true) }
    }

    val hasUnsavedChanges = remember(
        currentTitle,
        currentDescription,
        currentCompletionCaption,
        currentPriority,
        currentFolder,
        currentDueDate,
        reminderTime,
        reminderType,
        subtasks,
        originalSubtasks,
        task
    ) {
        currentTitle.trim() != task.title.trim() ||
            currentDescription.trim() != task.description.trim() ||
            currentCompletionCaption.trim() != (task.completionCaption?.trim() ?: "") ||
            currentPriority != task.priorityEnum ||
            currentFolder != task.folderName ||
            currentDueDate != task.dueDate ||
            reminderTime != task.reminderTime ||
            reminderType != task.reminderType ||
            subtasks != originalSubtasks
    }

    fun buildSnapshot(
        completed: Boolean = isCompleted,
        completedAtOverride: Long? = if (completed) (task.completedAt ?: System.currentTimeMillis()) else null,
        pinned: Boolean = isPinned,
        priority: TaskPriority = currentPriority,
        folder: String = currentFolder,
        dueDate: Long? = currentDueDate,
        remTime: Long? = reminderTime,
        remType: String = reminderType,
        subs: List<SubTask> = subtasks
    ): Task {
        return task.copy(
            title = currentTitle.trim().ifBlank { task.title },
            description = currentDescription.trim(),
            completionCaption = currentCompletionCaption.trim().ifBlank { null },
            isCompleted = completed,
            completedAt = completedAtOverride,
            isPinnedToWidget = pinned,
            priority = priority.name,
            folderName = folder,
            dueDate = dueDate,
            reminderTime = remTime,
            reminderType = remType,
            subTasksJson = converters.fromSubTaskList(subs)
        )
    }

    fun handleBackPress() {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    fun openDueDatePicker() {
        val baseCal = Calendar.getInstance().apply {
            currentDueDate?.let { timeInMillis = it } ?: run {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val picked = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        currentDueDate = picked
                        if (!isEditing) {
                            onSaveTask(buildSnapshot(dueDate = picked))
                        }
                    },
                    baseCal.get(Calendar.HOUR_OF_DAY),
                    baseCal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            baseCal.get(Calendar.YEAR),
            baseCal.get(Calendar.MONTH),
            baseCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun addSubtask() {
        val trimmed = newSubtaskText.trim()
        if (trimmed.isEmpty()) return
        val updated = subtasks + SubTask(
            id = UUID.randomUUID().toString(),
            title = trimmed,
            isDone = false
        )
        subtasks = updated
        newSubtaskText = ""
        if (!isEditing) {
            onSaveTask(buildSnapshot(subs = updated))
        }
    }

    BackHandler(enabled = hasUnsavedChanges && !showUnsavedDialog && fullscreenShowingAfter == null) {
        showUnsavedDialog = true
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateTimeShortFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    val subtitleText = remember(isCompleted, task.createdAt, task.completedAt, reminderTime, currentFolder) {
        val createdStr = timeFormat.format(Date(task.createdAt))
        when {
            isCompleted && task.completedAt != null -> {
                val doneStr = timeFormat.format(Date(task.completedAt))
                "Chụp $createdStr · Xong $doneStr · $currentFolder"
            }
            reminderTime != null -> {
                "Nhắc ${dateTimeShortFormat.format(Date(reminderTime!!))} · $currentFolder"
            }
            else -> "Tạo $createdStr · $currentFolder"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .imePadding()
                .padding(bottom = 112.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(
                    icon = SnapTaskIcons.ChevronLeft,
                    contentDescription = "Quay lại",
                    onClick = { handleBackPress() }
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        val mins = SnapDateHelper.durationMinutes(task)
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(colors.flash)
                                .clickable(role = Role.Button) {
                                    isCompleted = false
                                    onSaveTask(buildSnapshot(completed = false, completedAtOverride = null))
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = SnapTaskIcons.Check,
                                    contentDescription = null,
                                    tint = colors.onFlash,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Xong sau $mins phút",
                                    color = colors.onFlash,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(colors.elevated)
                                .border(1.dp, colors.border, CircleShape)
                                .clickable(role = Role.Button) {
                                    isCompleted = true
                                    onSaveTask(
                                        buildSnapshot(
                                            completed = true,
                                            completedAtOverride = System.currentTimeMillis()
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Đánh dấu xong",
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box {
                    RoundIconButton(
                        icon = SnapTaskIcons.MoreHorizontal,
                        contentDescription = "Tùy chọn",
                        onClick = { showMenu = true }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isPinned) "Bỏ ghim widget" else "Ghim ra widget",
                                    color = colors.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = SnapTaskIcons.Pin,
                                    contentDescription = null,
                                    tint = colors.text,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                val nextPinned = !isPinned
                                isPinned = nextPinned
                                showMenu = false
                                onSaveTask(buildSnapshot(pinned = nextPinned))
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Đặt giờ nhắc",
                                    color = colors.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = SnapTaskIcons.AlarmClock,
                                    contentDescription = null,
                                    tint = colors.text,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                showReminderPicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (hasAfter) "Chụp lại ảnh SAU" else "Chụp ảnh kết quả (SAU)",
                                    color = colors.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = SnapTaskIcons.Retake,
                                    contentDescription = null,
                                    tint = colors.text,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onOpenAfterCamera(task)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Mở thẻ chia sẻ",
                                    color = colors.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = SnapTaskIcons.ShareUp,
                                    contentDescription = null,
                                    tint = colors.text,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onOpenShareCard(buildSnapshot())
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Xóa việc",
                                    color = colors.urgent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = SnapTaskIcons.Trash,
                                    contentDescription = null,
                                    tint = colors.urgent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteTask(task)
                            }
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = currentTitle,
                        onValueChange = { currentTitle = it },
                        placeholder = {
                            Text("Tiêu đề công việc", color = colors.textMuted, fontSize = 16.sp)
                        },
                        textStyle = TextStyle(
                            color = colors.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text,
                            focusedContainerColor = colors.elevated,
                            unfocusedContainerColor = colors.elevated,
                            focusedBorderColor = colors.text,
                            unfocusedBorderColor = colors.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = currentTitle,
                        color = colors.text,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitleText,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (hasBefore && hasAfter && beforeFile != null && afterFile != null) {
                    BeforeAfterComparisonBox(
                        beforeFile = beforeFile,
                        afterFile = afterFile,
                        compareMode = compareMode,
                        splitFraction = splitFraction,
                        onSplitChange = { splitFraction = it },
                        isCardFlipped = isCardFlipped,
                        onToggleFlip = { isCardFlipped = !isCardFlipped },
                        onOpenFullscreen = { showAfter -> fullscreenShowingAfter = showAfter }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SnapSegmentedControl(
                        options = listOf("Kéo so sánh", "Lật thẻ", "Cạnh nhau"),
                        selectedIndex = compareMode,
                        onSelect = { compareMode = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                } else if (hasBefore && beforeFile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(36.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(36.dp))
                            .clickable { fullscreenShowingAfter = false }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(beforeFile).crossfade(true).build(),
                            contentDescription = "Ảnh trước",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.55f to Color.Transparent,
                                        1.0f to Color(0xCC0A0A09)
                                    )
                                )
                        )
                        CompareBadgeTag(
                            text = "TRƯỚC",
                            isAfter = false,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                        )
                        ExpandPhotoButton(
                            onClick = { fullscreenShowingAfter = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(colors.flash)
                                .clickable(role = Role.Button) { onOpenAfterCamera(task) }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chụp ảnh kết quả (SAU)",
                                color = colors.onFlash,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                } else if (hasAfter && afterFile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(36.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(36.dp))
                            .clickable { fullscreenShowingAfter = true }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(afterFile).crossfade(true).build(),
                            contentDescription = "Ảnh sau",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        CompareBadgeTag(
                            text = "SAU",
                            isAfter = true,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                        )
                        ExpandPhotoButton(
                            onClick = { fullscreenShowingAfter = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = if (isCompleted) currentCompletionCaption else currentDescription,
                        onValueChange = {
                            if (isCompleted) currentCompletionCaption = it else currentDescription = it
                        },
                        placeholder = {
                            Text(
                                text = if (isCompleted) "Ghi chú sau khi hoàn thành…" else "Ghi chú chi tiết…",
                                color = colors.textMuted,
                                fontSize = 14.sp
                            )
                        },
                        textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text,
                            focusedContainerColor = colors.elevated,
                            unfocusedContainerColor = colors.elevated,
                            focusedBorderColor = colors.text,
                            unfocusedBorderColor = colors.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    val userNote = currentCompletionCaption.ifBlank { currentDescription }
                    if (userNote.isNotBlank()) {
                        val displayNote = if (currentCompletionCaption.isNotBlank() &&
                            !userNote.startsWith("“") && !userNote.startsWith("\"")
                        ) {
                            "“$userNote”"
                        } else {
                            userNote
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = displayNote,
                                color = colors.text,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isUrgentPriority = currentPriority == TaskPriority.HIGH
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(if (isUrgentPriority) colors.urgent else colors.elevated)
                            .border(
                                width = 1.dp,
                                color = if (isUrgentPriority) colors.urgent else colors.border,
                                shape = CircleShape
                            )
                            .clickable(role = Role.Button) {
                                val next = when (currentPriority) {
                                    TaskPriority.LOW -> TaskPriority.MEDIUM
                                    TaskPriority.MEDIUM -> TaskPriority.HIGH
                                    TaskPriority.HIGH -> TaskPriority.LOW
                                }
                                currentPriority = next
                                if (!isEditing) {
                                    onSaveTask(buildSnapshot(priority = next))
                                }
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.FlagPriority,
                                contentDescription = null,
                                tint = if (isUrgentPriority) colors.onUrgent else colors.text,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentPriority.label,
                                color = if (isUrgentPriority) colors.onUrgent else colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val remShort = remember(reminderTime) {
                        reminderTime?.let { timeFormat.format(Date(it)) } ?: "Nhắc giờ"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(if (reminderTime != null) colors.flash else colors.elevated)
                            .border(
                                width = 1.dp,
                                color = if (reminderTime != null) colors.flash else colors.border,
                                shape = CircleShape
                            )
                            .clickable(role = Role.Button) { showReminderPicker = true }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Clock,
                                contentDescription = null,
                                tint = if (reminderTime != null) colors.onFlash else colors.text,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = remShort,
                                color = if (reminderTime != null) colors.onFlash else colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(colors.elevated)
                            .border(1.dp, colors.border, CircleShape)
                            .clickable(role = Role.Button) {
                                if (isEditing && folders.isNotEmpty()) {
                                    val curIdx = folders.indexOfFirst { it.name.equals(currentFolder, ignoreCase = true) }
                                    val nextFolder = folders[(curIdx + 1) % folders.size].name
                                    currentFolder = nextFolder
                                } else {
                                    onOpenFolders()
                                }
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(matchedFolder?.colorHex ?: 0xFFD4FF3A))
                            )
                            Text(
                                text = currentFolder,
                                color = colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val nowMillis = System.currentTimeMillis()
                val isTaskOverdue = DateTimeUtils.isOverdue(currentDueDate, isCompleted, nowMillis)
                val dueLabel = currentDueDate?.let { DateTimeUtils.formatDueLabel(it, nowMillis) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(if (isTaskOverdue) colors.urgentSoft else colors.elevated)
                            .border(
                                width = 1.dp,
                                color = when {
                                    isTaskOverdue -> colors.urgent
                                    currentDueDate != null -> colors.borderHighlight
                                    else -> colors.border
                                },
                                shape = CircleShape
                            )
                            .clickable(role = Role.Button) { openDueDatePicker() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Calendar,
                                contentDescription = null,
                                tint = when {
                                    isTaskOverdue -> colors.urgent
                                    currentDueDate != null -> colors.done
                                    else -> colors.textSecondary
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = when {
                                    isTaskOverdue && dueLabel != null -> "Quá hạn · $dueLabel"
                                    dueLabel != null -> "Hạn chót · $dueLabel"
                                    else -> "Đặt hạn chót"
                                },
                                color = when {
                                    isTaskOverdue -> colors.urgent
                                    currentDueDate != null -> colors.text
                                    else -> colors.textSecondary
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isTaskOverdue) FontWeight.ExtraBold else FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (currentDueDate != null) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.elevated)
                                .border(1.dp, colors.border, CircleShape)
                                .clickable(role = Role.Button) {
                                    currentDueDate = null
                                    if (!isEditing) {
                                        onSaveTask(buildSnapshot(dueDate = null))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Close,
                                contentDescription = "Xóa hạn chót",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "CÁC BƯỚC NHỎ · ${subtasks.count { it.isDone }}/${subtasks.size}",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    if (subtasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        subtasks.forEachIndexed { idx, st ->
                            if (idx > 0) {
                                HorizontalDivider(color = colors.border, thickness = 1.dp)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updated = subtasks.toMutableList()
                                        updated[idx] = st.copy(isDone = !st.isDone)
                                        subtasks = updated
                                        if (!isEditing) {
                                            onSaveTask(buildSnapshot(subs = updated))
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SnapCheckbox(checked = st.isDone)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = st.title,
                                    color = if (st.isDone) colors.textMuted else colors.text,
                                    textDecoration = if (st.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isEditing) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable(role = Role.Button) {
                                                val updated = subtasks.toMutableList().also { it.removeAt(idx) }
                                                subtasks = updated
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = SnapTaskIcons.Close,
                                            contentDescription = "Xóa bước nhỏ",
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = {
                                Text("Thêm bước nhỏ…", color = colors.textMuted, fontSize = 13.sp)
                            },
                            textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    addSubtask()
                                    keyboardController?.hide()
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text,
                                focusedContainerColor = colors.elevated,
                                unfocusedContainerColor = colors.elevated,
                                focusedBorderColor = colors.text,
                                unfocusedBorderColor = colors.border
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.flash)
                                .clickable(role = Role.Button) {
                                    addSubtask()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Plus,
                                contentDescription = "Thêm bước nhỏ",
                                tint = colors.onFlash,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.bg)
                .border(1.dp, colors.border)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(colors.elevated)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(role = Role.Button) {
                        if (isEditing) {
                            isEditing = false
                            onSaveTask(buildSnapshot())
                        } else {
                            isEditing = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) SnapTaskIcons.Check else SnapTaskIcons.EditPen,
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (isEditing) "Lưu" else "Sửa",
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val showCaptureAfterPrimary = !isCompleted && hasBefore && !hasAfter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(colors.flash)
                    .clickable(role = Role.Button) {
                        if (showCaptureAfterPrimary) {
                            onOpenAfterCamera(task)
                        } else {
                            onOpenShareCard(buildSnapshot())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!showCaptureAfterPrimary) {
                        Icon(
                            imageVector = SnapTaskIcons.ShareUp,
                            contentDescription = null,
                            tint = colors.onFlash,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (showCaptureAfterPrimary) "Chụp ảnh kết quả (SAU)" else "Chia sẻ Trước / Sau",
                        color = colors.onFlash,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    if (showReminderPicker) {
        ReminderPickerDialog(
            initialReminderTime = reminderTime,
            initialReminderType = reminderType,
            onConfirm = { newTime, newType ->
                reminderTime = newTime
                reminderType = newType
                showReminderPicker = false
                if (!isEditing) {
                    onSaveTask(buildSnapshot(remTime = newTime, remType = newType))
                }
            },
            onDismiss = { showReminderPicker = false }
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onSave = {
                showUnsavedDialog = false
                isEditing = false
                onSaveTask(buildSnapshot())
                onBack()
            },
            onDiscard = {
                showUnsavedDialog = false
                isEditing = false
                onBack()
            },
            onCancel = {
                showUnsavedDialog = false
            }
        )
    }

    fullscreenShowingAfter?.let { initialAfter ->
        FullscreenPhotoDialog(
            beforeFile = if (hasBefore) beforeFile else null,
            afterFile = if (hasAfter) afterFile else null,
            initialShowingAfter = initialAfter,
            completionCaption = currentCompletionCaption.trim(),
            onDismiss = { fullscreenShowingAfter = null }
        )
    }
}

@Composable
private fun BeforeAfterComparisonBox(
    beforeFile: File,
    afterFile: File,
    compareMode: Int,
    splitFraction: Float,
    onSplitChange: (Float) -> Unit,
    isCardFlipped: Boolean,
    onToggleFlip: () -> Unit,
    onOpenFullscreen: (showingAfter: Boolean) -> Unit
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val shape = RoundedCornerShape(36.dp)

    when (compareMode) {
        0 -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shape)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            onSplitChange((splitFraction + dragAmount / w).coerceIn(0.08f, 0.92f))
                        }
                    }
            ) {
                val splitOffset = maxWidth * splitFraction

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(SplitLeftClipShape(splitFraction))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(beforeFile).crossfade(true).build(),
                        contentDescription = "Ảnh trước",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(SplitRightClipShape(splitFraction))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(afterFile).crossfade(true).build(),
                        contentDescription = "Ảnh sau",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = splitOffset - 1.5.dp)
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFFAFAF6))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = splitOffset - 22.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFAFAF6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.CompareArrows,
                        contentDescription = "Kéo để so sánh",
                        tint = Color(0xFF10120A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                CompareBadgeTag(
                    text = "TRƯỚC",
                    isAfter = false,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                )

                CompareBadgeTag(
                    text = "SAU",
                    isAfter = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                )

                ExpandPhotoButton(
                    onClick = { onOpenFullscreen(splitFraction < 0.5f) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                )
            }
        }

        1 -> {
            val rotation by animateFloatAsState(
                targetValue = if (isCardFlipped) 180f else 0f,
                animationSpec = tween(durationMillis = 420),
                label = "BeforeAfterFlip"
            )
            val showingAfter = rotation > 90f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 14f * density
                    }
                    .clip(shape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shape)
                    .clickable(role = Role.Button, onClick = onToggleFlip)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (showingAfter) rotationY = 180f
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(if (showingAfter) afterFile else beforeFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = if (showingAfter) "Ảnh sau" else "Ảnh trước",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    CompareBadgeTag(
                        text = if (showingAfter) "SAU" else "TRƯỚC",
                        isAfter = showingAfter,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                    )

                    ExpandPhotoButton(
                        onClick = { onOpenFullscreen(showingAfter) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(colors.bubble)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showingAfter) "Chạm để xem ảnh TRƯỚC" else "Chạm để lật sang ảnh SAU",
                            color = Color(0xFFFAFAF6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shape)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(beforeFile).crossfade(true).build(),
                        contentDescription = "Ảnh trước",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onOpenFullscreen(false) },
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
                        contentDescription = "Ảnh sau",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onOpenFullscreen(true) },
                        contentScale = ContentScale.Crop
                    )
                }

                CompareBadgeTag(
                    text = "TRƯỚC",
                    isAfter = false,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                )

                CompareBadgeTag(
                    text = "SAU",
                    isAfter = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                )

                ExpandPhotoButton(
                    onClick = { onOpenFullscreen(true) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandPhotoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(colors.bubble)
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ExpandPhotoIcon,
            contentDescription = "Xem ảnh toàn màn hình",
            tint = Color(0xFFFAFAF6),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.snapColors
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Lưu thay đổi?",
                color = colors.text,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bạn có chỉnh sửa chưa lưu. Bạn muốn lưu trước khi quay lại không?",
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(colors.flash)
                    .clickable(role = Role.Button, onClick = onSave),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lưu",
                    color = colors.onFlash,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(colors.urgentSoft)
                        .border(1.dp, colors.urgent, CircleShape)
                        .clickable(role = Role.Button, onClick = onDiscard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bỏ thay đổi",
                        color = colors.urgent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(colors.elevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(role = Role.Button, onClick = onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hủy",
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenPhotoDialog(
    beforeFile: File?,
    afterFile: File?,
    initialShowingAfter: Boolean,
    completionCaption: String,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val canSwitch = beforeFile != null && afterFile != null
    var showingAfter by remember(initialShowingAfter, beforeFile, afterFile) {
        mutableStateOf(
            when {
                initialShowingAfter && afterFile != null -> true
                beforeFile != null -> false
                else -> afterFile != null
            }
        )
    }

    val activeFile = if (showingAfter && afterFile != null) afterFile else beforeFile
    var scale by remember(showingAfter) { mutableFloatStateOf(1f) }
    var offset by remember(showingAfter) { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A09))
        ) {
            if (activeFile != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(activeFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = if (showingAfter) "Ảnh kết quả Sau toàn màn hình" else "Ảnh Trước toàn màn hình",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(showingAfter) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                        offset = Offset.Zero
                                    }
                                }
                            )
                        }
                        .pointerInput(showingAfter) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 4f)
                                val maxX = (size.width * (newScale - 1f)) / 2f
                                val maxY = (size.height * (newScale - 1f)) / 2f
                                scale = newScale
                                offset = if (newScale <= 1f) {
                                    Offset.Zero
                                } else {
                                    Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canSwitch) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xAD1A1A18))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(if (!showingAfter) colors.flash else Color.Transparent)
                                .clickable(role = Role.Tab) { showingAfter = false }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TRƯỚC",
                                color = if (!showingAfter) colors.onFlash else Color(0xFFFAFAF6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(if (showingAfter) colors.flash else Color.Transparent)
                                .clickable(role = Role.Tab) { showingAfter = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SAU",
                                color = if (showingAfter) colors.onFlash else Color(0xFFFAFAF6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    CompareBadgeTag(
                        text = if (showingAfter) "SAU" else "TRƯỚC",
                        isAfter = showingAfter
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xAD1A1A18))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFFFAFAF6),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showingAfter && completionCaption.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xAD1A1A18))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "“$completionCaption”",
                            color = Color(0xFFFAFAF6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = "Chụm hoặc nhấn đúp để thu phóng",
                    color = Color(0x99FAFAF6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CompareBadgeTag(
    text: String,
    isAfter: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    Box(
        modifier = modifier
            .width(if (isAfter) 66.dp else 76.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(if (isAfter) colors.flash else colors.bubble),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isAfter) colors.onFlash else Color(0xFFFAFAF6),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
