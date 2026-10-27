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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.snaptask.app.ui.screens.editor.QuickSnapSheet
import com.snaptask.app.ui.screens.home.HomeScreen
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.widget.SnapTaskWidget
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    CAMERA,
    BOARD
}

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SnapTaskApplication
        val repository = app.taskRepository

        setContent {
            SnapTaskTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
                    val tasks by repository.getAllTasks().collectAsState(initial = emptyList())
                    val scope = rememberCoroutineScope()

                    // Quick Snap Bottom Sheet State
                    var pendingCapturedImage by remember { mutableStateOf<Pair<String, String>?>(null) }
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    // Camera permission launcher
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            currentScreen = ScreenState.CAMERA
                        } else {
                            Toast.makeText(this, "Cần cấp quyền máy ảnh để chụp ghi chú", Toast.LENGTH_SHORT).show()
                        }
                    }

                    fun openCameraWithCheck() {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            currentScreen = ScreenState.CAMERA
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }

                    when (currentScreen) {
                        ScreenState.HOME -> {
                            HomeScreen(
                                tasks = tasks,
                                onOpenLiveCamera = { openCameraWithCheck() },
                                onOpenBoardView = { currentScreen = ScreenState.BOARD },
                                onToggleTaskStatus = { task ->
                                    scope.launch {
                                        repository.toggleTaskStatus(task.id, task.status)
                                        SnapTaskWidget().updateAll(this@MainActivity)
                                    }
                                },
                                onTaskClick = { /* Can open full task detail */ }
                            )
                        }
                        ScreenState.CAMERA -> {
                            CameraCaptureScreen(
                                onPhotoCaptured = { fullPath, thumbPath ->
                                    currentScreen = ScreenState.HOME
                                    pendingCapturedImage = Pair(fullPath, thumbPath)
                                },
                                onClose = {
                                    currentScreen = ScreenState.HOME
                                }
                            )
                        }
                        ScreenState.BOARD -> {
                            BoardScreen(
                                tasks = tasks,
                                onToggleTaskStatus = { task ->
                                    scope.launch {
                                        repository.toggleTaskStatus(task.id, task.status)
                                        SnapTaskWidget().updateAll(this@MainActivity)
                                    }
                                },
                                onTaskClick = { /* Detail view */ },
                                onBack = { currentScreen = ScreenState.HOME }
                            )
                        }
                    }

                    // Show Quick Snap Modal Sheet when photo is captured
                    pendingCapturedImage?.let { (fullPath, thumbPath) ->
                        QuickSnapSheet(
                            imagePath = fullPath,
                            thumbnailPath = thumbPath,
                            sheetState = sheetState,
                            onSaveTask = { newTask ->
                                scope.launch {
                                    repository.createTask(newTask)
                                    pendingCapturedImage = null
                                    SnapTaskWidget().updateAll(this@MainActivity)
                                    Toast.makeText(this@MainActivity, "Đã lưu việc cần làm!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDismiss = {
                                pendingCapturedImage = null
                            }
                        )
                    }
                }
            }
        }
    }
}
