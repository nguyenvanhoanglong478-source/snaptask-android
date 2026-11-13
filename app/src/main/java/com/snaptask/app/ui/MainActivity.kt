package com.snaptask.app.ui

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.snaptask.app.SnapTaskApplication
import com.snaptask.app.data.preferences.FolderPreferencesRepository
import com.snaptask.app.data.preferences.RepeatCadence
import com.snaptask.app.domain.model.Task
import com.snaptask.app.reminder.TaskReminderScheduler
import com.snaptask.app.ui.components.DockTab
import com.snaptask.app.ui.components.SnapTaskNavHost
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import com.snaptask.app.widget.SnapTaskWidgetUpdater
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractTaskId(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingTaskIdState.value?.let { outState.putLong(STATE_PENDING_TASK_ID, it) }
    }

    private fun extractTaskId(intent: Intent?) {
        val i = intent ?: return
        val taskId = i.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
            .takeIf { it > 0L } ?: return
        pendingTaskIdState.value = taskId
        i.removeExtra(TaskReminderScheduler.EXTRA_TASK_ID)
    }

    private fun applyEdgeToEdgeBars(darkBars: Boolean) {
        val lightNavScrim = AndroidColor.argb(0x66, 0x0F, 0x0F, 0x0E)
        enableEdgeToEdge(
            statusBarStyle = if (darkBars) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, lightNavScrim)
            },
            navigationBarStyle = if (darkBars) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, lightNavScrim)
            }
        )
    }

    private fun buildReminderNotice(task: Task): String {
        val offset = DateTimeUtils.formatReminderOffsetLabel(task.reminderTime, task.hasReminder)
        return if (offset.isNotEmpty()) "\nĐã đặt nhắc nhở $offset" else ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            extractTaskId(intent)
        } else if (savedInstanceState.containsKey(STATE_PENDING_TASK_ID)) {
            pendingTaskIdState.value = savedInstanceState.getLong(STATE_PENDING_TASK_ID)
        }

        val prefs = getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
        val initialThemeMode = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"
        val initialSystemDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val initialIsDark = when (initialThemeMode) {
            "LIGHT" -> false
            "DARK" -> true
            else -> initialSystemDark
        }
        val initialDarkBars = initialIsDark || pendingTaskIdState.value == null
        applyEdgeToEdgeBars(darkBars = initialDarkBars)

        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        lifecycleScope.launch {
            delay(250)
            keepSplashScreen = false
        }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView
            val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.18f)
            val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.18f)
            val alpha = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 160
                interpolator = AccelerateDecelerateInterpolator()
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }

        val app = application as SnapTaskApplication
        val repository = app.taskRepository
        val folderPrefs = app.folderPreferences

        setContent {
            val themePrefs = remember { getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE) }
            var themeMode by rememberSaveable {
                mutableStateOf(themePrefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK")
            }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }

            var currentScreen by rememberSaveable { mutableStateOf(ScreenState.CAMERA_CREATOR) }
            var detailOriginScreen by rememberSaveable { mutableStateOf(ScreenState.HOME) }
            var selectedTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
            var calendarFocusMillis by rememberSaveable {
                mutableLongStateOf(System.currentTimeMillis())
            }
            var hasPromptedNotifications by rememberSaveable {
                mutableStateOf(themePrefs.getBoolean(KEY_NOTIF_PROMPTED, false))
            }
            var hasPromptedOnReminderThisSession by rememberSaveable { mutableStateOf(false) }

            val effectiveDarkBars = isDark ||
                currentScreen == ScreenState.CAMERA_CREATOR ||
                currentScreen == ScreenState.CAMERA_AFTER_PROOF

            DisposableEffect(effectiveDarkBars) {
                applyEdgeToEdgeBars(darkBars = effectiveDarkBars)
                onDispose {}
            }

            SnapTaskTheme(
                darkTheme = isDark,
                darkSystemBars = effectiveDarkBars,
                manageSystemBars = false
            ) {
                val onToggleTheme: () -> Unit = {
                    val nextMode = if (isDark) "LIGHT" else "DARK"
                    themeMode = nextMode
                    themePrefs.edit().putString(KEY_THEME_MODE, nextMode).apply()
                    val modeName = if (nextMode == "LIGHT") "Sáng (Snap Pop)" else "Tối (Snap Pop)"
                    Toast.makeText(this@MainActivity, "Giao diện: $modeName", Toast.LENGTH_SHORT).show()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.snapColors.bg
                ) {
                    val tasksFlow = remember { repository.getAllTasks() }
                    val tasksState = tasksFlow.collectAsState(initial = null)
                    val tasks = tasksState.value
                    val isLoadingTasks = tasks == null
                    val displayTasks = tasks ?: emptyList()
                    val selectedTask = displayTasks.find { it.id == selectedTaskId }

                    val folderConfig by folderPrefs.configFlow.collectAsState()
                    val scope = rememberCoroutineScope()
                    val lifecycleOwner = LocalLifecycleOwner.current

                    LaunchedEffect(isLoadingTasks) {
                        if (!isLoadingTasks) {
                            keepSplashScreen = false
                        }
                    }

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

                    LaunchedEffect(currentScreen) {
                        if (currentScreen !in setOf(
                                ScreenState.DETAIL,
                                ScreenState.SHARE_CARD,
                                ScreenState.CAMERA_AFTER_PROOF
                            )
                        ) {
                            delay(160)
                            selectedTaskId = null
                        }
                    }

                    BackHandler(enabled = currentScreen != ScreenState.HOME) {
                        when (currentScreen) {
                            ScreenState.SHARE_CARD -> currentScreen = ScreenState.DETAIL
                            ScreenState.DETAIL, ScreenState.CAMERA_AFTER_PROOF -> {
                                currentScreen = detailOriginScreen
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
                            selectedTaskId = null
                            currentScreen = ScreenState.HOME
                        }
                        pendingTaskIdState.value = null
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

                    fun requestNotificationPermissionIfNeeded(fromReminder: Boolean) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        val alreadyPrompted = if (fromReminder) {
                            hasPromptedOnReminderThisSession
                        } else {
                            hasPromptedNotifications
                        }
                        if (alreadyPrompted) return

                        hasPromptedNotifications = true
                        themePrefs.edit().putBoolean(KEY_NOTIF_PROMPTED, true).apply()
                        if (fromReminder) {
                            hasPromptedOnReminderThisSession = true
                        }
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    LaunchedEffect(currentScreen, pendingTaskIdState.value) {
                        if (currentScreen == ScreenState.HOME && pendingTaskIdState.value == null) {
                            requestNotificationPermissionIfNeeded(fromReminder = false)
                        }
                    }

                    var pendingCameraTarget by rememberSaveable { mutableStateOf(ScreenState.CAMERA_CREATOR) }
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

                    SnapTaskNavHost(
                        currentScreen = currentScreen,
                        isResolvingDeepLink = pendingTaskIdState.value != null,
                        tasks = displayTasks,
                        isLoadingTasks = isLoadingTasks,
                        selectedTask = selectedTask,
                        folderConfig = folderConfig,
                        hasCameraPermission = hasCameraPermission,
                        isDarkTheme = isDark,
                        calendarFocusMillis = calendarFocusMillis,
                        onRequestCameraPermission = {
                            pendingCameraTarget = ScreenState.CAMERA_CREATOR
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onSaveCameraTask = { newTask, cadence ->
                            if (newTask.hasReminder) {
                                requestNotificationPermissionIfNeeded(fromReminder = true)
                            }
                            scope.launch {
                                val createdId = repository.createTask(newTask)
                                if (cadence != RepeatCadence.NONE) {
                                    folderPrefs.setTaskRepeat(createdId, cadence)
                                    SnapTaskWidgetUpdater.requestUpdate(this@MainActivity)
                                }
                                currentScreen = ScreenState.HOME
                                val reminderNotice = buildReminderNotice(newTask)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Đã lưu việc cần làm!$reminderNotice",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onSaveQuickNote = { noteTask ->
                            if (noteTask.hasReminder) {
                                requestNotificationPermissionIfNeeded(fromReminder = true)
                            }
                            scope.launch {
                                repository.createTask(noteTask)
                                currentScreen = ScreenState.HOME
                                val reminderNotice = buildReminderNotice(noteTask)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Đã lưu ghi chú!$reminderNotice",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onQuickComplete = { task ->
                            scope.launch {
                                repository.completeTaskWithProof(
                                    taskId = task.id,
                                    afterImageUri = null,
                                    completionCaption = null,
                                    completedAt = System.currentTimeMillis()
                                )
                                Toast.makeText(
                                    this@MainActivity,
                                    "Đã hoàn thành!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onToggleTaskStatus = { task ->
                            scope.launch {
                                repository.toggleTaskStatus(task.id)
                            }
                        },
                        onSaveTaskDetail = { updated ->
                            if (updated.hasReminder) {
                                requestNotificationPermissionIfNeeded(fromReminder = true)
                            }
                            scope.launch {
                                repository.updateTask(updated)
                                val reminderNotice = buildReminderNotice(updated)
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
                                currentScreen = detailOriginScreen
                                selectedTaskId = null
                                Toast.makeText(
                                    this@MainActivity,
                                    "Đã xóa việc cần làm!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onProofCaptured = { targetTask, afterUri, caption ->
                            scope.launch {
                                repository.completeTaskWithProof(
                                    taskId = targetTask.id,
                                    afterImageUri = afterUri,
                                    completionCaption = caption,
                                    completedAt = System.currentTimeMillis()
                                )
                                currentScreen = ScreenState.DETAIL
                                Toast.makeText(
                                    this@MainActivity,
                                    "Đã lưu ảnh kết quả Trước / Sau!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onOpenTaskDetail = ::openTaskDetail,
                        onOpenAfterProof = ::openAfterProofWithCheck,
                        onOpenShareCard = { latestSnapshot ->
                            scope.launch {
                                if (latestSnapshot != selectedTask) {
                                    repository.updateTask(latestSnapshot)
                                }
                                selectedTaskId = latestSnapshot.id
                                currentScreen = ScreenState.SHARE_CARD
                            }
                        },
                        onOpenCamera = { currentScreen = ScreenState.CAMERA_CREATOR },
                        onOpenCalendar = { focusDate ->
                            calendarFocusMillis = focusDate
                            currentScreen = ScreenState.CALENDAR
                        },
                        onOpenHeatmap = { currentScreen = ScreenState.HEATMAP },
                        onOpenRecap = { currentScreen = ScreenState.WEEKLY_RECAP },
                        onOpenFolders = { currentScreen = ScreenState.FOLDERS },
                        onGoHome = { currentScreen = ScreenState.HOME },
                        onBackFromDetail = { currentScreen = detailOriginScreen },
                        onBackFromAfterProof = { currentScreen = detailOriginScreen },
                        onCloseShareCard = { currentScreen = ScreenState.DETAIL },
                        onInvalidTaskFallback = {
                            currentScreen = ScreenState.HOME
                            selectedTaskId = null
                        },
                        onSelectDockTab = ::handleDockSelect,
                        onToggleTheme = onToggleTheme,
                        onToggleSmartUrgent = {
                            folderPrefs.setSmartUrgentEnabled(it)
                            SnapTaskWidgetUpdater.requestUpdate(this@MainActivity)
                        },
                        onToggleSmartProof = {
                            folderPrefs.setSmartProofEnabled(it)
                            SnapTaskWidgetUpdater.requestUpdate(this@MainActivity)
                        },
                        onMoveFolder = { idx, delta ->
                            folderPrefs.moveFolder(idx, delta)
                            SnapTaskWidgetUpdater.requestUpdate(this@MainActivity)
                        },
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
                                }
                            } else {
                                SnapTaskWidgetUpdater.requestUpdate(this@MainActivity)
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
                                }
                            } else {
                                SnapTaskWidgetUpdater.requestUpdate(this@MainActivity)
                            }
                        }
                    )
                }
            }
        }
    }

    private companion object {
        private const val THEME_PREFS_NAME = "snaptask_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_NOTIF_PROMPTED = "notif_permission_prompted"
        private const val STATE_PENDING_TASK_ID = "state_pending_task_id"
    }
}
