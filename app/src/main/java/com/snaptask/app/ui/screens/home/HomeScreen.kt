package com.snaptask.app.ui.screens.home

import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.components.SnapTaskLogo
import com.snaptask.app.ui.screens.home.components.CompletionProofBottomSheet
import com.snaptask.app.ui.screens.home.components.FlipTaskCard3D
import com.snaptask.app.ui.screens.home.components.QuickNoteBottomSheet
import androidx.compose.foundation.isSystemInDarkTheme
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import java.io.File
import java.util.Calendar

enum class ViewMode {
    BENTO_GRID,
    FEED_FULL
}

@Composable
fun HomeScreen(
    tasks: List<Task>,
    onOpenLiveCamera: () -> Unit,
    onOpenProofCamera: (Task) -> Unit,
    onQuickComplete: (Task) -> Unit,
    onToggleTaskStatus: (Task) -> Unit,
    onTogglePin: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onSaveQuickNote: (Task) -> Unit = {},
    onOpenBoard: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    var viewMode by remember { mutableStateOf(ViewMode.BENTO_GRID) }
    var selectedCustomFolder by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var pendingCompleteTask by remember { mutableStateOf<Task?>(null) }
    var flippedMap by remember { mutableStateOf(mapOf<Long, Boolean>()) }

    var showQuickNoteSheet by remember { mutableStateOf(false) }

    // Dynamic Vietnamese uppercase date string: e.g. "THỨ NĂM · 29 THÁNG 10"
    val todayDateString = remember {
        val calendar = Calendar.getInstance()
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "THỨ HAI"
            Calendar.TUESDAY -> "THỨ BA"
            Calendar.WEDNESDAY -> "THỨ TƯ"
            Calendar.THURSDAY -> "THỨ NĂM"
            Calendar.FRIDAY -> "THỨ SÁU"
            Calendar.SATURDAY -> "THỨ BẢY"
            Calendar.SUNDAY -> "CHỦ NHẬT"
            else -> "HÔM NAY"
        }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        "$dayOfWeek · $day THÁNG $month"
    }

    val customFoldersList = remember { listOf("Công việc", "Nhà", "Cá nhân") }
    val allFolders = remember(tasks) {
        val taskFolders = tasks.map { it.folderName }.filter { it.isNotBlank() && it !in customFoldersList }
        (customFoldersList + taskFolders).distinct()
    }

    val handleCheckboxClick: (Task) -> Unit = { task ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (task.isCompleted) {
            onToggleTaskStatus(task)
        } else {
            pendingCompleteTask = task
        }
    }

    // Filter pipeline by folder and search
    val processedTasks = remember(tasks, selectedCustomFolder, searchQuery) {
        var list = tasks

        // 1. Filter by Folder
        if (selectedCustomFolder != null) {
            list = list.filter { it.folderName.equals(selectedCustomFolder, ignoreCase = true) }
        }

        // 2. Filter by Search Query
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.folderName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sort: Pinned first, then incomplete before completed, then newest
        list.sortedWith(
            compareByDescending<Task> { it.isPinnedToWidget }
                .thenBy { it.isCompleted }
                .thenByDescending { it.createdAt }
        )
    }

    val uncompletedCount = tasks.count { !it.isCompleted }
    val completedTasks = tasks.count { it.isCompleted }
    val totalTasks = tasks.size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 4.dp)
            ) {
                // 1. Top Row: Brand Header on Left, Search Button on Right (Page 3 & 4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedCustomFolder = null }
                    ) {
                        SnapTaskLogo(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SnapTask",
                            color = colors.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Light / Dark Mode Toggle Button (Page 3 vs Page 4)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.dp, colors.border, CircleShape)
                                .clickable(onClick = onToggleTheme),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = if (isDarkTheme) "Chuyển giao diện sáng" else "Chuyển giao diện tối",
                                tint = if (isDarkTheme) colors.flash else colors.text,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Circular Search Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.dp, colors.border, CircleShape)
                                .clickable { isSearchActive = !isSearchActive },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Search,
                                contentDescription = "Tìm kiếm",
                                tint = if (isSearchActive) colors.flash else colors.text,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Subtitle: Date in uppercase (THỨ NĂM · 29 THÁNG 10)
                Text(
                    text = todayDateString,
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                // 3. Main Headline: "Hôm nay còn X việc" (Bricolage 800)
                Text(
                    text = if (uncompletedCount > 0) "Hôm nay còn $uncompletedCount việc" else "Hôm nay đã xong việc",
                    color = colors.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Segmented Progress Bar: 7-segment rounded dashes + "3/7 xong" (Page 3 & 4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalSegments = if (totalTasks == 0) 4 else minOf(totalTasks, 8)
                    val filledSegments = if (totalTasks == 0) 0 else ((completedTasks.toFloat() / totalTasks) * totalSegments).toInt().coerceIn(0, totalSegments)

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until totalSegments) {
                            val isFilled = i < filledSegments
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isFilled) colors.flash else colors.elevated)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "$completedTasks/$totalTasks xong",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Folder Filter Chips Row: "Tất cả", "Công việc", "Nhà", "Cá nhân" (Page 3 & 4)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val isSelected = selectedCustomFolder == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) colors.flash else colors.elevated)
                                .border(1.dp, if (isSelected) colors.flash else colors.border, RoundedCornerShape(20.dp))
                                .clickable { selectedCustomFolder = null }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Tất cả",
                                color = if (isSelected) colors.onFlash else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                    items(allFolders) { folder ->
                        val isSelected = selectedCustomFolder.equals(folder, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) colors.flash else colors.elevated)
                                .border(1.dp, if (isSelected) colors.flash else colors.border, RoundedCornerShape(20.dp))
                                .clickable {
                                    selectedCustomFolder = if (isSelected) null else folder
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = folder,
                                color = if (isSelected) colors.onFlash else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Input Box
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm theo tên, ghi chú, thư mục...", color = colors.textMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.flash,
                            unfocusedBorderColor = colors.border
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Xoá", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                }

                // Feed Content
                if (processedTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(text = "Chưa có nhiệm vụ nào", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bấm nút chụp bên dưới để tạo việc mới",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Crossfade(
                        targetState = viewMode,
                        label = "ViewModeCrossfade",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { mode ->
                        when (mode) {
                            ViewMode.BENTO_GRID -> {
                                val featuredTask = processedTasks.firstOrNull { it.isPinnedToWidget } ?: processedTasks.firstOrNull()
                                val bentoRemainingTasks = if (featuredTask != null) processedTasks.filter { it.id != featuredTask.id } else emptyList()

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 100.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 1. Featured Large Card (Spans full 2 columns) - Exactly as in Page 3 & 4
                                    if (featuredTask != null) {
                                        item(span = { GridItemSpan(2) }, key = featuredTask.id) {
                                            FeaturedTaskCard(
                                                task = featuredTask,
                                                onToggleStatus = { handleCheckboxClick(featuredTask) },
                                                onClick = { onTaskClick(featuredTask) }
                                            )
                                        }
                                    }

                                    // 2. 2-Column Bento Grid for Remaining Tasks
                                    items(bentoRemainingTasks, key = { it.id }) { task ->
                                        BentoTaskCard(
                                            task = task,
                                            onToggleStatus = { handleCheckboxClick(task) },
                                            onTogglePin = { onTogglePin(task) },
                                            onClick = { onTaskClick(task) }
                                        )
                                    }
                                }
                            }
                            ViewMode.FEED_FULL -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 100.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(processedTasks, key = { it.id }) { task ->
                                        val isFlipped = flippedMap[task.id] ?: false
                                        FlipTaskCard3D(
                                            task = task,
                                            isFlipped = isFlipped,
                                            onToggleFlip = {
                                                flippedMap = flippedMap + (task.id to !isFlipped)
                                            },
                                            onToggleStatus = { handleCheckboxClick(task) },
                                            onTogglePin = { onTogglePin(task) },
                                            onClick = { onTaskClick(task) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING GLASS DOCK (Centered, 3 Symmetrical Controls as in Page 3 & 4)
            FloatingGlassDock(
                onOpenLiveCamera = onOpenLiveCamera,
                onToggleViewMode = {
                    viewMode = if (viewMode == ViewMode.BENTO_GRID) ViewMode.FEED_FULL else ViewMode.BENTO_GRID
                },
                onOpenBoard = onOpenBoard,
                viewMode = viewMode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }

        // Completion Proof Bottom Sheet
        pendingCompleteTask?.let { task ->
            CompletionProofBottomSheet(
                task = task,
                onDismissRequest = { pendingCompleteTask = null },
                onCaptureProof = {
                    val target = task
                    pendingCompleteTask = null
                    onOpenProofCamera(target)
                },
                onQuickComplete = {
                    val target = task
                    pendingCompleteTask = null
                    onQuickComplete(target)
                }
            )
        }

        // Quick Note Bottom Sheet
        if (showQuickNoteSheet) {
            QuickNoteBottomSheet(
                onDismissRequest = { showQuickNoteSheet = false },
                onSave = { newTask ->
                    showQuickNoteSheet = false
                    onSaveQuickNote(newTask)
                },
                existingFolders = allFolders
            )
        }
    }
}

/**
 * Featured Large Card across top of Feed (Page 3 & 4: "Tưới cây ban công" Card)
 */
@Composable
fun FeaturedTaskCard(
    task: Task,
    onToggleStatus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val photoFile = remember(task.beforeImageUri, task.afterImageUri, task.isCompleted) {
        if (task.hasProof && !task.afterImageUri.isNullOrBlank() && File(task.afterImageUri).exists()) {
            File(task.afterImageUri)
        } else if (task.hasImage && File(task.beforeImageUri).exists()) {
            File(task.beforeImageUri)
        } else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, if (task.isCompleted) colors.done.copy(alpha = 0.35f) else colors.border)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Photo Background with gradient overlay
            if (photoFile != null && photoFile.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = task.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x26000000),
                                    Color.Transparent,
                                    Color(0xCC000000)
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.elevated)
                )
            }

            // Top-Left Badges: [★ Ghim] and [Cao]
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.isPinnedToWidget) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xD9000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "★", color = colors.flash, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Ghim", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (task.priorityEnum == TaskPriority.HIGH) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xD9FF7A6B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Cao", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (task.priorityEnum == TaskPriority.MEDIUM) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xD9FFD23F), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Vừa", color = Color(0xFF17140A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom Content: Title & Subtitle on Left, Done Checkmark on Right
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = task.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val timeStr = if (task.reminderTime != null) {
                        DateTimeUtils.formatDateTime(task.reminderTime)
                    } else "Hôm nay"
                    Text(
                        text = "$timeStr · ${task.folderName}",
                        color = Color(0xFFDDD7C8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Yellow Circular Checkmark Done Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.flash)
                        .clickable(onClick = onToggleStatus),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Hoàn thành",
                        tint = colors.onFlash,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bento Grid Card (Compact 2-Column Mode)
 * Matches Page 3 & 4: Quick Notes with subtle Note icon, Photo tasks with bottom gradient, Completed with PiP
 */
@Composable
fun BentoTaskCard(
    task: Task,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val isDark = isSystemInDarkTheme()

    val photoFile = remember(task.beforeImageUri, task.afterImageUri, task.isCompleted) {
        if (task.hasProof && !task.afterImageUri.isNullOrBlank() && File(task.afterImageUri).exists()) {
            File(task.afterImageUri)
        } else if (task.hasImage && File(task.beforeImageUri).exists()) {
            File(task.beforeImageUri)
        } else null
    }

    val isQuickNote = photoFile == null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) colors.done.copy(alpha = 0.35f) else colors.border
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isQuickNote) {
                // Quick Note Layout: Document icon in background + Priority pill + Title + Subtitle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surface)
                        .padding(14.dp)
                ) {
                    // Top-Left Priority Badge
                    if (task.priorityEnum == TaskPriority.HIGH) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(colors.urgent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(text = "Cao", color = colors.urgent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (task.priorityEnum == TaskPriority.MEDIUM) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(colors.warning.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(text = "Vừa", color = colors.warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Center: Subtle note document icon
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.25f),
                        modifier = Modifier
                            .size(46.dp)
                            .align(Alignment.Center)
                    )

                    // Bottom: Title & Subtitle
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = task.title,
                            color = if (task.isCompleted) colors.textMuted else colors.text,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val subtitle = if (task.reminderTime != null) {
                            DateTimeUtils.formatDateTime(task.reminderTime)
                        } else task.folderName
                        Text(
                            text = subtitle,
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Photo Task Layout: 1:1 Image with Bottom Gradient
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = task.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Bottom Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x26000000),
                                    Color.Transparent,
                                    Color(0xCC000000)
                                )
                            )
                        )
                )

                // Top-Left Badges: "Đã xong" green pill OR Priority
                if (task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(colors.done, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(text = "Đã xong", color = Color(0xFF17140A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (task.priorityEnum == TaskPriority.HIGH) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(Color(0xD9FF7A6B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(text = "Cao", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Top-Right PiP thumbnail of After photo if present
                if (task.hasProof && !task.afterImageUri.isNullOrBlank() && File(task.afterImageUri).exists()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(task.afterImageUri))
                                .crossfade(true)
                                .build(),
                            contentDescription = "After Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Bottom Content: Title & Subtitle
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = task.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val subtitle = if (task.isCompleted && task.completedAt != null) {
                        "Xong lúc ${DateTimeUtils.formatTime(task.completedAt)}"
                    } else if (task.reminderTime != null) {
                        DateTimeUtils.formatDateTime(task.reminderTime)
                    } else task.folderName
                    Text(
                        text = subtitle,
                        color = Color(0xFFDDD7C8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Floating Glass Dock at bottom center (Page 3 & 4)
 * 3 Symmetrical Controls: [ ⊞ ] Bento Toggle - (O) Big Shutter Button - [ ||| ] Kanban Board
 */
@Composable
private fun FloatingGlassDock(
    onOpenLiveCamera: () -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenBoard: () -> Unit,
    viewMode: ViewMode,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = if (isDark) Color.Transparent else Color(0x1F17140A),
                spotColor = if (isDark) Color.Transparent else Color(0x1F17140A)
            )
            .clip(RoundedCornerShape(36.dp))
            .background(colors.surface.copy(alpha = 0.96f))
            .border(1.dp, colors.border, RoundedCornerShape(36.dp))
            .padding(horizontal = 22.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // 1. Left Button: Grid / Bento Mode Toggle [ ⊞ ]
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleViewMode),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (viewMode == ViewMode.BENTO_GRID) Icons.Filled.GridView else SnapTaskIcons.ListFeed,
                    contentDescription = "Chuyển chế độ xem",
                    tint = if (viewMode == ViewMode.BENTO_GRID) colors.flash else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 2. Center Button: The Big Shutter Button (60dp outer with 3dp flash stroke, 44dp inner flash disc)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(3.dp, colors.flash, CircleShape)
                    .clickable(onClick = onOpenLiveCamera),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.flash)
                )
            }

            // 3. Right Button: Kanban Board / Columns View [ ||| ]
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenBoard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ViewWeek,
                    contentDescription = "Bảng Cột Tiến Độ",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
