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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.data.preferences.RepeatCadence
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.components.DockTab
import com.snaptask.app.ui.components.FloatingBottomDock
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.components.rememberLifecycleTickerMillis
import com.snaptask.app.ui.screens.home.components.CompletedBentoTile
import com.snaptask.app.ui.screens.home.components.CompletionProofBottomSheet
import com.snaptask.app.ui.screens.home.components.FeedEmptyCard
import com.snaptask.app.ui.screens.home.components.HomeHeader
import com.snaptask.app.ui.screens.home.components.PhotoFeedTaskCard
import com.snaptask.app.ui.screens.home.components.QuickNoteBottomSheet
import com.snaptask.app.ui.screens.home.components.QuickNoteFeedCard
import com.snaptask.app.ui.screens.home.components.WeekStripRow
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SmartFilterChip(
    val key: String,
    val label: String
)

private data class FolderFilterChip(
    val key: String,
    val label: String,
    val colorHex: Long,
    val iconKey: String
)

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
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val nowBucketMillis = rememberLifecycleTickerMillis(intervalMs = 30_000L)
    val todayKey = remember(nowBucketMillis / 60_000L) { SnapDateHelper.dayKey(nowBucketMillis) }

    var activeFilter by rememberSaveable { mutableStateOf("all") }
    var proofTargetTask by remember { mutableStateOf<Task?>(null) }
    var showQuickNoteSheet by rememberSaveable { mutableStateOf(false) }

    var stampedTaskId by remember { mutableLongStateOf(-1L) }
    val stampScale = remember { Animatable(0f) }

    val folderByName = remember(folders) {
        folders.associateBy { it.name.lowercase(Locale.getDefault()) }
    }

    val openTasks = remember(tasks, nowBucketMillis) {
        tasks.filter { !it.isCompleted }.sortedWith(
            compareByDescending<Task> {
                (it.reminderTime != null && it.reminderTime < nowBucketMillis) ||
                    DateTimeUtils.isOverdue(it.dueDate?.takeIf { d -> d > 0L }, it.isCompleted, nowBucketMillis)
            }
                .thenByDescending { it.priorityEnum == TaskPriority.HIGH }
                .thenBy { it.dueDate?.takeIf { d -> d > 0L } ?: it.reminderTime ?: Long.MAX_VALUE }
                .thenBy { it.reminderTime ?: Long.MAX_VALUE }
                .thenByDescending { it.createdAt }
        )
    }
    val completedTasks = remember(tasks) {
        tasks.filter { it.isCompleted }.sortedByDescending { it.completedAt ?: it.createdAt }
    }
    val urgentTasks = remember(openTasks, nowBucketMillis) {
        openTasks.filter {
            it.priorityEnum == TaskPriority.HIGH ||
                (it.reminderTime != null && it.reminderTime < nowBucketMillis) ||
                DateTimeUtils.isOverdue(it.dueDate?.takeIf { d -> d > 0L }, it.isCompleted, nowBucketMillis)
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

    val smartFilters = remember(
        openTasks.size,
        completedTasks.size,
        urgentTasks.size,
        proofTasks.size,
        smartUrgentEnabled,
        smartProofEnabled
    ) {
        buildList {
            add(SmartFilterChip("all", "Cần làm · ${openTasks.size}"))
            add(SmartFilterChip("done", "Đã xong · ${completedTasks.size}"))
            if (smartUrgentEnabled) {
                add(SmartFilterChip("urgent", "Gấp & Trễ · ${urgentTasks.size}"))
            }
            if (smartProofEnabled) {
                add(SmartFilterChip("proof", "Trước / Sau · ${proofTasks.size}"))
            }
        }
    }

    val folderFilters = remember(folders, openTasks) {
        folders.map { f ->
            val folderOpen = openTasks.count { it.folderName.equals(f.name, ignoreCase = true) }
            val label = if (folderOpen > 0) "${f.name} · $folderOpen" else f.name
            FolderFilterChip(
                key = "folder:${f.name}",
                label = label,
                colorHex = f.colorHex,
                iconKey = f.iconKey
            )
        }
    }

    val effectiveFilter = remember(activeFilter, smartFilters, folderFilters) {
        val valid = smartFilters.any { it.key == activeFilter } || folderFilters.any { it.key == activeFilter }
        if (valid) activeFilter else "all"
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
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    HomeHeader(
                        nowMillis = nowBucketMillis,
                        isDarkTheme = isDarkTheme,
                        onOpenCalendar = onOpenCalendar,
                        onToggleTheme = onToggleTheme,
                        onOpenRecap = onOpenRecap
                    )

                    WeekStripRow(
                        tasks = tasks,
                        todayKey = todayKey,
                        onSelectDay = onOpenCalendar
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 1: Smart status filters
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(smartFilters, key = { it.key }) { chip ->
                            val selected = chip.key == effectiveFilter
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 96.dp)
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) colors.flash else colors.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) colors.flash else colors.border,
                                        shape = CircleShape
                                    )
                                    .clickable(role = Role.Tab) { activeFilter = chip.key }
                                    .padding(horizontal = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chip.label,
                                    color = if (selected) colors.onFlash else colors.text,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Row 2: Visually separated custom Folder chips
                    if (folderFilters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item(key = "folders_shortcut") {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.elevated)
                                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                        .clickable(role = Role.Button, onClick = onOpenFolders),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = SnapTaskIcons.Folder,
                                        contentDescription = "Quản lý thư mục",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            items(folderFilters, key = { it.key }) { chip ->
                                val selected = chip.key == effectiveFilter
                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) colors.text else colors.elevated)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) colors.text else colors.border,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(role = Role.Tab) {
                                            activeFilter = if (selected) "all" else chip.key
                                        }
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(chip.colorHex))
                                        )
                                        Icon(
                                            imageVector = SnapTaskIcons.folderIconByKey(chip.iconKey),
                                            contentDescription = null,
                                            tint = if (selected) colors.bg else colors.textSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = chip.label,
                                            color = if (selected) colors.bg else colors.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (isLoading) {
                item(key = "loading_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
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
                        isFirstRun = tasks.isEmpty(),
                        filterKey = effectiveFilter,
                        onClearFilter = { activeFilter = "all" },
                        onOpenCamera = onOpenCamera,
                        onOpenQuickNote = { showQuickNoteSheet = true }
                    )
                }
            } else {
                items(visibleOpenTasks, key = { "open_${it.id}" }) { task ->
                    val folderMatch = folderByName[task.folderName.lowercase(Locale.getDefault())]
                    val cadence = repeatByTaskId[task.id] ?: RepeatCadence.fromKeyOrTag(task.tag)
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                        if (task.hasImage) {
                            PhotoFeedTaskCard(
                                task = task,
                                folder = folderMatch,
                                repeatCadence = cadence,
                                nowMillis = nowBucketMillis,
                                isStamped = stampedTaskId == task.id,
                                stampScaleProvider = { if (stampedTaskId == task.id) stampScale.value else 0f },
                                onCardClick = { onTaskClick(task) },
                                onCompleteClick = { triggerDoneStamp(task) }
                            )
                        } else {
                            QuickNoteFeedCard(
                                task = task,
                                folder = folderMatch,
                                nowMillis = nowBucketMillis,
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
                                onReopen = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleTaskStatus(rowTasks[0])
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (rowTasks.size > 1) {
                                CompletedBentoTile(
                                    task = rowTasks[1],
                                    onClick = { onTaskClick(rowTasks[1]) },
                                    onReopen = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleTaskStatus(rowTasks[1])
                                    },
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
