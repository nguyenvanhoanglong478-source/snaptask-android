package com.snaptask.app.ui.screens.camera

import android.util.Size as AndroidSize
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.util.FileUtils
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private val ProofBg = Color(0xFF0F0F0E)
private val ProofSurface = Color(0xFF1A1A18)
private val ProofElevated = Color(0xFF252522)
private val ProofBorder = Color(0xFF34342F)
private val ProofText = Color(0xFFFAFAF6)
private val ProofTextSec = Color(0xFFB0AFA7)
private val ProofFlash = Color(0xFFD4FF3A)
private val ProofOnFlash = Color(0xFF10120A)
private val ProofBubble = Color(0xAD0F0F0E)

@Composable
fun CameraAfterProofScreen(
    task: Task,
    onProofCaptured: (afterImagePath: String, completionCaption: String) -> Unit,
    onClose: () -> Unit,
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
    var isGhostOverlayEnabled by remember { mutableStateOf(true) }

    var showShutterFlash by remember { mutableStateOf(false) }

    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var focusTapTriggerId by remember { mutableIntStateOf(0) }
    val focusRingAlpha = remember { Animatable(0f) }
    val focusRingScale = remember { Animatable(1.25f) }

    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var isPhotoSaved by remember { mutableStateOf(false) }
    var completionCaption by remember { mutableStateOf(task.completionCaption ?: "") }

    val beforeFile = remember(task.beforeImageUri) { File(task.beforeImageUri) }
    val hasBeforeFile = remember(beforeFile) { task.beforeImageUri.isNotBlank() && beforeFile.exists() }
    val cameraState = if (capturedPhotoPath != null) CameraState.CAPTURED else CameraState.PREVIEWING

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

    fun discardUnsavedProof() {
        if (!isPhotoSaved) {
            FileUtils.deleteTaskImages(capturedPhotoPath)
        }
        capturedPhotoPath = null
        isPhotoSaved = false
    }

    fun handleClose() {
        discardUnsavedProof()
        onClose()
    }

    fun bindCameraInternal() {
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

    LaunchedEffect(cameraSelector) {
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

    fun takeProofPhoto() {
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

                val tempFile = File(context.cacheDir, "raw_proof_${System.currentTimeMillis()}.jpg")
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
                                    if (previousUnsaved != null && previousUnsaved != compressedPath) {
                                        FileUtils.deleteTaskImages(previousUnsaved)
                                    }
                                } catch (e: CancellationException) {
                                    FileUtils.deleteTaskImages(tempFile.absolutePath)
                                    throw e
                                } catch (_: Exception) {
                                    FileUtils.deleteTaskImages(tempFile.absolutePath)
                                    showCameraError("Không thể xử lý ảnh kết quả. Vui lòng thử lại.")
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
                            showCameraError("Chụp ảnh kết quả thất bại. Vui lòng thử lại.")
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
                showCameraError("Không thể chụp ảnh kết quả. Vui lòng thử lại.")
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
            .background(ProofBg)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Symmetric [44dp Close | Centered Title + Task Name | 44dp Flash] Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ProofElevated)
                        .clickable(role = Role.Button) { handleClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.Close,
                        contentDescription = "Đóng",
                        tint = ProofText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Chụp ảnh kết quả (SAU)",
                        color = ProofText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = task.title,
                        color = ProofTextSec,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                val isFlashSupported = cameraState == CameraState.PREVIEWING && hasFlashUnit
                if (isFlashSupported) {
                    val isFlashOn = flashMode != CameraFlashMode.OFF
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isFlashOn) ProofFlash else ProofElevated)
                            .border(
                                width = 1.dp,
                                color = if (isFlashOn) Color.Transparent else ProofBorder,
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
                            tint = if (isFlashOn) ProofOnFlash else ProofText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }

            // Rounded 44dp Viewfinder with Ghost Before alignment overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(44.dp))
                    .background(ProofSurface)
                    .border(1.dp, ProofBorder, RoundedCornerShape(44.dp))
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

                    if (isGhostOverlayEnabled && hasBeforeFile) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(beforeFile)
                                .size(720)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Khung mờ ảnh Trước",
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.28f),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(camera, previewViewRef) {
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
                                .border(2.dp, ProofFlash, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ProofFlash)
                            )
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
                            contentDescription = "Ảnh kết quả Sau",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

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

                // Top-left fixed-width (140dp) Ghost toggle pill when Before image exists
                if (cameraState == CameraState.PREVIEWING && hasBeforeFile) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .width(142.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(if (isGhostOverlayEnabled) ProofFlash else ProofBubble)
                            .clickable(role = Role.Switch) { isGhostOverlayEnabled = !isGhostOverlayEnabled },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isGhostOverlayEnabled) "Khung mờ: BẬT" else "Khung mờ: TẮT",
                            color = if (isGhostOverlayEnabled) ProofOnFlash else ProofText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (cameraState == CameraState.CAPTURED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .width(104.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(ProofFlash),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đã chụp SAU",
                            color = ProofOnFlash,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Bottom-center frosted completion caption bubble
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .widthIn(min = 196.dp, max = 324.dp)
                        .clip(CircleShape)
                        .background(ProofBubble)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (completionCaption.isEmpty()) {
                        Text(
                            text = "Ghi chú kết quả…",
                            color = Color(0xD9FAFAF6),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    BasicTextField(
                        value = completionCaption,
                        onValueChange = { completionCaption = it },
                        textStyle = TextStyle(
                            color = ProofText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(ProofFlash),
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

            // Fixed 88dp-height Shutter / Save Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
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
                                .background(ProofElevated)
                                .clickable(role = Role.Button) { handleClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Close,
                                contentDescription = "Hủy",
                                tint = ProofText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .border(4.dp, ProofFlash, CircleShape)
                                .clickable(enabled = !isCapturing, role = Role.Button) { takeProofPhoto() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    color = ProofFlash,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(ProofFlash)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ProofElevated)
                                .clickable(enabled = !isCapturing, role = Role.Button) {
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
                                tint = ProofText,
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
                                .background(ProofElevated)
                                .clickable(role = Role.Button) {
                                    discardUnsavedProof()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Retake,
                                contentDescription = "Chụp lại",
                                tint = ProofText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(ProofFlash)
                                .clickable(role = Role.Button) {
                                    val finalPath = capturedPhotoPath ?: return@clickable
                                    isPhotoSaved = true
                                    onProofCaptured(finalPath, completionCaption.trim())
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.Check,
                                contentDescription = null,
                                tint = ProofOnFlash,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lưu & Hoàn thành",
                                color = ProofOnFlash,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
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
                containerColor = ProofElevated,
                contentColor = ProofText,
                actionColor = ProofFlash,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@ComposePreview(widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCameraAfterProofScreen() {
    SnapTaskTheme(darkTheme = true) {
        CameraAfterProofScreen(
            task = Task(
                id = 1,
                title = "Dọn bàn làm việc",
                beforeImageUri = "",
                folderName = "Nhà"
            ),
            onProofCaptured = { _, _ -> },
            onClose = {}
        )
    }
}
