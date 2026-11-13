package com.snaptask.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.snaptask.app.data.preferences.FolderConfig
import com.snaptask.app.data.preferences.RepeatCadence
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.ScreenState
import com.snaptask.app.ui.screens.board.BoardScreen
import com.snaptask.app.ui.screens.calendar.CalendarScreen
import com.snaptask.app.ui.screens.camera.CameraAfterProofScreen
import com.snaptask.app.ui.screens.camera.CameraCreatorScreen
import com.snaptask.app.ui.screens.detail.TaskDetailScreen
import com.snaptask.app.ui.screens.folders.FolderManagerScreen
import com.snaptask.app.ui.screens.heatmap.HeatmapScreen
import com.snaptask.app.ui.screens.home.HomeScreen
import com.snaptask.app.ui.screens.recap.WeeklyRecapScreen
import com.snaptask.app.ui.screens.share.ShareCardScreen
import com.snaptask.app.ui.theme.snapColors

private val OVERLAY_SCREENS = setOf(
    ScreenState.DETAIL,
    ScreenState.SHARE_CARD,
    ScreenState.WEEKLY_RECAP,
    ScreenState.CAMERA_AFTER_PROOF
)

private const val OVERLAY_FADE_MS = 140

@Composable
fun SnapTaskNavHost(
    currentScreen: ScreenState,
    isResolvingDeepLink: Boolean,
    tasks: List<Task>,
    isLoadingTasks: Boolean,
    selectedTask: Task?,
    folderConfig: FolderConfig,
    hasCameraPermission: Boolean,
    isDarkTheme: Boolean,
    calendarFocusMillis: Long,
    onRequestCameraPermission: () -> Unit,
    onSaveCameraTask: (Task, RepeatCadence) -> Unit,
    onSaveQuickNote: (Task) -> Unit,
    onQuickComplete: (Task) -> Unit,
    onToggleTaskStatus: (Task) -> Unit,
    onSaveTaskDetail: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onProofCaptured: (Task, String, String) -> Unit,
    onOpenTaskDetail: (Task, ScreenState) -> Unit,
    onOpenAfterProof: (Task, ScreenState) -> Unit,
    onOpenShareCard: (Task) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenCalendar: (Long) -> Unit,
    onOpenHeatmap: () -> Unit,
    onOpenRecap: () -> Unit,
    onOpenFolders: () -> Unit,
    onGoHome: () -> Unit,
    onBackFromDetail: () -> Unit,
    onBackFromAfterProof: () -> Unit,
    onCloseShareCard: () -> Unit,
    onInvalidTaskFallback: () -> Unit,
    onSelectDockTab: (DockTab) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleSmartUrgent: (Boolean) -> Unit,
    onToggleSmartProof: (Boolean) -> Unit,
    onMoveFolder: (Int, Int) -> Unit,
    onSaveFolder: (String?, String, String, Long, Boolean) -> Unit,
    onDeleteFolder: (String) -> Unit
) {
    if (isResolvingDeepLink) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.snapColors.bg)
        )
        return
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val useOverlayFade = targetState in OVERLAY_SCREENS || initialState in OVERLAY_SCREENS
            if (useOverlayFade) {
                fadeIn(animationSpec = tween(durationMillis = OVERLAY_FADE_MS)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = OVERLAY_FADE_MS))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "SnapTaskNavHost"
    ) { screen ->
        when (screen) {
            ScreenState.CAMERA_CREATOR -> {
                CameraCreatorScreen(
                    tasks = tasks,
                    folders = folderConfig.folders,
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = onRequestCameraPermission,
                    onSaveTask = onSaveCameraTask,
                    onSaveQuickNote = onSaveQuickNote,
                    onGoHome = onGoHome,
                    onOpenFolders = onOpenFolders
                )
            }

            ScreenState.HOME -> {
                HomeScreen(
                    tasks = tasks,
                    isLoading = isLoadingTasks,
                    folders = folderConfig.folders,
                    smartUrgentEnabled = folderConfig.smartUrgentEnabled,
                    smartProofEnabled = folderConfig.smartProofEnabled,
                    repeatByTaskId = folderConfig.repeatByTaskId,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onOpenCamera = onOpenCamera,
                    onOpenProofCamera = { task -> onOpenAfterProof(task, ScreenState.HOME) },
                    onQuickComplete = onQuickComplete,
                    onToggleTaskStatus = onToggleTaskStatus,
                    onTaskClick = { task -> onOpenTaskDetail(task, ScreenState.HOME) },
                    onSaveQuickNote = onSaveQuickNote,
                    onOpenCalendar = onOpenCalendar,
                    onOpenRecap = onOpenRecap,
                    onOpenFolders = onOpenFolders,
                    onSelectDockTab = onSelectDockTab
                )
            }

            ScreenState.CALENDAR -> {
                CalendarScreen(
                    tasks = tasks,
                    isLoading = isLoadingTasks,
                    initialSelectedDateMillis = calendarFocusMillis,
                    onOpenHeatmap = onOpenHeatmap,
                    onOpenCamera = onOpenCamera,
                    onTaskClick = { task -> onOpenTaskDetail(task, ScreenState.CALENDAR) },
                    onToggleTask = onToggleTaskStatus,
                    onSelectDockTab = onSelectDockTab
                )
            }

            ScreenState.HEATMAP -> {
                HeatmapScreen(
                    tasks = tasks,
                    isLoading = isLoadingTasks,
                    folders = folderConfig.folders,
                    onOpenCalendar = onOpenCalendar,
                    onOpenCamera = onOpenCamera,
                    onSelectDockTab = onSelectDockTab
                )
            }

            ScreenState.FOLDERS -> {
                FolderManagerScreen(
                    tasks = tasks,
                    folders = folderConfig.folders,
                    smartUrgentEnabled = folderConfig.smartUrgentEnabled,
                    smartProofEnabled = folderConfig.smartProofEnabled,
                    onToggleSmartUrgent = onToggleSmartUrgent,
                    onToggleSmartProof = onToggleSmartProof,
                    onMoveFolder = onMoveFolder,
                    onSaveFolder = onSaveFolder,
                    onDeleteFolder = onDeleteFolder,
                    onOpenCamera = onOpenCamera,
                    onSelectDockTab = onSelectDockTab
                )
            }

            ScreenState.BOARD -> {
                BoardScreen(
                    tasks = tasks,
                    isLoading = isLoadingTasks,
                    folders = folderConfig.folders,
                    onTaskClick = { task -> onOpenTaskDetail(task, ScreenState.BOARD) },
                    onToggleTaskStatus = onToggleTaskStatus,
                    onOpenCamera = onOpenCamera,
                    onOpenFolders = onOpenFolders,
                    onSelectDockTab = onSelectDockTab
                )
            }

            ScreenState.WEEKLY_RECAP -> {
                WeeklyRecapScreen(
                    tasks = tasks,
                    onBack = onGoHome,
                    onOpenHeatmap = onOpenHeatmap,
                    onTaskClick = { task -> onOpenTaskDetail(task, ScreenState.WEEKLY_RECAP) }
                )
            }

            ScreenState.CAMERA_AFTER_PROOF -> {
                when {
                    isLoadingTasks -> Box(modifier = Modifier.fillMaxSize())
                    selectedTask != null -> {
                        CameraAfterProofScreen(
                            task = selectedTask,
                            onProofCaptured = { afterUri, caption ->
                                onProofCaptured(selectedTask, afterUri, caption)
                            },
                            onClose = onBackFromAfterProof
                        )
                    }
                    screen == currentScreen -> {
                        LaunchedEffect(Unit) { onInvalidTaskFallback() }
                    }
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }

            ScreenState.DETAIL -> {
                when {
                    isLoadingTasks -> Box(modifier = Modifier.fillMaxSize())
                    selectedTask != null -> {
                        TaskDetailScreen(
                            task = selectedTask,
                            folders = folderConfig.folders,
                            onBack = onBackFromDetail,
                            onSaveTask = onSaveTaskDetail,
                            onDeleteTask = onDeleteTask,
                            onOpenAfterCamera = { targetTask ->
                                onOpenAfterProof(targetTask, ScreenState.DETAIL)
                            },
                            onOpenFolders = onOpenFolders,
                            onOpenShareCard = onOpenShareCard
                        )
                    }
                    screen == currentScreen -> {
                        LaunchedEffect(Unit) { onInvalidTaskFallback() }
                    }
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }

            ScreenState.SHARE_CARD -> {
                when {
                    isLoadingTasks -> Box(modifier = Modifier.fillMaxSize())
                    selectedTask != null -> {
                        ShareCardScreen(
                            task = selectedTask,
                            onClose = onCloseShareCard
                        )
                    }
                    screen == currentScreen -> {
                        LaunchedEffect(Unit) { onInvalidTaskFallback() }
                    }
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
