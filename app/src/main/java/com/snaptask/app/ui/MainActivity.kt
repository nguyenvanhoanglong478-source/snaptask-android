package com.snaptask.app.ui

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.snaptask.app.SnapTaskApplication
import com.snaptask.app.domain.model.Task
import com.snaptask.app.reminder.AlarmSoundPlayer
import com.snaptask.app.reminder.TaskReminderScheduler
import com.snaptask.app.ui.screens.board.BoardScreen
import com.snaptask.app.ui.screens.camera.CameraAfterProofScreen
import com.snaptask.app.ui.screens.camera.CameraCreatorScreen
import com.snaptask.app.ui.screens.detail.TaskDetailScreen
import com.snaptask.app.ui.screens.home.HomeScreen
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.util.NotificationPermissionHelper
import com.snaptask.app.widget.LocketFocusWidget
import com.snaptask.app.widget.NotionStackWidget
import com.snaptask.app.widget.SnapTaskWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    CAMERA_CREATOR,
    CAMERA_AFTER_PROOF,
    DETAIL,
    BOARD
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
        val taskId = intent?.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)?.takeIf { it != -1L }
        if (taskId != null) {
            pendingTaskIdState.value = taskId
            intent.removeExtra(TaskReminderScheduler.EXTRA_TASK_ID)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        extractTaskId(intent)

        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        lifecycleScope.launch {
            delay(550)
            keepSplashScreen = false
        }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView
            val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.3f)
            val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.3f)
            val alpha = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 250
                interpolator = AccelerateDecelerateInterpolator()
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }

        val app = application as SnapTaskApplication
        val repository = app.taskRepository

        setContent {
            SnapTaskTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by rememberSaveable { mutableStateOf(ScreenState.HOME) }
                    var selectedTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
                    val tasksState = repository.getAllTasks().collectAsState(initial = null)
                    val tasks = tasksState.value
                    val displayTasks = tasks ?: emptyList()
                    val selectedTask = displayTasks.find { it.id == selectedTaskId }
                    val scope = rememberCoroutineScope()

                    // BackHandler: Return to HOME from other screens, default behavior on HOME
                    BackHandler(enabled = currentScreen != ScreenState.HOME) {
                        currentScreen = ScreenState.HOME
                        selectedTaskId = null
                    }

                    // Deep link from notification: navigate to DETAIL of the task once tasks are loaded
                    LaunchedEffect(tasks, pendingTaskIdState.value) {
                        val targetId = pendingTaskIdState.value ?: return@LaunchedEffect
                        val loadedTasks = tasks ?: return@LaunchedEffect
                        val found = loadedTasks.find { it.id == targetId }
                        if (found != null) {
                            selectedTaskId = targetId
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
                            } catch (_: Exception) {}
                        }
                    }

                    // Notification permission launcher for Android 13+ (API 33+)
                    val notifPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (!isGranted) {
                            Toast.makeText(this@MainActivity, "Thông báo đang tắt. Bạn có thể bật lại trong Cài đặt.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Prompt for notification permission on app launch if not enabled
                    LaunchedEffect(Unit) {
                        AlarmSoundPlayer.stop()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (!NotificationPermissionHelper.areNotificationsEnabled(this@MainActivity)) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    fun formatReminderConfirmation(task: Task): String {
                        if (!task.hasReminder || task.reminderTime == null) return ""
                        val diffMs = task.reminderTime!! - System.currentTimeMillis()
                        val diffSec = diffMs / 1000
                        val diffMin = diffSec / 60
                        return when {
                            diffSec in 0..10 -> "\nĐã đặt nhắc nhở sau ${diffSec.coerceAtLeast(1)} giây"
                            diffMin < 60 -> "\nĐã đặt nhắc nhở sau ${diffMin} phút nữa"
                            else -> {
                                val hours = diffMin / 60
                                val remMin = diffMin % 60
                                if (remMin > 0) "\nĐã đặt nhắc nhở sau ${hours}h ${remMin}m nữa"
                                else "\nĐã đặt nhắc nhở sau ${hours} giờ nữa"
                            }
                        }
                    }

                    // Camera permission launcher
                    var pendingCameraTarget by remember { mutableStateOf(ScreenState.CAMERA_CREATOR) }
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            currentScreen = pendingCameraTarget
                        } else {
                            Toast.makeText(this, "Cần cấp quyền máy ảnh để chụp ghi chú", Toast.LENGTH_SHORT).show()
                        }
                    }

                    fun openCameraWithCheck(target: ScreenState = ScreenState.CAMERA_CREATOR) {
                        pendingCameraTarget = target
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            currentScreen = target
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }

                    when (currentScreen) {
                        ScreenState.HOME -> {
                            HomeScreen(
                                tasks = displayTasks,
                                onOpenLiveCamera = { openCameraWithCheck(ScreenState.CAMERA_CREATOR) },
                                onOpenProofCamera = { task ->
                                    selectedTaskId = task.id
                                    openCameraWithCheck(ScreenState.CAMERA_AFTER_PROOF)
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
                                        Toast.makeText(this@MainActivity, "Đã hoàn thành!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onToggleTaskStatus = { task ->
                                    scope.launch {
                                        repository.toggleTaskStatus(task.id, task.status)
                                        updateWidgets()
                                    }
                                },
                                onTogglePin = { task ->
                                    scope.launch {
                                        repository.togglePinTask(task.id, !task.isPinnedToWidget)
                                        updateWidgets()
                                    }
                                },
                                onTaskClick = { task ->
                                    selectedTaskId = task.id
                                    currentScreen = ScreenState.DETAIL
                                },
                                onSaveQuickNote = { newTask ->
                                    scope.launch {
                                        repository.createTask(newTask)
                                        updateWidgets()
                                        val reminderNotice = formatReminderConfirmation(newTask)
                                        Toast.makeText(this@MainActivity, "Đã lưu ghi chú!$reminderNotice", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }

                        ScreenState.CAMERA_CREATOR -> {
                            CameraCreatorScreen(
                                onSaveTask = { newTask ->
                                    scope.launch {
                                        repository.createTask(newTask)
                                        updateWidgets()
                                        currentScreen = ScreenState.HOME
                                        val reminderNotice = formatReminderConfirmation(newTask)
                                        Toast.makeText(this@MainActivity, "Đã lưu việc cần làm!$reminderNotice", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onClose = {
                                    currentScreen = ScreenState.HOME
                                }
                            )
                        }

                        ScreenState.CAMERA_AFTER_PROOF -> {
                            selectedTask?.let { taskToProve ->
                                CameraAfterProofScreen(
                                    task = taskToProve,
                                    onProofCaptured = { afterUri, caption ->
                                        scope.launch {
                                            repository.completeTaskWithProof(
                                                taskId = taskToProve.id,
                                                afterImageUri = afterUri,
                                                completionCaption = caption,
                                                completedAt = System.currentTimeMillis()
                                            )
                                            updateWidgets()
                                            selectedTaskId = null
                                            currentScreen = ScreenState.HOME
                                            Toast.makeText(this@MainActivity, "Đã hoàn thành Before / After!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onClose = {
                                        currentScreen = ScreenState.HOME
                                        selectedTaskId = null
                                    }
                                )
                            } ?: run {
                                currentScreen = ScreenState.HOME
                                selectedTaskId = null
                            }
                        }

                        ScreenState.DETAIL -> {
                            selectedTask?.let { task ->
                                TaskDetailScreen(
                                    task = task,
                                    onBack = {
                                        currentScreen = ScreenState.HOME
                                        selectedTaskId = null
                                    },
                                    onSaveTask = { updated ->
                                        scope.launch {
                                            repository.updateTask(updated)
                                            updateWidgets()
                                            val reminderNotice = formatReminderConfirmation(updated)
                                            Toast.makeText(this@MainActivity, "Đã cập nhật việc cần làm!$reminderNotice", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onDeleteTask = { toDelete ->
                                        scope.launch {
                                            repository.deleteTask(toDelete)
                                            updateWidgets()
                                            selectedTaskId = null
                                            currentScreen = ScreenState.HOME
                                            Toast.makeText(this@MainActivity, "Đã xóa việc cần làm!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onOpenAfterCamera = {
                                        selectedTaskId = task.id
                                        openCameraWithCheck(ScreenState.CAMERA_AFTER_PROOF)
                                    }
                                )
                            } ?: run {
                                currentScreen = ScreenState.HOME
                                selectedTaskId = null
                            }
                        }

                        ScreenState.BOARD -> {
                            BoardScreen(
                                tasks = displayTasks,
                                onToggleTaskStatus = { task ->
                                    scope.launch {
                                        repository.toggleTaskStatus(task.id, task.status)
                                        updateWidgets()
                                    }
                                },
                                onTaskClick = { task ->
                                    selectedTaskId = task.id
                                    currentScreen = ScreenState.DETAIL
                                },
                                onBack = {
                                    currentScreen = ScreenState.HOME
                                    selectedTaskId = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
