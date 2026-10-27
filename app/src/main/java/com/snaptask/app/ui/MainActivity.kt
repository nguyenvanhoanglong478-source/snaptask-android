package com.snaptask.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.snaptask.app.SnapTaskApplication
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.screens.board.BoardScreen
import com.snaptask.app.ui.screens.camera.CameraCaptureScreen
import com.snaptask.app.ui.screens.camera.CameraCreatorScreen
import com.snaptask.app.ui.screens.detail.TaskDetailScreen
import com.snaptask.app.ui.screens.home.HomeScreen
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.widget.LocketFocusWidget
import com.snaptask.app.widget.NotionStackWidget
import com.snaptask.app.widget.SnapTaskWidget
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    CAMERA_CREATOR,
    CAMERA_AFTER,
    DETAIL,
    BOARD
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SnapTaskApplication
        val repository = app.taskRepository

        setContent {
            SnapTaskTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
                    var selectedTask by remember { mutableStateOf<Task?>(null) }
                    val tasks by repository.getAllTasks().collectAsState(initial = emptyList())
                    val scope = rememberCoroutineScope()

                    fun updateWidgets() {
                        scope.launch {
                            try {
                                SnapTaskWidget().updateAll(this@MainActivity)
                                LocketFocusWidget().updateAll(this@MainActivity)
                                NotionStackWidget().updateAll(this@MainActivity)
                            } catch (_: Exception) {}
                        }
                    }

                    // Keep selectedTask synchronized with updated tasks list
                    selectedTask?.let { curr ->
                        tasks.find { it.id == curr.id }?.let { matched ->
                            if (matched != curr) selectedTask = matched
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
                                tasks = tasks,
                                onOpenLiveCamera = { openCameraWithCheck(ScreenState.CAMERA_CREATOR) },
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
                                    selectedTask = task
                                    currentScreen = ScreenState.DETAIL
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
                                        Toast.makeText(this@MainActivity, "Đã lưu việc cần làm! 🚀", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClose = {
                                    currentScreen = ScreenState.HOME
                                }
                            )
                        }

                        ScreenState.CAMERA_AFTER -> {
                            CameraCaptureScreen(
                                onPhotoCaptured = { fullPath, _ ->
                                    scope.launch {
                                        selectedTask?.let { t ->
                                            val updated = t.copy(
                                                afterImageUri = fullPath,
                                                isCompleted = true,
                                                completedAt = System.currentTimeMillis()
                                            )
                                            repository.updateTask(updated)
                                            selectedTask = updated
                                            updateWidgets()
                                            Toast.makeText(this@MainActivity, "Đã lưu ảnh hoàn thành! ✨", Toast.LENGTH_SHORT).show()
                                        }
                                        currentScreen = ScreenState.DETAIL
                                    }
                                },
                                onClose = {
                                    currentScreen = ScreenState.DETAIL
                                }
                            )
                        }

                        ScreenState.DETAIL -> {
                            selectedTask?.let { task ->
                                TaskDetailScreen(
                                    task = task,
                                    onBack = {
                                        currentScreen = ScreenState.HOME
                                        selectedTask = null
                                    },
                                    onSaveTask = { updated ->
                                        scope.launch {
                                            repository.updateTask(updated)
                                            selectedTask = updated
                                            updateWidgets()
                                            Toast.makeText(this@MainActivity, "Đã cập nhật việc cần làm!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteTask = { toDelete ->
                                        scope.launch {
                                            repository.deleteTask(toDelete)
                                            updateWidgets()
                                            selectedTask = null
                                            currentScreen = ScreenState.HOME
                                            Toast.makeText(this@MainActivity, "Đã xóa việc cần làm!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onOpenAfterCamera = {
                                        openCameraWithCheck(ScreenState.CAMERA_AFTER)
                                    }
                                )
                            } ?: run {
                                currentScreen = ScreenState.HOME
                            }
                        }

                        ScreenState.BOARD -> {
                            BoardScreen(
                                tasks = tasks,
                                onToggleTaskStatus = { task ->
                                    scope.launch {
                                        repository.toggleTaskStatus(task.id, task.status)
                                        updateWidgets()
                                    }
                                },
                                onTaskClick = { task ->
                                    selectedTask = task
                                    currentScreen = ScreenState.DETAIL
                                },
                                onBack = { currentScreen = ScreenState.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
