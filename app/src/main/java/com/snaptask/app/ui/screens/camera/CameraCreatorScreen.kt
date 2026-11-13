package com.snaptask.app.ui.screens.camera

import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.IntOffset
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
import com.snaptask.app.util.FileUtils
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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

private val GalleryPhotoIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "GalleryPhoto",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(5.5f, 4f)
        lineTo(18.5f, 4f)
        arcTo(2f, 2f, 0f, false, true, 20.5f, 6f)
        lineTo(20.5f, 18f)
        arcTo(2f, 2f, 0f, false, true, 18.5f, 20f)
        lineTo(5.5f, 20f)
        arcTo(2f, 2f, 0f, false, true, 3.5f, 18f)
        lineTo(3.5f, 6f)
        arcTo(2f, 2f, 0f, false, true, 5.5f, 4f)
        close()
        moveTo(8.8f, 8.8f)
        arcTo(1.2f, 1.2f, 0f, true, true, 8.79f, 8.8f)
        close()
        moveTo(4f, 16.5f)
        lineTo(9.2f, 11.8f)
        lineTo(13.5f, 15.8f)
        lineTo(16.2f, 13.2f)
        lineTo(20f, 16.8f)
    }.build()
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
    val snackbarHostState = remember { SnackbarHostState() }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(CameraFlashMode.OFF) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    var showShutterFlash by remember { mutableStateOf(false) }

    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var focusTapTriggerId by remember { mutableIntStateOf(0) }
    val focusRingAlpha = remember { Animatable(0f) }
    val focusRingScale = remember { Animatable(1.25f) }

    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var isPhotoSaved by remember { mutableStateOf(false) }
    var isFromGallery by remember { mutableStateOf(false) }
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

    fun showCameraError(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    fun cleanupFlashEffects() {
        val keepTorch = hasFlashUnit && flashMode == CameraFlashMode.TORCH
        if (!keepTorch) {
            try {
                camera?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {
            }
        }
    }

    fun discardUnsavedPhoto() {
        if (!isPhotoSaved) {
            FileUtils.deleteTaskImages(capturedPhotoPath)
        }
        capturedPhotoPath = null
        isPhotoSaved = false
        isFromGallery = false
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null || isCapturing) return@rememberLauncherForActivityResult
        isCapturing = true
        scope.launch {
            try {
                val compressedPath = ImageCompressor.compressUri(context, uri)
                val previousUnsaved = if (!isPhotoSaved) capturedPhotoPath else null
                capturedPhotoPath = compressedPath
                isPhotoSaved = false
                isFromGallery = true
                if (previousUnsaved != null && previousUnsaved != compressedPath) {
                    FileUtils.deleteTaskImages(previousUnsaved)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                showCameraError("Không thể chọn ảnh từ thư viện. Vui lòng thử ảnh khác.")
            } finally {
                isCapturing = false
            }
        }
    }

    fun openGalleryPicker() {
        if (isCapturing) return
        try {
            galleryPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: Exception) {
            showCameraError("Không thể mở thư viện ảnh trên thiết bị này.")
        }
    }

    fun handleGoHome() {
        discardUnsavedPhoto()
        onGoHome()
    }

    fun handleOpenFolders() {
        discardUnsavedPhoto()
        onOpenFolders()
    }

    fun bindCameraInternal() {
        if (!hasCameraPermission) return
        val provider = cameraProvider ?: return
        val previewView = previewViewRef ?: return

        try {
            provider.unbindAll()

            val previewSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        AndroidSize(1280, 960),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                )
                .build()

            val captureSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        AndroidSize(1440, 1080),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(previewSelector)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Flash is driven manually via 350ms torch assist so exposure settles before capture.
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .setResolutionSelector(captureSelector)
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
            val cameraHasFlash = boundCamera.cameraInfo.hasFlashUnit()
            hasFlashUnit = cameraHasFlash

            if (!cameraHasFlash) {
                flashMode = CameraFlashMode.OFF
                boundCamera.cameraControl.enableTorch(false)
            } else if (flashMode == CameraFlashMode.TORCH) {
                boundCamera.cameraControl.enableTorch(true)
            } else {
                boundCamera.cameraControl.enableTorch(false)
            }
        } catch (_: Exception) {
            camera = null
            imageCapture = null
            hasFlashUnit = false
            flashMode = CameraFlashMode.OFF
            showCameraError("Không thể khởi tạo máy ảnh. Vui lòng thử lại.")
        }
    }

    fun applyFlashMode(newMode: CameraFlashMode) {
        if (!hasFlashUnit) {
            flashMode = CameraFlashMode.OFF
            try {
                camera?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {
            }
            return
        }
        flashMode = newMode
        try {
            if (newMode == CameraFlashMode.TORCH) {
                camera?.cameraControl?.enableTorch(true)
            } else {
                camera?.cameraControl?.enableTorch(false)
            }
        } catch (_: Exception) {
            showCameraError("Không thể bật đèn flash trên thiết bị này.")
        }
    }

    LaunchedEffect(cameraSelector, hasCameraPermission) {
        focusTapOffset = null
        bindCameraInternal()
    }

    LaunchedEffect(cameraState) {
        if (cameraState == CameraState.CAPTURED) {
            focusTapOffset = null
            try {
                camera?.cameraControl?.enableTorch(false)
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(focusTapTriggerId) {
        if (focusTapTriggerId > 0 && focusTapOffset != null) {
            focusRingAlpha.snapTo(1f)
            focusRingScale.snapTo(1.28f)
            focusRingScale.animateTo(1f, tween(durationMillis = 180))
            delay(650L)
            focusRingAlpha.animateTo(0f, tween(durationMillis = 200))
            focusTapOffset = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
            if (!isPhotoSaved) {
                FileUtils.deleteTaskImages(capturedPhotoPath)
            }
        }
    }

    fun cycleFolder() {
        if (folders.isEmpty()) {
            handleOpenFolders()
        } else {
            selectedFolderIdx = (selectedFolderIdx + 1) % folders.size
        }
    }

    fun takePhoto() {
        if (!hasCameraPermission) {
            onRequestCameraPermission()
            return
        }
        val capture = imageCapture ?: run {
            showCameraError("Máy ảnh chưa sẵn sàng. Vui lòng thử lại.")
            return
        }
        if (isCapturing) return
        isCapturing = true

        val isFlashActive = flashMode != CameraFlashMode.OFF

        scope.launch {
            var captureSubmitted = false
            var rawFile: File? = null

            try {
                if (hasFlashUnit && isFlashActive) {
                    try {
                        camera?.cameraControl?.enableTorch(true)
                    } catch (_: Exception) {
                    }
                    delay(350L)
                }

                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showShutterFlash = true
                scope.launch {
                    delay(120L)
                    showShutterFlash = false
                }

                val tempFile = File(context.cacheDir, "raw_snap_${System.currentTimeMillis()}.jpg")
                rawFile = tempFile
                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                captureSubmitted = true
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            cleanupFlashEffects()
                            val compressJob = scope.launch {
                                try {
                                    val compressedPath = ImageCompressor.compressPhoto(context, tempFile)
                                    val previousUnsaved = if (!isPhotoSaved) capturedPhotoPath else null
                                    capturedPhotoPath = compressedPath
                                    isPhotoSaved = false
                                    isFromGallery = false
                                    if (previousUnsaved != null && previousUnsaved != compressedPath) {
                                        FileUtils.deleteTaskImages(previousUnsaved)
                                    }
                                } catch (e: CancellationException) {
                                    FileUtils.deleteTaskImages(tempFile.absolutePath)
                                    throw e
                                } catch (_: Exception) {
                                    FileUtils.deleteTaskImages(tempFile.absolutePath)
                                    showCameraError("Không thể xử lý ảnh vừa chụp. Vui lòng thử lại.")
                                } finally {
                                    isCapturing = false
                                }
                            }
                            if (compressJob.isCancelled) {
                                FileUtils.deleteTaskImages(tempFile.absolutePath)
                                isCapturing = false
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            cleanupFlashEffects()
                            FileUtils.deleteTaskImages(tempFile.absolutePath)
                            isCapturing = false
                            showCameraError("Chụp ảnh thất bại. Vui lòng thử lại.")
                        }
                    }
                )
            } catch (e: CancellationException) {
                captureSubmitted = false
                FileUtils.deleteTaskImages(rawFile?.absolutePath)
                cleanupFlashEffects()
                isCapturing = false
                throw e
            } catch (_: Exception) {
                captureSubmitted = false
                FileUtils.deleteTaskImages(rawFile?.absolutePath)
                cleanupFlashEffects()
                isCapturing = false
                showCameraError("Không thể chụp ảnh. Vui lòng thử lại.")
            } finally {
                if (!captureSubmitted) {
                    cleanupFlashEffects()
                    isCapturing = false
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
            // Uncluttered Top Bar: [Status Pill + Active Folder Chip] ... [Flash?] [Manage Folders]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(CamElevated)
                            .border(1.dp, CamBorder, CircleShape)
                            .clickable(role = Role.Button) { handleGoHome() }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val progressFraction = if (totalToday <= 0) 0f else (doneToday.toFloat() / totalToday).coerceIn(0f, 1f)
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val stroke = 2.4.dp.toPx()
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$doneToday/$totalToday xong",
                            color = CamText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    val folderColor = Color(activeFolder?.colorHex ?: 0xFFB0AFA7)
                    val folderIcon = SnapTaskIcons.folderIconByKey(activeFolder?.iconKey ?: "folder")
                    val folderLabel = activeFolder?.name ?: "Không mục"

                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(CamSurface)
                            .border(1.dp, CamBorder, CircleShape)
                            .clickable(role = Role.Button) { cycleFolder() }
                            .padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(folderColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = folderIcon,
                            contentDescription = null,
                            tint = CamText,
                            modifier = Modifier.size(14.dp)
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
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isFlashSupported = cameraState == CameraState.PREVIEWING && hasCameraPermission && hasFlashUnit
                    if (isFlashSupported) {
                        val isFlashActive = flashMode != CameraFlashMode.OFF
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isFlashActive) CamFlash else CamElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isFlashActive) Color.Transparent else CamBorder,
                                    shape = CircleShape
                                )
                                .clickable(role = Role.Button) {
                                    applyFlashMode(flashMode.next())
                                },
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CamElevated)
                            .border(1.dp, CamBorder, CircleShape)
                            .clickable(role = Role.Button) { handleOpenFolders() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Folder,
                            contentDescription = "Quản lý thư mục",
                            tint = CamText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Rounded 40dp Viewfinder Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(CamSurface)
                    .border(1.dp, CamBorder, RoundedCornerShape(40.dp))
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
                                    try {
                                        cameraProvider = providerFuture.get()
                                        bindCameraInternal()
                                    } catch (_: Exception) {
                                        showCameraError("Không thể khởi động máy ảnh. Vui lòng thử lại.")
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(camera, previewViewRef, hasCameraPermission) {
                                    detectTapGestures { tapOffset ->
                                        val boundCamera = camera ?: return@detectTapGestures
                                        val pv = previewViewRef ?: return@detectTapGestures
                                        try {
                                            val point = pv.meteringPointFactory.createPoint(tapOffset.x, tapOffset.y)
                                            val action = FocusMeteringAction.Builder(
                                                point,
                                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                            )
                                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                                .build()
                                            focusTapOffset = tapOffset
                                            focusTapTriggerId++
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            boundCamera.cameraControl.startFocusAndMetering(action)
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                        )

                        focusTapOffset?.let { tap ->
                            val ringSizeDp = 64.dp
                            val ringPx = with(LocalDensity.current) { ringSizeDp.toPx() }
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (tap.x - ringPx / 2f).roundToInt(),
                                            y = (tap.y - ringPx / 2f).roundToInt()
                                        )
                                    }
                                    .size(ringSizeDp)
                                    .graphicsLayer {
                                        scaleX = focusRingScale.value
                                        scaleY = focusRingScale.value
                                        alpha = focusRingAlpha.value
                                    }
                                    .border(2.dp, CamFlash, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CamFlash)
                                )
                            }
                        }
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
                                text = "Cho phép máy ảnh để chụp việc trong 1 chạm, chọn từ thư viện hoặc bấm Ghi chú bên dưới.",
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
                                .size(960)
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
                        .height(104.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x990F0F0E))
                            )
                        )
                )

                // Top-right captured status pill
                if (cameraState == CameraState.CAPTURED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(CamFlash)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFromGallery) "Từ thư viện" else "Đã chụp",
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

            // Compact Ergonomic One-Handed Shutter / Save Row (82dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(82.dp),
                contentAlignment = Alignment.Center
            ) {
                if (cameraState == CameraState.PREVIEWING) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(CamElevated)
                                .border(1.dp, CamBorder, CircleShape)
                                .clickable(enabled = !isCapturing, role = Role.Button) {
                                    openGalleryPicker()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = GalleryPhotoIcon,
                                contentDescription = "Chọn ảnh từ thư viện",
                                tint = CamText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .border(4.dp, CamFlash, CircleShape)
                                .clickable(enabled = !isCapturing, role = Role.Button) { takePhoto() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    color = CamFlash,
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(CamFlash)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(CamElevated)
                                .border(1.dp, CamBorder, CircleShape)
                                .clickable(enabled = !isCapturing && hasCameraPermission, role = Role.Button) {
                                    try {
                                        camera?.cameraControl?.enableTorch(false)
                                    } catch (_: Exception) {
                                    }
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
                                tint = if (hasCameraPermission) CamText else CamTextSec,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(CamElevated)
                                .border(1.dp, CamBorder, CircleShape)
                                .clickable(enabled = !isCapturing, role = Role.Button) {
                                    discardUnsavedPhoto()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Retake,
                                contentDescription = "Chụp lại",
                                tint = CamText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(CircleShape)
                                .background(CamFlash)
                                .clickable(enabled = !isCapturing, role = Role.Button) {
                                    val finalPath = capturedPhotoPath ?: return@clickable
                                    isPhotoSaved = true
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
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(CamElevated)
                                .border(1.dp, CamBorder, CircleShape)
                                .clickable(enabled = !isCapturing, role = Role.Button) {
                                    openGalleryPicker()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = GalleryPhotoIcon,
                                contentDescription = "Đổi ảnh thư viện",
                                tint = CamText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Bottom One-Handed Dock: ["Việc của tôi · N" Pill] + [Aa "Ghi chú" Pill]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(CamSurface)
                        .border(1.dp, CamBorder, CircleShape)
                        .clickable(role = Role.Button) { handleGoHome() }
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
                                    model = ImageRequest.Builder(context)
                                        .data(file)
                                        .size(120)
                                        .crossfade(true)
                                        .build(),
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = SnapTaskIcons.ChevronUp,
                        contentDescription = null,
                        tint = CamText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(CamElevated)
                        .border(1.dp, CamBorder, CircleShape)
                        .clickable(role = Role.Button) { showQuickNoteSheet = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.TextAa,
                        contentDescription = null,
                        tint = CamText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ghi chú",
                        color = CamText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = CamElevated,
                contentColor = CamText,
                actionColor = CamFlash,
                shape = RoundedCornerShape(16.dp)
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
