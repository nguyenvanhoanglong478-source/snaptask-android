package com.snaptask.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.snaptask.app.ui.theme.AccentPrimary
import com.snaptask.app.ui.theme.AccentSecondary
import com.snaptask.app.ui.theme.AuroraDockGradient
import com.snaptask.app.ui.theme.AuroraGradient
import com.snaptask.app.ui.theme.BgElevated
import com.snaptask.app.ui.theme.BgPrimary
import com.snaptask.app.ui.theme.BgSurface
import com.snaptask.app.ui.theme.BorderHighlight
import com.snaptask.app.ui.theme.BorderSubtle
import com.snaptask.app.ui.theme.CardBottomScrim
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.StatusAmber
import com.snaptask.app.ui.theme.StatusSuccess
import com.snaptask.app.ui.theme.StatusUrgent
import com.snaptask.app.ui.theme.TextMuted
import com.snaptask.app.ui.theme.TextPrimary
import com.snaptask.app.ui.theme.TextSecondary
import java.io.File

enum class ViewMode {
    BENTO_GRID,
    FEED_FULL
}

enum class SmartFolderType {
    ALL,
    SCHEDULED,
    GALLERY,
    PINNED
}

enum class SortOption(val displayName: String) {
    NEWEST("Mới nhất"),
    HIGH_PRIORITY("Ưu tiên cao"),
    UPCOMING("Sắp đến giờ"),
    UNCOMPLETED("Chưa xong")
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
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var viewMode by remember { mutableStateOf(ViewMode.FEED_FULL) }
    var selectedSmartFolder by remember { mutableStateOf<SmartFolderType?>(SmartFolderType.ALL) }
    var selectedCustomFolder by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var pendingCompleteTask by remember { mutableStateOf<Task?>(null) }
    var flippedMap by remember { mutableStateOf(mapOf<Long, Boolean>()) }

    var showQuickNoteSheet by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var customFoldersList by remember { mutableStateOf(listOf("Cá nhân", "Công việc", "Học tập", "Mua sắm")) }

    // Aggregate custom folders with existing tasks
    val allFolders = remember(tasks, customFoldersList) {
        val taskFolders = tasks.map { it.folderName }.filter { it.isNotBlank() }
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

    // Filter & Sort pipeline
    val processedTasks = remember(tasks, selectedSmartFolder, selectedCustomFolder, searchQuery, sortOption) {
        var list = tasks

        // 1. Filter by Smart Folder
        list = when (selectedSmartFolder) {
            SmartFolderType.SCHEDULED -> list.filter { it.hasReminder }
            SmartFolderType.GALLERY -> list.filter { it.hasImage }
            SmartFolderType.PINNED -> list.filter { it.isPinnedToWidget }
            SmartFolderType.ALL, null -> list
        }

        // 2. Filter by Custom Folder
        if (selectedCustomFolder != null) {
            list = list.filter { it.folderName.equals(selectedCustomFolder, ignoreCase = true) }
        }

        // 3. Filter by Search Query
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.folderName.contains(searchQuery, ignoreCase = true)
            }
        }

        // 4. Sort
        when (sortOption) {
            SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOption.HIGH_PRIORITY -> list.sortedWith(
                compareByDescending<Task> { it.priority == TaskPriority.HIGH.name }
                    .thenByDescending { it.priority == TaskPriority.MEDIUM.name }
                    .thenByDescending { it.createdAt }
            )
            SortOption.UPCOMING -> list.sortedWith(
                compareBy<Task> { it.reminderTime ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )
            SortOption.UNCOMPLETED -> list.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgPrimary,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 6.dp)
            ) {
                // Header Bar: App Name + View Switcher + Search + Sort
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SnapTaskLogo(size = 28.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SnapTask",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AccentPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "3.0",
                                        color = AccentPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${tasks.count { !it.isCompleted }} việc cần làm • ${tasks.size} tổng",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Search Button (42x42 square)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF15171E))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { isSearchActive = !isSearchActive },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Search,
                                contentDescription = "Tìm kiếm",
                                tint = if (isSearchActive) AccentPrimary else TextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 2. View Mode Switcher Button (42x42 square, Icon-Only)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF15171E))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewMode = if (viewMode == ViewMode.BENTO_GRID) ViewMode.FEED_FULL else ViewMode.BENTO_GRID
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.BENTO_GRID) SnapTaskIcons.ListFeed else SnapTaskIcons.GridBento,
                                contentDescription = "Chuyển chế độ xem",
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 3. Sort Button (42x42 square)
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF15171E))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                    .clickable { showSortMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = SnapTaskIcons.Sort,
                                    contentDescription = "Sắp xếp",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier
                                    .background(BgSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            ) {
                                SortOption.entries.forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = opt.displayName,
                                                color = if (sortOption == opt) AccentPrimary else TextPrimary,
                                                fontWeight = if (sortOption == opt) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            sortOption = opt
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
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
                        placeholder = { Text("Tìm theo tên, ghi chú, thư mục...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BgSurface,
                            unfocusedContainerColor = BgSurface,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Xoá", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                }

                // 1. Smart Folders Bento Row (2x2 Grid / Quick Segmented Row)
                SmartFoldersSection(
                    tasks = tasks,
                    selectedFolder = selectedSmartFolder,
                    onSelectFolder = { selected ->
                        selectedSmartFolder = if (selectedSmartFolder == selected) null else selected
                    }
                )

                // 2. Custom Folders Row + "+ Thư mục"
                CustomFoldersRow(
                    folders = allFolders,
                    selectedFolder = selectedCustomFolder,
                    tasks = tasks,
                    onSelectFolder = { folder ->
                        selectedCustomFolder = if (selectedCustomFolder == folder) null else folder
                    },
                    onAddNewFolder = { showNewFolderDialog = true }
                )

                // 3. Feed Content
                if (processedTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(BgSurface, CircleShape)
                                    .border(1.dp, BorderSubtle, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(text = "Chưa có nhiệm vụ nào", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bấm nút Camera để chụp task hoặc Ghi chú nhanh bên dưới",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(processedTasks, key = { it.id }) { task ->
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
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
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

            // 4. FLOATING GLASS DOCK (Bottom Center Pill)
            FloatingGlassDock(
                onOpenLiveCamera = onOpenLiveCamera,
                onOpenQuickNote = { showQuickNoteSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }

        // Completion Proof Bottom Sheet (when uncompleted task checkbox is tapped)
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

        // New Folder Dialog
        if (showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                containerColor = BgSurface,
                title = {
                    Text("Tạo thư mục mới", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        placeholder = { Text("Tên thư mục (ví dụ: Gym, Dự án X)", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFolderNameInput.isNotBlank()) {
                                customFoldersList = (customFoldersList + newFolderNameInput.trim()).distinct()
                                selectedCustomFolder = newFolderNameInput.trim()
                                newFolderNameInput = ""
                                showNewFolderDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tạo thư mục", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) {
                        Text("Huỷ", color = TextSecondary)
                    }
                }
            )
        }
    }
}

/**
 * Smart Folders Bento Row: 4 key compartments
 */
@Composable
private fun SmartFoldersSection(
    tasks: List<Task>,
    selectedFolder: SmartFolderType?,
    onSelectFolder: (SmartFolderType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmartFolderItem(
            title = "Tất cả",
            count = tasks.size,
            icon = Icons.Filled.AllInclusive,
            isSelected = selectedFolder == SmartFolderType.ALL,
            onClick = { onSelectFolder(SmartFolderType.ALL) },
            modifier = Modifier.weight(1f)
        )
        SmartFolderItem(
            title = "Lên lịch",
            count = tasks.count { it.hasReminder },
            icon = Icons.Filled.Alarm,
            isSelected = selectedFolder == SmartFolderType.SCHEDULED,
            onClick = { onSelectFolder(SmartFolderType.SCHEDULED) },
            modifier = Modifier.weight(1f)
        )
        SmartFolderItem(
            title = "Kho ảnh",
            count = tasks.count { it.hasImage },
            icon = Icons.Filled.Image,
            isSelected = selectedFolder == SmartFolderType.GALLERY,
            onClick = { onSelectFolder(SmartFolderType.GALLERY) },
            modifier = Modifier.weight(1f)
        )
        SmartFolderItem(
            title = "Đã ghim",
            count = tasks.count { it.isPinnedToWidget },
            icon = Icons.Filled.PushPin,
            isSelected = selectedFolder == SmartFolderType.PINNED,
            onClick = { onSelectFolder(SmartFolderType.PINNED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SmartFolderItem(
    title: String,
    count: Int,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) AccentPrimary.copy(alpha = 0.2f) else BgSurface)
            .border(1.dp, if (isSelected) AccentPrimary else BorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AccentPrimary else TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = count.toString(),
                    color = if (isSelected) AccentPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Custom Folders Row with item counts and "+ Thư mục"
 */
@Composable
private fun CustomFoldersRow(
    folders: List<String>,
    selectedFolder: String?,
    tasks: List<Task>,
    onSelectFolder: (String) -> Unit,
    onAddNewFolder: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(folders) { folder ->
            val isSelected = selectedFolder.equals(folder, ignoreCase = true)
            val count = tasks.count { it.folderName.equals(folder, ignoreCase = true) }
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) AccentPrimary else BgElevated)
                    .border(1.dp, if (isSelected) AccentPrimary else BorderSubtle, RoundedCornerShape(10.dp))
                    .clickable { onSelectFolder(folder) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$folder ($count)",
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddNewFolder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SnapTaskIcons.Plus,
                    contentDescription = "Thêm thư mục",
                    tint = AccentPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Floating Glass Dock at bottom center
 */
@Composable
private fun FloatingGlassDock(
    onOpenLiveCamera: () -> Unit,
    onOpenQuickNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(BgSurface.copy(alpha = 0.92f))
            .border(1.dp, BorderHighlight, RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Center Hero: Shutter Camera Capture Button (56dp circle with Aurora Gradient)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AuroraDockGradient)
                    .border(2.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .clickable(onClick = onOpenLiveCamera),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Chụp Task Mới",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Quick Note Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgElevated)
                    .clickable(onClick = onOpenQuickNote)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = "Ghi chú nhanh",
                    tint = AccentPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ghi chú",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Bento Grid Card (Compact 2-Column Mode)
 */
@Composable
fun BentoTaskCard(
    task: Task,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (task.priorityEnum) {
        TaskPriority.HIGH -> StatusUrgent
        TaskPriority.MEDIUM -> StatusAmber
        TaskPriority.LOW -> Color.Transparent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (task.isCompleted) StatusSuccess.copy(alpha = 0.35f) else BorderSubtle
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(BgElevated)
        ) {
            val photoFile = remember(task.beforeImageUri, task.afterImageUri, task.isCompleted) {
                if (task.hasProof && !task.afterImageUri.isNullOrBlank() && File(task.afterImageUri).exists()) {
                    File(task.afterImageUri)
                } else if (task.hasImage) {
                    File(task.beforeImageUri)
                } else null
            }

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
            } else {
                // Text-only Bento layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
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
                            Box(
                                modifier = Modifier
                                    .background(BgSurface, RoundedCornerShape(6.dp))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = task.folderName,
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (task.priorityEnum == TaskPriority.HIGH || task.priorityEnum == TaskPriority.MEDIUM) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(priorityColor)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF15171E))
                                .border(1.dp, BorderSubtle, CircleShape)
                                .clickable(onClick = onTogglePin),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Pin,
                                contentDescription = "Ghim",
                                tint = if (task.isPinnedToWidget) AccentPrimary else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = task.title,
                        color = if (task.isCompleted) TextMuted else TextPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(onClick = onToggleStatus),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = "Trạng thái",
                                tint = if (task.isCompleted) StatusSuccess else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (task.parsedSubtasks.isNotEmpty()) {
                            Text(
                                text = "${task.completedSubtasksCount}/${task.parsedSubtasks.size}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (photoFile != null && photoFile.exists()) {
                // Top Row: Tag badge + 6dp priority dot on Left, 30dp round Pin Button on Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (task.hasProof) StatusSuccess else Color(0xCC000000), RoundedCornerShape(7.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (task.hasProof) "AFTER" else task.folderName,
                                color = if (task.hasProof) BgPrimary else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (task.priorityEnum == TaskPriority.HIGH || task.priorityEnum == TaskPriority.MEDIUM) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF15171E).copy(alpha = 0.85f))
                            .border(1.dp, BorderSubtle, CircleShape)
                            .clickable(onClick = onTogglePin),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Pin,
                            contentDescription = "Ghim",
                            tint = if (task.isPinnedToWidget) AccentPrimary else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Bottom Scrim Bar: 24dp Checkmark spaced 12dp from title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(CardBottomScrim)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onToggleStatus),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = "Hoàn thành",
                            tint = if (task.isCompleted) StatusSuccess else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = task.title,
                        color = if (task.isCompleted) TextMuted else TextPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
