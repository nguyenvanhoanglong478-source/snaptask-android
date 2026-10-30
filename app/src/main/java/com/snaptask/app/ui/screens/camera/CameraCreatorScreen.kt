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
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.data.preferences.FolderPreferencesRepository
import com.snaptask.app.data.preferences.RepeatCadence
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.screens.editor.ReminderPickerDialog
import com.snaptask.app.ui.screens.home.components.QuickNoteBottomSheet
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.util.DateTimeUtils
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CameraFlashMode {
    OFF,
    ON,
    AUTO,
    TORCH;

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

// Forced-dark palette keeps the camera viewfinder free of light-mode glare.
private val CamBg = Color(0xFF0F0F0E)
private val CamSurface = Color(0xFF1A1A18)
private val CamElevated = Color(0xFF252522)
private val CamBorder = Color(0xFF34342F)
private val CamText = Color(0xFFFAFAF6)
private val CamTextSec = Color(0xFFB0AFA7)
private val CamFlash = Color(0xFFD4FF3A)
private val CamOnFlash = Color(0xFF10120A)
private val CamHot = Color(0xFFFF5C4D)
private val CamBubble = Color(0xAD0F0F0E)

@Composable
fun CameraCreatorScreen(
    tasks: List<Task>,
    folders: List<CustomFolder>,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onSaveTask: (Task, RepeatCadence) -> Unit,
    onSaveQuickNote: (Task) -> Unit,
    onGoHome: () -> Unit,
    onOpenFolders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(CameraFlashMode.OFF) }
    var isCapturing by remember { mutableStateOf(false) }

    var showShutterFlash by remember { mutableStateOf(false) }
    var showScreenFlash by remember { mutableStateOf(false) }
    var originalScreenBrightness by remember { mutableStateOf<Float?>(null) }

    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var repeatCadence by remember { mutableStateOf(RepeatCadence.NONE) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var reminderType by remember { mutableStateOf("NOTIFICATION") }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showQuickNoteSheet by remember { mutableStateOf(false) }

    val defaultFolderIdx = remember(folders) {
        folders.indexOfFirst { it.isDefault }.let { if (it >= 0) it else 0 }
    }
    var selectedFolderIdx by remember(folders.size) { mutableIntStateOf(defaultFolderIdx) }
    val activeFolder = folders.getOrNull(selectedFolderIdx.coerceIn(0, (folders.size - 1).coerceAtLeast(0)))

    val cameraState = if (capturedPhotoPath != null) CameraState.CAPTURED else CameraState.PREVIEWING

    val nowMillis = System.currentTimeMillis()
    val doneToday = remember(tasks) {
        tasks.count { it.isCompleted && SnapDateHelper.isSameDay(it.completedAt ?: it.createdAt, nowMillis) }
    }
    val openTasks = remember(tasks) {
        tasks.filter { !it.isCompleted }.sortedByDescending { it.createdAt }
    }
    val openCount = openTasks.size
    val totalToday = doneToday + openCount
    val pendingThumbFiles = remember(openTasks) {
        openTasks.mapNotNull { t ->
            if (t.hasImage) File(t.beforeImageUri).takeIf { it.exists() } else null
        }.take(3)
    }

    fun bindCameraInternal() {
        if (!hasCameraPermission) return
        val provider = cameraProvider ?: return
        val previewView = previewViewRef ?: return

        try {
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Flash is driven manually via 350ms torch/screen assist so exposure settles before capture.
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()

            imageCapture = capture

            val groupBuilder = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(capture)

            previewView.viewPort?.let { vp ->
                groupBuilder.setViewPort(vp)
            }

            val boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                groupBuilder.build()
            )
            camera = boundCamera

            if (flashMode == CameraFlashMode.TORCH && cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                boundCamera.cameraControl.enableTorch(true)
            } else {
                boundCamera.cameraControl.enableTorch(false)
            }
        } catch (_: Exception) {
        }
    }

    fun applyFlashMode(newMode: CameraFlashMode) {
        flashMode = newMode
        if (newMode == CameraFlashMode.TORCH && cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            camera?.cameraControl?.enableTorch(true)
        } else {
            camera?.cameraControl?.enableTorch(false)
        }
    }

    LaunchedEffect(cameraSelector, hasCameraPermission) {
        bindCameraInternal()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }

            val activity = findActivity(context)
            originalScreenBrightness?.let { orig ->
                try {
                    val window = activity?.window
                    val layoutParams = window?.attributes
                    if (layoutParams != null) {
                        layoutParams.screenBrightness = orig
                        window.attributes = layoutParams
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun cycleFolder() {
        if (folders.isEmpty()) {
            onOpenFolders()
        } else {
            selectedFolderIdx = (selectedFolderIdx + 1) % folders.size
        }
    }

    fun takePhoto() {
        if (!hasCameraPermission) {
            onRequestCameraPermission()
            return
        }
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true

        val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        val isFlashActive = flashMode != CameraFlashMode.OFF

        scope.launch {
            val activity = findActivity(context)
            var originalBrightness: Float? = null

            try {
                if (isFrontCamera && isFlashActive) {
                    val window = activity?.window
                    val layoutParams = window?.attributes
                    if (layoutParams != null) {
                        originalBrightness = layoutParams.screenBrightness
                        originalScreenBrightness = originalBrightness
                        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                        window.attributes = layoutParams
                    }
                    showScreenFlash = true
                    delay(350L)
                } else if (!isFrontCamera && isFlashActive) {
                    try {
                        camera?.cameraControl?.enableTorch(true)
                    } catch (_: Exception) {
                    }
                    delay(350L)
                }

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
                            try {
                                if (flashMode != CameraFlashMode.TORCH) {
                                    camera?.cameraControl?.enableTorch(false)
                                }
                            } catch (_: Exception) {
                            }

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
                                    } catch (_: Exception) {
                                    }
                                }
                            }

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
                            try {
                                if (flashMode != CameraFlashMode.TORCH) {
                                    camera?.cameraControl?.enableTorch(false)
                                }
                            } catch (_: Exception) {
                            }

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
                                    } catch (_: Exception) {
                                    }
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
                } catch (_: Exception) {
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CamBg)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: [146dp Today Progress Pill] ... [44dp Flash] [44dp Folders]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .width(148.dp)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(CamElevated)
                        .clickable(role = Role.Button, onClick = onGoHome)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val progressFraction = if (totalToday <= 0) 0f else (doneToday.toFloat() / totalToday).coerceIn(0f, 1f)
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val stroke = 2.6.dp.toPx()
                        drawCircle(color = CamBorder, style = Stroke(width = stroke))
                        if (progressFraction > 0f) {
                            drawArc(
                                color = CamFlash,
                                startAngle = -90f,
                                sweepAngle = 360f * progressFraction,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$doneToday/$totalToday hôm nay",
                        color = CamText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isFlashActive = flashMode != CameraFlashMode.OFF
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isFlashActive) CamFlash else CamElevated)
                            .clickable(role = Role.Button) { applyFlashMode(flashMode.next()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (flashMode) {
                                CameraFlashMode.OFF -> SnapTaskIcons.FlashOff
                                CameraFlashMode.AUTO -> SnapTaskIcons.FlashAuto
                                CameraFlashMode.ON, CameraFlashMode.TORCH -> SnapTaskIcons.FlashOn
                            },
                            contentDescription = "Bật tắt đèn flash",
                            tint = if (isFlashActive) CamOnFlash else CamText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CamElevated)
                            .clickable(role = Role.Button, onClick = onOpenFolders),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Folder,
                            contentDescription = "Quản lý thư mục",
                            tint = CamText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Rounded 44dp Viewfinder Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(44.dp))
                    .background(CamSurface)
                    .border(1.dp, CamBorder, RoundedCornerShape(44.dp))
            ) {
                if (cameraState == CameraState.PREVIEWING) {
                    if (hasCameraPermission) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Chưa cấp quyền máy ảnh",
                                color = CamText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cho phép máy ảnh để chụp việc trong 1 chạm, hoặc bấm nút Aa bên dưới để ghi chú chữ.",
                                color = CamTextSec,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(CamFlash)
                                    .clickable(role = Role.Button, onClick = onRequestCameraPermission)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cho phép máy ảnh",
                                    color = CamOnFlash,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                } else {
                    capturedPhotoPath?.let { path ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(path))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Ảnh việc vừa chụp",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Bottom scrim so the caption input stays high-contrast over bright subjects
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x990F0F0E))
                            )
                        )
                )

                // Top-left fixed-width (132dp) folder pill
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .width(132.dp)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(CamBubble)
                        .clickable(role = Role.Button) { cycleFolder() }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val folderColor = Color(activeFolder?.colorHex ?: 0xFFB0AFA7)
                    val folderIcon = SnapTaskIcons.folderIconByKey(activeFolder?.iconKey ?: "folder")
                    val folderLabel = activeFolder?.name ?: "Không mục"

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(folderColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = folderIcon,
                        contentDescription = null,
                        tint = CamText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = folderLabel,
                        color = CamText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Top-right fixed-width (84dp) captured status pill
                if (cameraState == CameraState.CAPTURED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .width(84.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(CamFlash),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đã chụp",
                            color = CamOnFlash,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Bottom-center frosted caption bubble
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .widthIn(min = 184.dp, max = 316.dp)
                        .clip(CircleShape)
                        .background(CamBubble)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (title.isEmpty()) {
                        Text(
                            text = "Thêm tên việc…",
                            color = Color(0xD9FAFAF6),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            color = CamText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(CamFlash),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showShutterFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.88f))
                    )
                }
            }

            // 3 Fixed Equal-Width Metadata Chips: Reminder | Priority | Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasReminder = reminderTime != null
                val remLabel = reminderTime?.let {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                } ?: "Nhắc giờ"

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hasReminder) CamFlash else CamElevated)
                        .clickable(role = Role.Button) { showReminderPicker = true }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.Clock,
                        contentDescription = null,
                        tint = if (hasReminder) CamOnFlash else CamText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = remLabel,
                        color = if (hasReminder) CamOnFlash else CamText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val isHighPrio = selectedPriority == TaskPriority.HIGH
                val prioText = when (selectedPriority) {
                    TaskPriority.LOW -> "Thấp"
                    TaskPriority.MEDIUM -> "Vừa"
                    TaskPriority.HIGH -> "Cao"
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isHighPrio) CamHot else CamElevated)
                        .clickable(role = Role.Button) {
                            val nextIdx = (selectedPriority.ordinal + 1) % TaskPriority.entries.size
                            selectedPriority = TaskPriority.entries[nextIdx]
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.FlagPriority,
                        contentDescription = null,
                        tint = if (isHighPrio) CamOnFlash else CamText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = prioText,
                        color = if (isHighPrio) CamOnFlash else CamText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                val hasRepeat = repeatCadence != RepeatCadence.NONE
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hasRepeat) CamFlash else CamElevated)
                        .clickable(role = Role.Button) { repeatCadence = repeatCadence.next() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.Repeat,
                        contentDescription = null,
                        tint = if (hasRepeat) CamOnFlash else CamText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = repeatCadence.chipLabel,
                        color = if (hasRepeat) CamOnFlash else CamText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Fixed 88dp-height Shutter / Save Row (zero vertical jump between preview & captured)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(88.dp),
                contentAlignment = Alignment.Center
            ) {
                if (cameraState == CameraState.PREVIEWING) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(CamElevated)
                                .clickable(role = Role.Button) { showQuickNoteSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.TextAa,
                                contentDescription = "Ghi chú chữ",
                                tint = CamText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .border(4.dp, CamFlash, CircleShape)
                                .clickable(enabled = !isCapturing, role = Role.Button) { takePhoto() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    color = CamFlash,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(CamFlash)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(CamElevated)
                                .clickable(enabled = !isCapturing && hasCameraPermission, role = Role.Button) {
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
                                contentDescription = "Đổi camera",
                                tint = CamText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CamElevated)
                                .clickable(role = Role.Button) {
                                    capturedPhotoPath?.let { File(it).delete() }
                                    capturedPhotoPath = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Close,
                                contentDescription = "Chụp lại",
                                tint = CamText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(CamFlash)
                                .clickable(role = Role.Button) {
                                    val finalPath = capturedPhotoPath ?: return@clickable
                                    val createdTime = System.currentTimeMillis()
                                    val cleanTitle = title.trim().ifBlank {
                                        "Việc mới ${DateTimeUtils.formatTime(createdTime)}"
                                    }
                                    val folderName = activeFolder?.name
                                        ?: FolderPreferencesRepository.UNASSIGNED_FOLDER_NAME
                                    val newTask = Task(
                                        id = 0,
                                        title = cleanTitle,
                                        beforeImageUri = finalPath,
                                        folderName = folderName,
                                        priority = selectedPriority.name,
                                        tag = repeatCadence.toTaskTag(),
                                        reminderTime = reminderTime,
                                        reminderType = if (reminderTime != null) reminderType else "NONE",
                                        createdAt = createdTime
                                    )
                                    onSaveTask(newTask, repeatCadence)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Lưu việc",
                                color = CamOnFlash,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = SnapTaskIcons.Check,
                                contentDescription = null,
                                tint = CamOnFlash,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CamElevated)
                                .clickable(role = Role.Button) { cycleFolder() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Folder,
                                contentDescription = "Đổi thư mục",
                                tint = CamText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Bottom "Việc của tôi" Pill
            Row(
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .width(218.dp)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(CamSurface)
                    .border(1.dp, CamBorder, CircleShape)
                    .clickable(role = Role.Button, onClick = onGoHome)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (pendingThumbFiles.isNotEmpty()) {
                    val stackWidth = (26 + (pendingThumbFiles.size - 1) * 16).dp
                    Box(
                        modifier = Modifier
                            .width(stackWidth)
                            .height(26.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        pendingThumbFiles.forEachIndexed { idx, file ->
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(file).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .offset(x = (idx * 16).dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, CamSurface, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = "Việc của tôi · $openCount",
                    color = CamText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = SnapTaskIcons.ChevronUp,
                    contentDescription = null,
                    tint = CamText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Front-camera full-screen illumination assist
        if (showScreenFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.96f))
            )
        }
    }

    if (showReminderPicker) {
        ReminderPickerDialog(
            initialReminderTime = reminderTime,
            initialReminderType = reminderType,
            onConfirm = { pickedTime, pickedType ->
                reminderTime = pickedTime
                reminderType = pickedType
                showReminderPicker = false
            },
            onDismiss = { showReminderPicker = false }
        )
    }

    if (showQuickNoteSheet) {
        QuickNoteBottomSheet(
            onDismissRequest = { showQuickNoteSheet = false },
            onSave = { newTask ->
                showQuickNoteSheet = false
                onSaveQuickNote(newTask)
            },
            existingFolders = folders.map { it.name }.ifEmpty { listOf("Nhà", "Công việc", "Cá nhân") }
        )
    }
}

@ComposePreview(widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCameraCreatorScreen() {
    SnapTaskTheme(darkTheme = true) {
        CameraCreatorScreen(
            tasks = emptyList(),
            folders = listOf(
                CustomFolder("nha", "Nhà", "home", 0xFFD4FF3A, true)
            ),
            hasCameraPermission = false,
            onRequestCameraPermission = {},
            onSaveTask = { _, _ -> },
            onSaveQuickNote = {},
            onGoHome = {},
            onOpenFolders = {}
        )
    }
}
