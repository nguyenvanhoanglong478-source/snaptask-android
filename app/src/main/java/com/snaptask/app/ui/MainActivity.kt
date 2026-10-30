package com.snaptask.app.ui

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.snaptask.app.SnapTaskApplication
import com.snaptask.app.data.preferences.FolderPreferencesRepository
import com.snaptask.app.data.preferences.RepeatCadence
import com.snaptask.app.domain.model.Task
import com.snaptask.app.reminder.AlarmSoundPlayer
import com.snaptask.app.reminder.TaskReminderScheduler
import com.snaptask.app.ui.components.DockTab
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
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.util.NotificationPermissionHelper
import com.snaptask.app.widget.LocketFocusWidget
import com.snaptask.app.widget.NotionStackWidget
import com.snaptask.app.widget.SnapTaskWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScreenState {
    CAMERA_CREATOR,
    HOME,
    CALENDAR,
    HEATMAP,
    FOLDERS,
    BOARD,
    DETAIL,
    CAMERA_AFTER_PROOF,
    WEEKLY_RECAP,
    SHARE_CARD
}

class MainActivity : ComponentActivity() {

    private val pendingTaskIdState = mutableStateOf<Long?>(null)

    override fun onResume() {
        super.onResume()
        AlarmSoundPlayer.stop()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        AlarmSoundPlayer.stop()
        extractTaskId(intent)
    }

    private fun extractTaskId(intent: Intent?) {
        val i = intent ?: return
        val taskId = i.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
            .takeIf { it != -1L } ?: return
        pendingTaskIdState.value = taskId
        i.removeExtra(TaskReminderScheduler.EXTRA_TASK_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            extractTaskId(intent)
        }

        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        lifecycleScope.launch {
            delay(500)
            keepSplashScreen = false
        }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView
            val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.25f)
            val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.25f)
            val alpha = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 240
                interpolator = AccelerateDecelerateInterpolator()
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }

        val app = application as SnapTaskApplication
        val repository = app.taskRepository
        val folderPrefs = app.folderPreferences

        setContent {
            val prefs = remember { getSharedPreferences("snaptask_theme_prefs", Context.MODE_PRIVATE) }
            var themeMode by rememberSaveable {
                mutableStateOf(prefs.getString("theme_mode", "DARK") ?: "DARK")
            }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }

            SnapTaskTheme(darkTheme = isDark) {
                val onToggleTheme: () -> Unit = {
                    val nextMode = if (isDark) "LIGHT" else "DARK"
                    themeMode = nextMode
                    prefs.edit().putString("theme_mode", nextMode).apply()
                    val modeName = if (nextMode == "LIGHT") "Sáng (Snap Pop)" else "Tối (Snap Pop)"
                    Toast.makeText(this@MainActivity, "Giao diện: $modeName", Toast.LENGTH_SHORT).show()
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by rememberSaveable { mutableStateOf(ScreenState.CAMERA_CREATOR) }
                    var detailOriginScreen by rememberSaveable { mutableStateOf(ScreenState.HOME) }
                    var selectedTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
                    var calendarFocusMillis by rememberSaveable {
                        mutableLongStateOf(System.currentTimeMillis())
                    }

                    val tasksFlow = remember { repository.getAllTasks() }
                    val tasksState = tasksFlow.collectAsState(initial = null)
                    val tasks = tasksState.value
                    val isLoadingTasks = tasks == null
                    val displayTasks = tasks ?: emptyList()
                    val selectedTask = displayTasks.find { it.id == selectedTaskId }

                    val folderConfig by folderPrefs.configFlow.collectAsState()
                    val scope = rememberCoroutineScope()
                    val lifecycleOwner = LocalLifecycleOwner.current

                    var hasCameraPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                hasCameraPermission = ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    LaunchedEffect(tasks?.size) {
                        val loaded = tasks ?: return@LaunchedEffect
                        if (loaded.isNotEmpty()) {
                            folderPrefs.syncDiscoveredFolders(loaded.map { it.folderName })
                        }
                    }

                    BackHandler(enabled = currentScreen != ScreenState.HOME) {
                        when (currentScreen) {
                            ScreenState.SHARE_CARD -> currentScreen = ScreenState.DETAIL
                            ScreenState.DETAIL -> {
                                currentScreen = detailOriginScreen
                                selectedTaskId = null
                            }
                            ScreenState.CAMERA_AFTER_PROOF -> {
                                currentScreen = detailOriginScreen
                                selectedTaskId = null
                            }
                            ScreenState.HEATMAP -> currentScreen = ScreenState.CALENDAR
                            else -> {
                                currentScreen = ScreenState.HOME
                                selectedTaskId = null
                            }
                        }
                    }

                    LaunchedEffect(tasks, pendingTaskIdState.value) {
                        val targetId = pendingTaskIdState.value ?: return@LaunchedEffect
                        val loadedTasks = tasks ?: return@LaunchedEffect
                        val found = loadedTasks.find { it.id == targetId }
                        if (found != null) {
                            selectedTaskId = targetId
                            detailOriginScreen = ScreenState.HOME
                            currentScreen = ScreenState.DETAIL
                        } else {
                            currentScreen = ScreenState.HOME
                        }
                        pendingTaskIdState.value = null
                    }

                    fun updateWidgets() {
                        scope.launch {
                            try {
                                SnapTaskWidget().updateAll(this@MainActivity)
                                LocketFocusWidget().updateAll(this@MainActivity)
                                NotionStackWidget().updateAll(this@MainActivity)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    val notifPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (!isGranted) {
                            Toast.makeText(
                                this@MainActivity,
                                "Thông báo đang tắt. Bạn có thể bật lại trong Cài đặt.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    LaunchedEffect(Unit) {
                        AlarmSoundPlayer.stop()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (!NotificationPermissionHelper.areNotificationsEnabled(this@MainActivity)) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    fun formatReminderConfirmation(task: Task): String {
                        val rem = task.reminderTime ?: return ""
                        if (!task.hasReminder) return ""
                        val diffMs = rem - System.currentTimeMillis()
                        val diffSec = diffMs / 1000
                        val diffMin = diffSec / 60
                        return when {
                            diffSec in 0..10 -> "\nĐã đặt nhắc nhở sau ${diffSec.coerceAtLeast(1)} giây"
                            diffMin < 60 -> "\nĐã đặt nhắc nhở sau $diffMin phút nữa"
                            else -> {
                                val hours = diffMin / 60
                                val remMin = diffMin % 60
                                if (remMin > 0) "\nĐã đặt nhắc nhở sau ${hours}h ${remMin}m nữa"
                                else "\nĐã đặt nhắc nhở sau $hours giờ nữa"
                            }
                        }
                    }

                    var pendingCameraTarget by remember { mutableStateOf(ScreenState.CAMERA_CREATOR) }
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        hasCameraPermission = isGranted
                        if (isGranted) {
                            currentScreen = pendingCameraTarget
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Cần cấp quyền máy ảnh để chụp ảnh việc cần làm",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    fun openAfterProofWithCheck(task: Task, origin: ScreenState) {
                        selectedTaskId = task.id
                        detailOriginScreen = origin
                        pendingCameraTarget = ScreenState.CAMERA_AFTER_PROOF
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        hasCameraPermission = granted
                        if (granted) {
                            currentScreen = ScreenState.CAMERA_AFTER_PROOF
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }

                    fun openTaskDetail(task: Task, origin: ScreenState) {
                        selectedTaskId = task.id
                        detailOriginScreen = origin
                        currentScreen = ScreenState.DETAIL
                    }

                    fun handleDockSelect(tab: DockTab) {
                        selectedTaskId = null
                        currentScreen = when (tab) {
                            DockTab.TODAY -> ScreenState.HOME
                            DockTab.CALENDAR -> ScreenState.CALENDAR
                            DockTab.BOARD -> ScreenState.BOARD
                            DockTab.FOLDERS -> ScreenState.FOLDERS
                        }
                    }

                    when (currentScreen) {
                        ScreenState.CAMERA_CREATOR -> {
                            CameraCreatorScreen(
                                tasks = displayTasks,
                                folders = folderConfig.folders,
                                hasCameraPermission = hasCameraPermission,
                                onRequestCameraPermission = {
                                    pendingCameraTarget = ScreenState.CAMERA_CREATOR
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                onSaveTask = { newTask, cadence ->
                                    scope.launch {
                                        val createdId = repository.createTask(newTask)
                                        if (cadence != RepeatCadence.NONE) {
                                            folderPrefs.setTaskRepeat(createdId, cadence)
                                        }
                                        updateWidgets()
                                        currentScreen = ScreenState.HOME
                                        val reminderNotice = formatReminderConfirmation(newTask)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Đã lưu việc cần làm!$reminderNotice",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onSaveQuickNote = { noteTask ->
                                    scope.launch {
                                        repository.createTask(noteTask)
                                        updateWidgets()
                                        currentScreen = ScreenState.HOME
                                        val reminderNotice = formatReminderConfirmation(noteTask)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Đã lưu ghi chú!$reminderNotice",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onGoHome = {
                                    currentScreen = ScreenState.HOME
                                },
                                onOpenFolders = {
                                    currentScreen = ScreenState.FOLDERS
                                }
                            )
                        }

                        ScreenState.HOME -> {
                            HomeScreen(
                                tasks = displayTasks,
                                isLoading = isLoadingTasks,
                                folders = folderConfig.folders,
                                smartUrgentEnabled = folderConfig.smartUrgentEnabled,
                                smartProofEnabled = folderConfig.smartProofEnabled,
                                repeatByTaskId = folderConfig.repeatByTaskId,
                                isDarkTheme = isDark,
                                onToggleTheme = onToggleTheme,
                                onOpenCamera = { currentScreen = ScreenState.CAMERA_CREATOR },
                                onOpenProofCamera = { task ->
                                    openAfterProofWithCheck(task, origin = ScreenState.HOME)
                                },
                                onQuickComplete = { task ->
                                    scope.launch {
                                        repository.completeTaskWithProof(
                                            taskId = task.id,
                                            afterImageUri = null,
                                            completionCaption = null,
                                            completedAt = System.currentTimeMillis()
                                        )
                                        updateWidgets()
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Đã hoàn thành!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onToggleTaskStatus = { task ->
                                    scope.launch {
                                        repository.toggleTaskStatus(task.id, task.status)
                                        updateWidgets()
                                    }
                                },
                                onTaskClick = { task ->
                                    openTaskDetail(task, origin = ScreenState.HOME)
                                },
                                onSaveQuickNote = { newTask ->
                                    scope.launch {
                                        repository.createTask(newTask)
                                        updateWidgets()
                                        val reminderNotice = formatReminderConfirmation(newTask)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Đã lưu ghi chú!$reminderNotice",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onOpenCalendar = { focusDate ->
                                    calendarFocusMillis = focusDate
                                    currentScreen = ScreenState.CALENDAR
                                },
                                onOpenRecap = {
                                    currentScreen = ScreenState.WEEKLY_RECAP
                                },
                                onOpenFolders = {
                                    currentScreen = ScreenState.FOLDERS
                                },
                                onSelectDockTab = ::handleDockSelect
                            )
                        }

                        ScreenState.CALENDAR -> {
                            CalendarScreen(
                                tasks = displayTasks,
                                isLoading = isLoadingTasks,
                                initialSelectedDateMillis = calendarFocusMillis,
                                onOpenHeatmap = { currentScreen = ScreenState.HEATMAP },
                                onOpenCamera = { currentScreen = ScreenState.CAMERA_CREATOR },
                                onTaskClick = { task ->
                                    openTaskDetail(task, origin = ScreenState.CALENDAR)
                                },
                                onToggleTask = { task ->
                                    scope.launch {
                                        repository.toggleTaskStatus(task.id, task.status)
                                        updateWidgets()
                                    }
                                },
                                onSelectDockTab = ::handleDockSelect
                            )
                        }

                        ScreenState.HEATMAP -> {
                            HeatmapScreen(
                                tasks = displayTasks,
                                isLoading = isLoadingTasks,
                                folders = folderConfig.folders,
                                onOpenCalendar = { focusDate ->
                                    calendarFocusMillis = focusDate
                                    currentScreen = ScreenState.CALENDAR
                                },
                                onOpenCamera = { currentScreen = ScreenState.CAMERA_CREATOR },
                                onSelectDockTab = ::handleDockSelect
                            )
                        }

                        ScreenState.FOLDERS -> {
                            FolderManagerScreen(
                                tasks = displayTasks,
                                folders = folderConfig.folders,
                                smartUrgentEnabled = folderConfig.smartUrgentEnabled,
                                smartProofEnabled = folderConfig.smartProofEnabled,
                                onToggleSmartUrgent = { folderPrefs.setSmartUrgentEnabled(it) },
                                onToggleSmartProof = { folderPrefs.setSmartProofEnabled(it) },
                                onMoveFolder = { idx, delta -> folderPrefs.moveFolder(idx, delta) },
                                onSaveFolder = { editingId, name, iconKey, colorHex, makeDefault ->
                                    val (oldName, newName) = folderPrefs.saveFolder(
                                        editingId = editingId,
                                        rawName = name,
                                        iconKey = iconKey,
                                        colorHex = colorHex,
                                        makeDefault = makeDefault
                                    )
                                    if (oldName != null) {
                                        scope.launch {
                                            repository.renameFolderTasks(oldName, newName)
                                            updateWidgets()
                                        }
                                    }
                                },
                                onDeleteFolder = { id ->
                                    val removedName = folderPrefs.deleteFolder(id)
                                    if (removedName != null) {
                                        scope.launch {
                                            repository.renameFolderTasks(
                                                removedName,
                                                FolderPreferencesRepository.UNASSIGNED_FOLDER_NAME
                                            )
                                            updateWidgets()
                                        }
                                    }
                                },
                                onOpenCamera = { currentScreen = ScreenState.CAMERA_CREATOR },
                                onSelectDockTab = ::handleDockSelect
                            )
                        }

                        ScreenState.BOARD -> {
                            BoardScreen(
                                tasks = displayTasks,
                                isLoading = isLoadingTasks,
                                folders = folderConfig.folders,
                                onTaskClick = { task ->
                                    openTaskDetail(task, origin = ScreenState.BOARD)
                                },
                                onToggleTaskStatus = { task ->
                                    scope.launch {
                                        repository.toggleTaskStatus(task.id, task.status)
                                        updateWidgets()
                                    }
                                },
                                onOpenCamera = { currentScreen = ScreenState.CAMERA_CREATOR },
                                onOpenFolders = { currentScreen = ScreenState.FOLDERS },
                                onSelectDockTab = ::handleDockSelect
                            )
                        }

                        ScreenState.WEEKLY_RECAP -> {
                            WeeklyRecapScreen(
                                tasks = displayTasks,
                                onBack = { currentScreen = ScreenState.HOME },
                                onOpenHeatmap = { currentScreen = ScreenState.HEATMAP },
                                onTaskClick = { task ->
                                    openTaskDetail(task, origin = ScreenState.WEEKLY_RECAP)
                                }
                            )
                        }

                        ScreenState.CAMERA_AFTER_PROOF -> {
                            if (tasks == null) {
                                Box(modifier = Modifier.fillMaxSize())
                            } else if (selectedTask != null) {
                                CameraAfterProofScreen(
                                    task = selectedTask,
                                    onProofCaptured = { afterUri, caption ->
                                        scope.launch {
                                            repository.completeTaskWithProof(
                                                taskId = selectedTask.id,
                                                afterImageUri = afterUri,
                                                completionCaption = caption,
                                                completedAt = System.currentTimeMillis()
                                            )
                                            updateWidgets()
                                            currentScreen = ScreenState.DETAIL
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Đã lưu ảnh kết quả Trước / Sau!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onClose = {
                                        currentScreen = detailOriginScreen
                                        selectedTaskId = null
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    currentScreen = ScreenState.HOME
                                    selectedTaskId = null
                                }
                            }
                        }

                        ScreenState.DETAIL -> {
                            if (tasks == null) {
                                Box(modifier = Modifier.fillMaxSize())
                            } else if (selectedTask != null) {
                                TaskDetailScreen(
                                    task = selectedTask,
                                    folders = folderConfig.folders,
                                    onBack = {
                                        currentScreen = detailOriginScreen
                                        selectedTaskId = null
                                    },
                                    onSaveTask = { updated ->
                                        scope.launch {
                                            repository.updateTask(updated)
                                            updateWidgets()
                                            val reminderNotice = formatReminderConfirmation(updated)
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Đã cập nhật việc cần làm!$reminderNotice",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onDeleteTask = { toDelete ->
                                        scope.launch {
                                            repository.deleteTask(toDelete)
                                            updateWidgets()
                                            selectedTaskId = null
                                            currentScreen = detailOriginScreen
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Đã xóa việc cần làm!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onOpenAfterCamera = { targetTask ->
                                        openAfterProofWithCheck(targetTask, origin = ScreenState.DETAIL)
                                    },
                                    onOpenFolders = {
                                        currentScreen = ScreenState.FOLDERS
                                    },
                                    onOpenShareCard = { latestSnapshot ->
                                        scope.launch {
                                            repository.updateTask(latestSnapshot)
                                            selectedTaskId = latestSnapshot.id
                                            currentScreen = ScreenState.SHARE_CARD
                                        }
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    currentScreen = ScreenState.HOME
                                    selectedTaskId = null
                                }
                            }
                        }

                        ScreenState.SHARE_CARD -> {
                            if (tasks == null) {
                                Box(modifier = Modifier.fillMaxSize())
                            } else if (selectedTask != null) {
                                ShareCardScreen(
                                    task = selectedTask,
                                    onClose = { currentScreen = ScreenState.DETAIL }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    currentScreen = ScreenState.HOME
                                    selectedTaskId = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
