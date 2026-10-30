package com.snaptask.app.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.data.preferences.RepeatCadence
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.components.DockTab
import com.snaptask.app.ui.components.FloatingBottomDock
import com.snaptask.app.ui.components.RoundIconButton
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.screens.home.components.CompletionProofBottomSheet
import com.snaptask.app.ui.screens.home.components.QuickNoteBottomSheet
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class FeedFilterChip(
    val key: String,
    val label: String,
    val folderColorHex: Long? = null,
    val folderIconKey: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tasks: List<Task>,
    isLoading: Boolean,
    folders: List<CustomFolder>,
    smartUrgentEnabled: Boolean,
    smartProofEnabled: Boolean,
    repeatByTaskId: Map<Long, RepeatCadence>,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenProofCamera: (Task) -> Unit,
    onQuickComplete: (Task) -> Unit,
    onToggleTaskStatus: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onSaveQuickNote: (Task) -> Unit,
    onOpenCalendar: (Long) -> Unit,
    onOpenRecap: () -> Unit,
    onOpenFolders: () -> Unit,
    onSelectDockTab: (DockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val scope = rememberCoroutineScope()
    val now = System.currentTimeMillis()
    val todayKey = remember(now / 60_000L) { SnapDateHelper.dayKey(now) }

    var activeFilter by rememberSaveable { mutableStateOf("all") }
    var proofTargetTask by remember { mutableStateOf<Task?>(null) }
    var showQuickNoteSheet by rememberSaveable { mutableStateOf(false) }

    var stampedTaskId by remember { mutableLongStateOf(-1L) }
    val stampScale = remember { Animatable(0f) }

    val folderByName = remember(folders) {
        folders.associateBy { it.name.lowercase(Locale.getDefault()) }
    }

    val openTasks = remember(tasks, now / 30_000L) {
        tasks.filter { !it.isCompleted }.sortedWith(
            compareByDescending<Task> { it.reminderTime != null && it.reminderTime < now }
                .thenByDescending { it.priorityEnum == TaskPriority.HIGH }
                .thenBy { it.reminderTime ?: Long.MAX_VALUE }
                .thenByDescending { it.createdAt }
        )
    }
    val completedTasks = remember(tasks) {
        tasks.filter { it.isCompleted }.sortedByDescending { it.completedAt ?: it.createdAt }
    }
    val urgentTasks = remember(openTasks, now / 30_000L) {
        openTasks.filter {
            it.priorityEnum == TaskPriority.HIGH || (it.reminderTime != null && it.reminderTime < now)
        }
    }
    val proofTasks = remember(completedTasks) {
        completedTasks.filter { it.hasProof }
    }
    val completedToday = remember(completedTasks, todayKey) {
        completedTasks.filter {
            SnapDateHelper.dayKey(it.completedAt ?: it.createdAt) == todayKey
        }
    }

    val filterChips = remember(
        openTasks.size,
        completedTasks.size,
        urgentTasks.size,
        proofTasks.size,
        smartUrgentEnabled,
        smartProofEnabled,
        folders
    ) {
        buildList {
            add(FeedFilterChip("all", "Cần làm · ${openTasks.size}"))
            add(FeedFilterChip("done", "Đã xong · ${completedTasks.size}"))
            if (smartUrgentEnabled) {
                add(FeedFilterChip("urgent", "Gấp & Trễ · ${urgentTasks.size}"))
            }
            if (smartProofEnabled) {
                add(FeedFilterChip("proof", "Trước / Sau · ${proofTasks.size}"))
            }
            folders.forEach { f ->
                val folderOpen = openTasks.count { it.folderName.equals(f.name, ignoreCase = true) }
                val label = if (folderOpen > 0) "${f.name} · $folderOpen" else f.name
                add(
                    FeedFilterChip(
                        key = "folder:${f.name}",
                        label = label,
                        folderColorHex = f.colorHex,
                        folderIconKey = f.iconKey
                    )
                )
            }
        }
    }

    val effectiveFilter = remember(activeFilter, filterChips) {
        if (filterChips.any { it.key == activeFilter }) activeFilter else "all"
    }

    val visibleOpenTasks = remember(effectiveFilter, openTasks, urgentTasks) {
        when {
            effectiveFilter == "all" -> openTasks
            effectiveFilter == "urgent" -> urgentTasks
            effectiveFilter == "done" || effectiveFilter == "proof" -> emptyList()
            effectiveFilter.startsWith("folder:") -> {
                val targetFolder = effectiveFilter.removePrefix("folder:")
                openTasks.filter { it.folderName.equals(targetFolder, ignoreCase = true) }
            }
            else -> openTasks
        }
    }

    val visibleCompletedGrid = remember(effectiveFilter, completedToday, completedTasks, proofTasks) {
        when {
            effectiveFilter == "all" -> completedToday
            effectiveFilter == "done" -> completedTasks
            effectiveFilter == "proof" -> proofTasks
            effectiveFilter.startsWith("folder:") -> {
                val targetFolder = effectiveFilter.removePrefix("folder:")
                completedTasks.filter { it.folderName.equals(targetFolder, ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    val completedSectionTitle = remember(effectiveFilter, visibleCompletedGrid.size) {
        when {
            effectiveFilter == "done" -> "TẤT CẢ ĐÃ XONG · ${visibleCompletedGrid.size}"
            effectiveFilter == "proof" -> "BỘ SƯU TẬP TRƯỚC / SAU · ${visibleCompletedGrid.size}"
            effectiveFilter.startsWith("folder:") -> "ĐÃ XONG TRONG THƯ MỤC · ${visibleCompletedGrid.size}"
            else -> "ĐÃ XONG HÔM NAY · ${visibleCompletedGrid.size}"
        }
    }

    fun triggerDoneStamp(task: Task) {
        scope.launch {
            stampedTaskId = task.id
            stampScale.snapTo(0.45f)
            stampScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            delay(220)
            stampedTaskId = -1L
            if (task.hasImage) {
                proofTargetTask = task
            } else {
                onQuickComplete(task)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 136.dp)
        ) {
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button) { onOpenCalendar(now) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = SnapDateHelper.formatVietnameseDate(now),
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = SnapTaskIcons.ChevronRight,
                                    contentDescription = "Mở lịch tháng",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Hôm nay",
                                color = colors.text,
                                fontSize = 32.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.8).sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundIconButton(
                                icon = if (isDarkTheme) SnapTaskIcons.Sun else SnapTaskIcons.Moon,
                                contentDescription = if (isDarkTheme) "Chuyển giao diện sáng" else "Chuyển giao diện tối",
                                onClick = onToggleTheme
                            )
                            RoundIconButton(
                                icon = SnapTaskIcons.Flame,
                                contentDescription = "Nhìn lại tuần",
                                onClick = onOpenRecap
                            )
                        }
                    }

                    WeekStripRow(
                        tasks = tasks,
                        todayKey = todayKey,
                        onSelectDay = onOpenCalendar
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterChips, key = { it.key }) { chip ->
                            val selected = chip.key == effectiveFilter
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 104.dp)
                                    .height(38.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) colors.text else colors.elevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) colors.text else colors.border,
                                        shape = CircleShape
                                    )
                                    .clickable(role = Role.Tab) { activeFilter = chip.key }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (chip.folderColorHex != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(chip.folderColorHex))
                                        )
                                    }
                                    if (chip.folderIconKey != null) {
                                        Icon(
                                            imageVector = SnapTaskIcons.folderIconByKey(chip.folderIconKey),
                                            contentDescription = null,
                                            tint = if (selected) colors.bg else colors.text,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = chip.label,
                                        color = if (selected) colors.bg else colors.text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (isLoading) {
                item(key = "loading_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
                            .padding(vertical = 44.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = colors.hm4,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Đang tải danh sách việc…",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else if (visibleOpenTasks.isEmpty() && visibleCompletedGrid.isEmpty()) {
                item(key = "empty_state") {
                    FeedEmptyCard(
                        filterKey = effectiveFilter,
                        onOpenCamera = onOpenCamera,
                        onOpenQuickNote = { showQuickNoteSheet = true }
                    )
                }
            } else {
                items(visibleOpenTasks, key = { "open_${it.id}" }) { task ->
                    val folderMatch = folderByName[task.folderName.lowercase(Locale.getDefault())]
                    val cadence = repeatByTaskId[task.id] ?: RepeatCadence.fromKeyOrTag(task.tag)
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (task.hasImage) {
                            PhotoFeedTaskCard(
                                task = task,
                                folder = folderMatch,
                                repeatCadence = cadence,
                                nowMillis = now,
                                isStamped = stampedTaskId == task.id,
                                stampScale = if (stampedTaskId == task.id) stampScale.value else 0f,
                                onCardClick = { onTaskClick(task) },
                                onCompleteClick = { triggerDoneStamp(task) }
                            )
                        } else {
                            QuickNoteFeedCard(
                                task = task,
                                folder = folderMatch,
                                nowMillis = now,
                                onCardClick = { onTaskClick(task) },
                                onFolderClick = onOpenFolders,
                                onCompleteClick = { triggerDoneStamp(task) }
                            )
                        }
                    }
                }

                if (visibleCompletedGrid.isNotEmpty()) {
                    item(key = "completed_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = completedSectionTitle,
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button, onClick = onOpenRecap)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Nhìn lại tuần",
                                    color = colors.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = SnapTaskIcons.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.text,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    val rows = visibleCompletedGrid.chunked(2)
                    items(rows, key = { row -> "done_row_${row.first().id}" }) { rowTasks ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompletedBentoTile(
                                task = rowTasks[0],
                                onClick = { onTaskClick(rowTasks[0]) },
                                onReopen = { onToggleTaskStatus(rowTasks[0]) },
                                modifier = Modifier.weight(1f)
                            )
                            if (rowTasks.size > 1) {
                                CompletedBentoTile(
                                    task = rowTasks[1],
                                    onClick = { onTaskClick(rowTasks[1]) },
                                    onReopen = { onToggleTaskStatus(rowTasks[1]) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
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
                .height(112.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.bg.copy(alpha = 0.94f), colors.bg)
                    )
                )
        )

        FloatingBottomDock(
            activeTab = DockTab.TODAY,
            onSelectTab = onSelectDockTab,
            onShutterClick = onOpenCamera,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )
    }

    val targetForProof = proofTargetTask
    if (targetForProof != null) {
        CompletionProofBottomSheet(
            task = targetForProof,
            onDismissRequest = { proofTargetTask = null },
            onCaptureProof = {
                val chosen = targetForProof
                proofTargetTask = null
                onOpenProofCamera(chosen)
            },
            onQuickComplete = {
                val chosen = targetForProof
                proofTargetTask = null
                onQuickComplete(chosen)
            }
        )
    }

    if (showQuickNoteSheet) {
        QuickNoteBottomSheet(
            onDismissRequest = { showQuickNoteSheet = false },
            onSave = { created ->
                showQuickNoteSheet = false
                onSaveQuickNote(created)
            },
            existingFolders = folders.map { it.name }
        )
    }
}

@Composable
private fun WeekStripRow(
    tasks: List<Task>,
    todayKey: Long,
    onSelectDay: (Long) -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekDays.forEach { (dayKeyMillis, wdShort, dayNum) ->
            val isToday = dayKeyMillis == todayKey
            val count = (countsByDay[dayKeyMillis] ?: 0).coerceAtMost(3)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isToday) colors.flash else Color.Transparent)
                    .clickable(role = Role.Button) { onSelectDay(dayKeyMillis) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = wdShort,
                    color = if (isToday) colors.onFlash else colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dayNum.toString(),
                    color = if (isToday) colors.onFlash else colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.height(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(count) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isToday) colors.onFlash else colors.hm4)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoFeedTaskCard(
    task: Task,
    folder: CustomFolder?,
    repeatCadence: RepeatCadence,
    nowMillis: Long,
    isStamped: Boolean,
    stampScale: Float,
    onCardClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val imageFile = remember(task.beforeImageUri) { File(task.beforeImageUri) }
    val isOverdue = task.reminderTime != null && task.reminderTime < nowMillis
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(36.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(36.dp))
            .clickable(onClick = onCardClick)
    ) {
        if (imageFile.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageFile)
                    .crossfade(true)
                    .build(),
                contentDescription = task.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A18)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ảnh gốc không còn trên thiết bị",
                    color = Color(0xFFB0AFA7),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x400A0A09),
                        0.45f to Color.Transparent,
                        1.0f to Color(0xD90A0A09)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isOverdue && task.reminderTime != null -> {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(colors.urgent)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Clock,
                                contentDescription = null,
                                tint = colors.onUrgent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Trễ ${formatOverdueDuration(nowMillis - task.reminderTime)}",
                                color = colors.onUrgent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                task.reminderTime != null -> {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(colors.bubble)
                            .border(1.dp, Color(0x22FAFAF6), CircleShape)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Clock,
                                contentDescription = null,
                                tint = Color(0xFFFAFAF6),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = timeFormat.format(Date(task.reminderTime)),
                                color = Color(0xFFFAFAF6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                task.priorityEnum == TaskPriority.HIGH -> {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(colors.urgent)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Gấp",
                            color = colors.onUrgent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> Spacer(modifier = Modifier.width(4.dp))
            }

            if (repeatCadence != RepeatCadence.NONE) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(colors.bubble)
                        .border(1.dp, Color(0x22FAFAF6), CircleShape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Repeat,
                            contentDescription = null,
                            tint = Color(0xFFFAFAF6),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = repeatCadence.badgeLabel,
                            color = Color(0xFFFAFAF6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(colors.bubble)
                        .border(1.dp, Color(0x22FAFAF6), CircleShape)
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
                            color = Color(0xFFFAFAF6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.bubble)
                    .border(1.dp, Color(0x1FFAFAF6), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = task.title,
                    color = Color(0xFFFAFAF6),
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.flash)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Đánh dấu hoàn thành",
                        onClick = onCompleteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SnapTaskIcons.Check,
                    contentDescription = "Hoàn thành",
                    tint = colors.onFlash,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isStamped) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x550A0A09)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = stampScale
                            scaleY = stampScale
                            rotationZ = -8f
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.flash)
                        .border(4.dp, colors.onFlash, RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "XONG!",
                        color = colors.onFlash,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickNoteFeedCard(
    task: Task,
    folder: CustomFolder?,
    nowMillis: Long,
    onCardClick: () -> Unit,
    onFolderClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    val colors = MaterialTheme.snapColors
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isOverdue = task.reminderTime != null && task.reminderTime < nowMillis
    val metaSuffix = when {
        isOverdue && task.reminderTime != null -> " · Trễ ${formatOverdueDuration(nowMillis - task.reminderTime)}"
        task.reminderTime != null -> " · ${timeFormat.format(Date(task.reminderTime))}"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
            .clickable(onClick = onCardClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = SnapTaskIcons.TextAa,
                contentDescription = null,
                tint = if (isOverdue) colors.urgent else colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "GHI CHÚ NHANH$metaSuffix",
                color = if (isOverdue) colors.urgent else colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = task.title,
            color = colors.text,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp
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

        val subs = task.parsedSubtasks
        if (subs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Đã xong ${subs.count { it.isDone }}/${subs.size} bước nhỏ",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 104.dp)
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.elevated)
                    .border(2.dp, colors.borderHighlight, CircleShape)
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
                    tint = colors.text,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CompletedBentoTile(
    task: Task,
    onClick: () -> Unit,
    onReopen: () -> Unit,
    modifier: Modifier = Modifier
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
    val mins = SnapDateHelper.durationMinutes(task)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1.0f to Color(0xCC0A0A09)
                        )
                    )
            )
        } else if (hasBefore || hasAfter) {
            val single = if (hasAfter) afterFile else beforeFile
            AsyncImage(
                model = ImageRequest.Builder(context).data(single).crossfade(true).build(),
                contentDescription = task.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1.0f to Color(0xCC0A0A09)
                        )
                    )
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = SnapTaskIcons.TextAa,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.title,
                    color = colors.text,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .height(24.dp)
                .clip(CircleShape)
                .background(colors.flash)
                .clickable(role = Role.Button, onClickLabel = "Mở lại việc", onClick = onReopen)
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$mins phút",
                color = colors.onFlash,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (hasBefore || hasAfter) {
            Text(
                text = task.title,
                color = Color(0xFFFAFAF6),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun FeedEmptyCard(
    filterKey: String,
    onOpenCamera: () -> Unit,
    onOpenQuickNote: () -> Unit
) {
    val colors = MaterialTheme.snapColors
    val (headline, body) = when {
        filterKey == "done" -> Pair(
            "Chưa có việc nào hoàn thành",
            "Khi bạn bấm dấu tích trên một việc, ảnh Trước / Sau sẽ lưu vào đây."
        )
        filterKey == "urgent" -> Pair(
            "Không có việc gấp hay trễ hạn",
            "Mọi lịch nhắc hiện tại đều đang đúng tiến độ."
        )
        filterKey == "proof" -> Pair(
            "Chưa có cặp ảnh Trước / Sau",
            "Chụp thêm ảnh kết quả lúc hoàn thành việc để tạo thẻ so sánh."
        )
        filterKey.startsWith("folder:") -> Pair(
            "Thư mục này chưa có việc nào",
            "Chụp ảnh việc mới hoặc ghi chú chữ để thêm vào thư mục."
        )
        else -> Pair(
            "Chưa có việc cần làm hôm nay",
            "Chụp nhanh góc cần dọn hoặc viết một ghi chú ngắn để bắt đầu."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = headline,
            color = colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            color = colors.textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(colors.flash)
                    .clickable(role = Role.Button, onClick = onOpenCamera),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chụp việc mới",
                    color = colors.onFlash,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(colors.elevated)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(role = Role.Button, onClick = onOpenQuickNote),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ghi chú chữ",
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatOverdueDuration(diffMs: Long): String {
    val mins = (diffMs / 60_000L).coerceAtLeast(1L)
    return when {
        mins < 60L -> "$mins phút"
        mins < 1440L -> "${mins / 60L} giờ"
        else -> "${mins / 1440L} ngày"
    }
}
