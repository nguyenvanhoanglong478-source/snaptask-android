package com.snaptask.app.ui.screens.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.data.preferences.FolderPreferencesRepository
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.components.DockTab
import com.snaptask.app.ui.components.FloatingBottomDock
import com.snaptask.app.ui.components.SnapCheckbox
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.components.SnapSegmentedControl
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class KanbanColumnData(
    val key: String,
    val title: String,
    val dotColor: Color,
    val iconKey: String? = null,
    val tasks: List<Task>
)

@Composable
fun BoardScreen(
    tasks: List<Task>,
    isLoading: Boolean,
    folders: List<CustomFolder>,
    onTaskClick: (Task) -> Unit,
    onToggleTaskStatus: (Task) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenFolders: () -> Unit,
    onSelectDockTab: (DockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val now = System.currentTimeMillis()
    var groupMode by rememberSaveable { mutableIntStateOf(0) }

    val columns = remember(groupMode, tasks, folders, colors.hm4, colors.urgent, colors.textMuted) {
        if (groupMode == 0) {
            val openGeneral = tasks.filter { !it.isCompleted && it.reminderTime == null && !it.isPinnedToWidget }
                .sortedByDescending { it.createdAt }
            val openScheduled = tasks.filter { !it.isCompleted && (it.reminderTime != null || it.isPinnedToWidget) }
                .sortedBy { it.reminderTime ?: it.createdAt }
            val doneList = tasks.filter { it.isCompleted }
                .sortedByDescending { it.completedAt ?: it.createdAt }
            val hasOverdue = openScheduled.any { it.reminderTime != null && it.reminderTime < now }

            listOf(
                KanbanColumnData(
                    key = "status_todo",
                    title = "Cần làm",
                    dotColor = colors.textMuted,
                    tasks = openGeneral
                ),
                KanbanColumnData(
                    key = "status_focus",
                    title = "Đã hẹn & Ghim",
                    dotColor = if (hasOverdue) colors.urgent else colors.hm4,
                    tasks = openScheduled
                ),
                KanbanColumnData(
                    key = "status_done",
                    title = "Đã xong",
                    dotColor = colors.hm4,
                    tasks = doneList
                )
            )
        } else {
            val knownNames = folders.map { it.name.lowercase(Locale.getDefault()) }.toSet()
            val folderColumns = folders.map { folder ->
                val list = tasks.filter { it.folderName.equals(folder.name, ignoreCase = true) }
                    .sortedWith(compareBy<Task> { it.isCompleted }.thenByDescending { it.createdAt })
                KanbanColumnData(
                    key = "folder_${folder.id}",
                    title = folder.name,
                    dotColor = Color(folder.colorHex),
                    iconKey = folder.iconKey,
                    tasks = list
                )
            }
            val unassigned = tasks.filter { it.folderName.lowercase(Locale.getDefault()) !in knownNames }
            if (unassigned.isNotEmpty()) {
                folderColumns + KanbanColumnData(
                    key = "folder_unassigned",
                    title = FolderPreferencesRepository.UNASSIGNED_FOLDER_NAME,
                    dotColor = colors.textMuted,
                    tasks = unassigned
                )
            } else {
                folderColumns
            }
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
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bảng",
                    color = colors.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp
                )

                Box(
                    modifier = Modifier
                        .width(124.dp)
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(colors.elevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(role = Role.Button, onClick = onOpenFolders),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Folder,
                            contentDescription = null,
                            tint = colors.text,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Thư mục",
                            color = colors.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            SnapSegmentedControl(
                options = listOf("Theo trạng thái", "Theo thư mục"),
                selectedIndex = groupMode,
                onSelect = { groupMode = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(columns, key = { it.key }) { column ->
                    BoardKanbanColumn(
                        column = column,
                        isLoading = isLoading,
                        nowMillis = now,
                        onTaskClick = onTaskClick,
                        onToggleTaskStatus = onToggleTaskStatus,
                        onOpenCamera = onOpenCamera
                    )
                }

                if (groupMode == 1) {
                    item(key = "manage_folders_column") {
                        val dashColor = colors.borderHighlight
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(role = Role.Button, onClick = onOpenFolders)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRoundRect(
                                    color = dashColor,
                                    cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                    )
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = SnapTaskIcons.Plus,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Thêm / Sửa thư mục",
                                    color = colors.textSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(104.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.bg.copy(alpha = 0.94f), colors.bg)
                    )
                )
        )

        FloatingBottomDock(
            activeTab = DockTab.BOARD,
            onSelectTab = onSelectDockTab,
            onShutterClick = onOpenCamera,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun BoardKanbanColumn(
    column: KanbanColumnData,
    isLoading: Boolean,
    nowMillis: Long,
    onTaskClick: (Task) -> Unit,
    onToggleTaskStatus: (Task) -> Unit,
    onOpenCamera: () -> Unit
) {
    val colors = MaterialTheme.snapColors
    val dashColor = colors.borderHighlight

    Column(
        modifier = Modifier
            .width(276.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(column.dotColor)
                )
                if (column.iconKey != null) {
                    Icon(
                        imageVector = SnapTaskIcons.folderIconByKey(column.iconKey),
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = column.title,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .height(26.dp)
                    .clip(CircleShape)
                    .background(colors.elevated)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = column.tasks.size.toString(),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isLoading) {
                item(key = "col_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.elevated)
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đang tải…",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (column.tasks.isEmpty()) {
                item(key = "col_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.elevated)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Chưa có việc nào trong cột này.",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(column.tasks, key = { it.id }) { task ->
                    BoardTaskCard(
                        task = task,
                        nowMillis = nowMillis,
                        onClick = { onTaskClick(task) },
                        onToggleStatus = { onToggleTaskStatus(task) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onOpenCamera),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            }
            Text(
                text = "+ Chụp thêm",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BoardTaskCard(
    task: Task,
    nowMillis: Long,
    onClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val beforeFile = remember(task.beforeImageUri) {
        if (task.beforeImageUri.isNotBlank()) File(task.beforeImageUri) else null
    }
    val afterFile = remember(task.afterImageUri) {
        if (!task.afterImageUri.isNullOrBlank()) File(task.afterImageUri) else null
    }
    val hasBefore = beforeFile != null && beforeFile.exists()
    val hasAfter = afterFile != null && afterFile.exists()
    val isOverdue = !task.isCompleted && task.reminderTime != null && task.reminderTime < nowMillis
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.elevated)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        if (hasBefore || hasAfter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                if (hasBefore && hasAfter) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(beforeFile).crossfade(true).build(),
                            contentDescription = "Trước",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFFAFAF6))
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(afterFile).crossfade(true).build(),
                            contentDescription = "Sau",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(if (hasAfter) afterFile else beforeFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = task.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                when {
                    task.isCompleted -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(colors.flash)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${SnapDateHelper.durationMinutes(task)} phút",
                                color = colors.onFlash,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    isOverdue -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(colors.urgent)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Trễ hạn",
                                color = colors.onUrgent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    task.reminderTime != null -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(colors.bubble)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeFormat.format(Date(task.reminderTime)),
                                color = Color(0xFFFAFAF6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (task.isCompleted) colors.textMuted else colors.text,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.folderName,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Checkbox,
                        onClickLabel = "Đổi trạng thái hoàn thành",
                        onClick = onToggleStatus
                    ),
                contentAlignment = Alignment.Center
            ) {
                SnapCheckbox(checked = task.isCompleted)
            }
        }
    }
}
