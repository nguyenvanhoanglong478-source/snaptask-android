package com.snaptask.app.ui.screens.camera

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.domain.model.TaskTag
import com.snaptask.app.ui.screens.editor.ReminderPickerDialog
import androidx.compose.material.icons.filled.Folder
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

enum class CameraFlashMode {
    OFF, ON, AUTO, TORCH;
    fun next(): CameraFlashMode = when (this) {
        OFF -> ON
        ON -> AUTO
        AUTO -> TORCH
        TORCH -> OFF
    }
}

enum class CameraState {
    PREVIEWING,
    CAPTURED
}

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun CameraCreatorScreen(
    onSaveTask: (Task) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Camera Hardware References & State
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(CameraFlashMode.OFF) }
    var isCapturing by remember { mutableStateOf(false) }

    // Instant Feedback & Screen Flash States
    var showShutterFlash by remember { mutableStateOf(false) }
    var showScreenFlash by remember { mutableStateOf(false) }
    var originalScreenBrightness by remember { mutableStateOf<Float?>(null) }

    // Task & Form States
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("Cá nhân") }
    var selectedTag by remember { mutableStateOf(TaskTag.PERSONAL) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var isPinned by remember { mutableStateOf(true) }

    // Reminder States
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var reminderType by remember { mutableStateOf("NOTIFICATION") }
    var showReminderPicker by remember { mutableStateOf(false) }

    val cameraState = if (capturedPhotoPath != null) CameraState.CAPTURED else CameraState.PREVIEWING

    // Rebind Camera whenever cameraSelector changes
    fun bindCameraInternal() {
        val provider = cameraProvider ?: return
        val previewView = previewViewRef ?: return

        try {
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF) // Flash is controlled via Torch-Assisted sync to prevent de-sync
                .build()

            imageCapture = capture
            val boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )
            camera = boundCamera

            if (flashMode == CameraFlashMode.TORCH && cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                boundCamera.cameraControl.enableTorch(true)
            } else {
                boundCamera.cameraControl.enableTorch(false)
            }
        } catch (_: Exception) {}
    }

    fun applyFlashMode(newMode: CameraFlashMode) {
        flashMode = newMode
        when (newMode) {
            CameraFlashMode.TORCH -> {
                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    camera?.cameraControl?.enableTorch(true)
                }
            }
            CameraFlashMode.ON, CameraFlashMode.AUTO, CameraFlashMode.OFF -> {
                camera?.cameraControl?.enableTorch(false)
            }
        }
    }

    LaunchedEffect(cameraSelector) {
        bindCameraInternal()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
                cameraProvider?.unbindAll()
            } catch (_: Exception) {}

            // Restore screen brightness if modified by front camera screen flash
            val activity = findActivity(context)
            originalScreenBrightness?.let { orig ->
                try {
                    val window = activity?.window
                    val layoutParams = window?.attributes
                    if (layoutParams != null) {
                        layoutParams.screenBrightness = orig
                        window.attributes = layoutParams
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun takePhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true

        val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        val isFlashActive = flashMode == CameraFlashMode.ON || flashMode == CameraFlashMode.AUTO || flashMode == CameraFlashMode.TORCH

        scope.launch {
            val activity = findActivity(context)
            var originalBrightness: Float? = null

            try {
                if (isFrontCamera && isFlashActive) {
                    // 1. FRONT CAMERA SCREEN FLASH: Set 100% full screen brightness
                    val window = activity?.window
                    val layoutParams = window?.attributes
                    if (layoutParams != null) {
                        originalBrightness = layoutParams.screenBrightness
                        originalScreenBrightness = originalBrightness
                        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                        window.attributes = layoutParams
                    }
                    showScreenFlash = true
                    // Wait 350ms for display to reach peak illumination & sensor AE/AF to converge
                    delay(350L)
                } else if (!isFrontCamera && isFlashActive) {
                    // 2. REAR CAMERA TORCH-ASSISTED FLASH: Turn on physical LED
                    try {
                        camera?.cameraControl?.enableTorch(true)
                    } catch (_: Exception) {}
                    // Wait 350ms for physical LED to reach full brightness & sensor AE/AF convergence
                    delay(350L)
                }

                // Shutter feedback: Haptic vibration + 120ms Shutter Flash
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showShutterFlash = true
                launch {
                    delay(120L)
                    showShutterFlash = false
                }

                val rawFile = File(context.cacheDir, "snap_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(rawFile).build()

                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            // Turn off Rear Torch ONLY here inside callback after sensor has captured the frame
                            try {
                                if (flashMode != CameraFlashMode.TORCH) {
                                    camera?.cameraControl?.enableTorch(false)
                                }
                            } catch (_: Exception) {}

                            // Dismiss Front Screen Flash & restore original brightness ONLY here inside callback
                            if (showScreenFlash) {
                                showScreenFlash = false
                                originalBrightness?.let { orig ->
                                    try {
                                        val window = activity?.window
                                        val layoutParams = window?.attributes
                                        if (layoutParams != null) {
                                            layoutParams.screenBrightness = orig
                                            window.attributes = layoutParams
                                        }
                                    } catch (_: Exception) {}
                                }
                            }

                            // Compress and display the illuminated photo
                            scope.launch {
                                try {
                                    val compressed = ImageCompressor.compressPhoto(context, rawFile)
                                    capturedPhotoPath = compressed.fullImagePath
                                } catch (_: Exception) {
                                } finally {
                                    isCapturing = false
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // Cleanup on error
                            try {
                                if (flashMode != CameraFlashMode.TORCH) {
                                    camera?.cameraControl?.enableTorch(false)
                                }
                            } catch (_: Exception) {}

                            if (showScreenFlash) {
                                showScreenFlash = false
                                originalBrightness?.let { orig ->
                                    try {
                                        val window = activity?.window
                                        val layoutParams = window?.attributes
                                        if (layoutParams != null) {
                                            layoutParams.screenBrightness = orig
                                            window.attributes = layoutParams
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                            isCapturing = false
                        }
                    }
                )
            } catch (_: Exception) {
                isCapturing = false
                showScreenFlash = false
                try {
                    if (flashMode != CameraFlashMode.TORCH) {
                        camera?.cameraControl?.enableTorch(false)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val folderOptions = remember { listOf("Cá nhân", "Công việc", "Học tập", "Mua sắm") }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: [X] on Left, "Việc mới" in Center, [Flash] on Right (Page 5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Close Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Đóng",
                        tint = colors.text,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center: Screen Title
                Text(
                    text = "Việc mới",
                    color = colors.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Right: Flash Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (flashMode != CameraFlashMode.OFF) colors.flash else colors.surface)
                        .border(1.dp, if (flashMode != CameraFlashMode.OFF) colors.flash else colors.border, CircleShape)
                        .clickable { applyFlashMode(flashMode.next()) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            CameraFlashMode.OFF -> SnapTaskIcons.FlashOff
                            CameraFlashMode.ON, CameraFlashMode.TORCH -> SnapTaskIcons.FlashOn
                            CameraFlashMode.AUTO -> SnapTaskIcons.FlashAuto
                        },
                        contentDescription = "Flash: ${flashMode.name}",
                        tint = if (flashMode != CameraFlashMode.OFF) colors.onFlash else colors.text,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Camera Viewfinder 1:1 Aspect Ratio with Overlay Text Pill (Page 5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, if (cameraState == CameraState.CAPTURED) colors.flash else colors.border, RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (cameraState == CameraState.PREVIEWING) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            previewViewRef = previewView

                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                cameraProvider = providerFuture.get()
                                bindCameraInternal()
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Pill at bottom inside viewfinder: "Chụp thứ cần làm, đặt tên sau cũng được" (Page 5)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCC1B1A17))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Chụp thứ cần làm, đặt tên sau cũng được",
                            color = Color(0xFFF5F1E8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Instantly display captured image in 1:1 squircle
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(capturedPhotoPath!!))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Captured Photo Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Top Badge: "ĐÃ CHỤP"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ĐÃ CHỤP",
                            color = colors.flash,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Shutter Flash Feedback Overlay (120ms)
                if (showShutterFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.88f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Title Input Field: Pill with Pen Icon (Page 5: "Tên việc, ví dụ: Tưới cây")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Tên việc, ví dụ: Tưới cây", color = colors.textMuted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedContainerColor = colors.elevated,
                    unfocusedContainerColor = colors.elevated,
                    focusedBorderColor = colors.flash,
                    unfocusedBorderColor = colors.border
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Option Chips Row: [⏰ Hôm nay 18:00] [Ưu tiên: Cao] [Nhà] (Page 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip 1: Reminder Chip
                val hasReminder = reminderTime != null
                val reminderLabel = if (hasReminder) {
                    "⏰ ${DateTimeUtils.formatDateTime(reminderTime!!)}"
                } else "⏰ Đặt giờ nhắc"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (hasReminder) colors.flash else colors.elevated)
                        .border(1.dp, if (hasReminder) colors.flash else colors.border, RoundedCornerShape(14.dp))
                        .clickable { showReminderPicker = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = reminderLabel,
                        color = if (hasReminder) colors.onFlash else colors.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Chip 2: Priority Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.elevated)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable {
                            val nextIndex = (selectedPriority.ordinal + 1) % TaskPriority.entries.size
                            selectedPriority = TaskPriority.entries[nextIndex]
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Ưu tiên: ${selectedPriority.label}",
                        color = when (selectedPriority) {
                            TaskPriority.HIGH -> colors.urgent
                            TaskPriority.MEDIUM -> colors.warning
                            TaskPriority.LOW -> colors.text
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Chip 3: Folder Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.elevated)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable {
                            val nextIdx = (folderOptions.indexOf(selectedFolder) + 1) % folderOptions.size
                            selectedFolder = folderOptions[nextIdx]
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = selectedFolder,
                        color = colors.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Bottom Controls Bar: [☰] (Quick Note) - (O) (Big Shutter) - [🔄] (Flip Lens) (Page 5)
            if (cameraState == CameraState.PREVIEWING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Quick Note / Text Task Mode
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape)
                            .clickable {
                                // Save task as text-only task
                                val task = Task(
                                    title = title.ifBlank { "Ghi chú mới" },
                                    beforeImageUri = "",
                                    tag = selectedTag.name,
                                    folderName = selectedFolder,
                                    priority = selectedPriority.name,
                                    isPinnedToWidget = isPinned,
                                    reminderTime = reminderTime,
                                    reminderType = reminderType
                                )
                                onSaveTask(task)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notes,
                            contentDescription = "Ghi chú không ảnh",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center: Big Shutter Button (72dp outer ring, 56dp inner disk solid Flash Yellow)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.5.dp, colors.flash, CircleShape)
                            .clickable(enabled = !isCapturing) { takePhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = colors.flash,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.flash)
                            )
                        }
                    }

                    // Right: Camera Lens Flip Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape)
                            .clickable(enabled = !isCapturing) {
                                camera?.cameraControl?.enableTorch(false)
                                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.CameraFlip,
                            contentDescription = "Đổi ống kính",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // In CAPTURED state: Retake Button on Left, Save Task Button on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Retake
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.elevated)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .clickable {
                                capturedPhotoPath?.let { File(it).delete() }
                                capturedPhotoPath = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chụp lại",
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Save Task CTA
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.flash)
                            .clickable {
                                val finalPhoto = capturedPhotoPath ?: ""
                                val task = Task(
                                    title = title.ifBlank { "Việc cần làm" },
                                    beforeImageUri = finalPhoto,
                                    tag = selectedTag.name,
                                    folderName = selectedFolder,
                                    priority = selectedPriority.name,
                                    isPinnedToWidget = isPinned,
                                    reminderTime = reminderTime,
                                    reminderType = reminderType
                                )
                                onSaveTask(task)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lưu việc",
                            color = colors.onFlash,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Full Screen White Flash Overlay for Front Camera Screen Flash
        if (showScreenFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    }

    // Reminder Picker Bottom Sheet
    if (showReminderPicker) {
        ReminderPickerDialog(
            initialReminderTime = reminderTime,
            initialReminderType = reminderType,
            onConfirm = { time, type ->
                reminderTime = time
                reminderType = type
                showReminderPicker = false
            },
            onDismiss = { showReminderPicker = false }
        )
    }
}
