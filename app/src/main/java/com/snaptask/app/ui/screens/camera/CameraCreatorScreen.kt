package com.snaptask.app.ui.screens.camera

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
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
import com.snaptask.app.ui.theme.ChipActiveBg
import com.snaptask.app.ui.theme.ChipActiveText
import com.snaptask.app.ui.theme.ChipInactiveBg
import com.snaptask.app.ui.theme.ChipInactiveBorder
import com.snaptask.app.ui.theme.ChipInactiveText
import com.snaptask.app.ui.theme.LocketAccent
import com.snaptask.app.ui.theme.NotionBorder
import com.snaptask.app.ui.theme.NotionCardSurface
import com.snaptask.app.ui.theme.NotionDarkBg
import com.snaptask.app.ui.theme.StatusAmber
import com.snaptask.app.ui.theme.StatusDanger
import com.snaptask.app.ui.theme.StatusSuccess
import com.snaptask.app.ui.theme.TextMuted
import com.snaptask.app.ui.theme.TextPrimary
import com.snaptask.app.ui.theme.TextSecondary
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

@Composable
fun CameraCreatorScreen(
    onSaveTask: (Task) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    // Instant Feedback States
    var showShutterFlash by remember { mutableStateOf(false) }
    var showScreenFlash by remember { mutableStateOf(false) }

    // Task & Form States
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
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
                .setFlashMode(
                    when (flashMode) {
                        CameraFlashMode.ON -> ImageCapture.FLASH_MODE_ON
                        CameraFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                )
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
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    camera?.cameraControl?.enableTorch(true)
                }
            }
            CameraFlashMode.ON -> {
                camera?.cameraControl?.enableTorch(false)
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
            }
            CameraFlashMode.AUTO -> {
                camera?.cameraControl?.enableTorch(false)
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
            }
            CameraFlashMode.OFF -> {
                camera?.cameraControl?.enableTorch(false)
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
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
        }
    }

    fun takePhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true

        scope.launch {
            // Screen flash simulation for front camera
            if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA && flashMode != CameraFlashMode.OFF) {
                showScreenFlash = true
                delay(300)
            }

            // Shutter feedback: Haptic + 120ms Shutter Flash
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showShutterFlash = true
            launch {
                delay(120)
                showShutterFlash = false
                showScreenFlash = false
            }

            val rawFile = File(context.cacheDir, "snap_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(rawFile).build()

            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        scope.launch {
                            try {
                                val compressed = ImageCompressor.compressPhoto(context, rawFile)
                                capturedPhotoPath = compressed.fullImagePath
                                camera?.cameraControl?.enableTorch(false)
                            } catch (_: Exception) {
                            } finally {
                                isCapturing = false
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                    }
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NotionDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .background(NotionCardSurface, CircleShape)
                    .border(1.dp, NotionBorder, CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Đóng", tint = TextPrimary)
            }

            Text(
                text = "Tạo Task Mới",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            // Placeholder to keep title balanced center
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Camera Viewfinder 1:1 Aspect Ratio with 32dp Squircle Corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .border(2.dp, if (cameraState == CameraState.CAPTURED) LocketAccent else NotionBorder, RoundedCornerShape(32.dp))
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

                // Top Badge: "✅ ĐÃ CHỤP ẢNH"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✅ ĐÃ CHỤP",
                        color = LocketAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Screen Flash Overlay (For front camera)
            if (showScreenFlash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
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

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Camera Symmetrical Control Bar (Nhiệm vụ 3)
        if (cameraState == CameraState.PREVIEWING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Flash Mode Toggle Button (fixed 48dp circle)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NotionCardSurface)
                        .border(1.dp, if (flashMode != CameraFlashMode.OFF) LocketAccent else NotionBorder, CircleShape)
                        .clickable {
                            applyFlashMode(flashMode.next())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            CameraFlashMode.OFF -> Icons.Filled.FlashOff
                            CameraFlashMode.ON -> Icons.Filled.FlashOn
                            CameraFlashMode.AUTO -> Icons.Filled.FlashAuto
                            CameraFlashMode.TORCH -> Icons.Filled.Bolt
                        },
                        contentDescription = "Flash: ${flashMode.name}",
                        tint = if (flashMode != CameraFlashMode.OFF) LocketAccent else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center: Shutter Button (fixed 76dp outer / 62dp inner circle in LocketAccent)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(3.5.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(LocketAccent)
                        .clickable(enabled = !isCapturing) {
                            takePhoto()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Right: Flip Camera Button (fixed 48dp circle)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NotionCardSurface)
                        .border(1.dp, NotionBorder, CircleShape)
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
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Đổi camera trước/sau",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            // When CAPTURED: Retake Button ("🔄 Chụp lại")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NotionCardSurface)
                        .border(1.dp, NotionBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            capturedPhotoPath?.let { File(it).delete() }
                            capturedPhotoPath = null
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Chụp lại",
                            tint = LocketAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔄 Chụp lại (Retake)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Notion Quick Input & Metadata Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NotionCardSurface, RoundedCornerShape(24.dp))
                .border(1.dp, NotionBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            // Task Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Bạn cần làm gì với bức ảnh này?", color = TextMuted, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = NotionDarkBg,
                    unfocusedContainerColor = NotionDarkBg,
                    focusedBorderColor = LocketAccent,
                    unfocusedBorderColor = NotionBorder
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notion Quick Property Chips (Fixed 38dp height, 12dp corner, 12dp padding)
            Text(
                text = "THUỘC TÍNH NHANH (PROPERTIES)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Tag Property Chip
                item {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChipActiveBg)
                            .border(1.dp, LocketAccent, RoundedCornerShape(12.dp))
                            .clickable {
                                val nextIndex = (selectedTag.ordinal + 1) % TaskTag.entries.size
                                selectedTag = TaskTag.entries[nextIndex]
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🏷️ ${selectedTag.displayName}",
                            color = ChipActiveText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Priority Property Chip
                item {
                    val priorityColor = when (selectedPriority) {
                        TaskPriority.LOW -> StatusSuccess
                        TaskPriority.MEDIUM -> StatusAmber
                        TaskPriority.HIGH -> StatusDanger
                    }
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(priorityColor.copy(alpha = 0.2f))
                            .border(1.dp, priorityColor, RoundedCornerShape(12.dp))
                            .clickable {
                                val nextIndex = (selectedPriority.ordinal + 1) % TaskPriority.entries.size
                                selectedPriority = TaskPriority.entries[nextIndex]
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedPriority.label,
                                color = priorityColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Reminder Property Chip (Nhiệm vụ 4)
                item {
                    val hasReminder = reminderTime != null
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (hasReminder) LocketAccent.copy(alpha = 0.2f) else ChipInactiveBg)
                            .border(1.dp, if (hasReminder) LocketAccent else ChipInactiveBorder, RoundedCornerShape(12.dp))
                            .clickable { showReminderPicker = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Alarm,
                                contentDescription = "Nhắc nhở",
                                tint = if (hasReminder) LocketAccent else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasReminder) "⏰ ${DateTimeUtils.formatTime(reminderTime!!)}" else "⏰ Nhắc nhở",
                                color = if (hasReminder) LocketAccent else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (hasReminder) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // 4. Pin to Widget Property Chip
                item {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPinned) LocketAccent.copy(alpha = 0.2f) else ChipInactiveBg)
                            .border(1.dp, if (isPinned) LocketAccent else ChipInactiveBorder, RoundedCornerShape(12.dp))
                            .clickable { isPinned = !isPinned }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Ghim",
                                tint = if (isPinned) LocketAccent else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPinned) "📌 Đã ghim Widget" else "📌 Ghim Widget",
                                color = if (isPinned) LocketAccent else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isPinned) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Button: "Save & Pin to Widget 🚀" (Fixed 52dp height, corner 16dp)
            Button(
                onClick = {
                    val finalPhoto = capturedPhotoPath ?: ""
                    val task = Task(
                        title = title.ifBlank { "Việc cần làm" },
                        beforeImageUri = finalPhoto,
                        tag = selectedTag.name,
                        priority = selectedPriority.name,
                        isPinnedToWidget = isPinned,
                        reminderTime = reminderTime,
                        reminderType = reminderType
                    )
                    onSaveTask(task)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocketAccent,
                    contentColor = NotionDarkBg
                )
            ) {
                Text(
                    text = "Save & Pin to Widget 🚀",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
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
