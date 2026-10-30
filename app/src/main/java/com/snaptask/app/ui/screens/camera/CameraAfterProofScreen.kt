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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private fun findProofActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

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

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(CameraFlashMode.OFF) }
    var isCapturing by remember { mutableStateOf(false) }
    var isGhostOverlayEnabled by remember { mutableStateOf(true) }

    var showShutterFlash by remember { mutableStateOf(false) }
    var showScreenFlash by remember { mutableStateOf(false) }
    var originalScreenBrightness by remember { mutableStateOf<Float?>(null) }

    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var completionCaption by remember { mutableStateOf(task.completionCaption ?: "") }

    val beforeFile = remember(task.beforeImageUri) { File(task.beforeImageUri) }
    val hasBeforeFile = remember(beforeFile) { task.beforeImageUri.isNotBlank() && beforeFile.exists() }
    val cameraState = if (capturedPhotoPath != null) CameraState.CAPTURED else CameraState.PREVIEWING

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

    LaunchedEffect(cameraSelector) {
        bindCameraInternal()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }

            val activity = findProofActivity(context)
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

    fun takeProofPhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true

        val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        val isFlashActive = flashMode != CameraFlashMode.OFF

        scope.launch {
            val activity = findProofActivity(context)
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

                val rawFile = File(context.cacheDir, "proof_${System.currentTimeMillis()}.jpg")
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
                        .clickable(role = Role.Button, onClick = onClose),
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

                val isFlashOn = flashMode != CameraFlashMode.OFF
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isFlashOn) ProofFlash else ProofElevated)
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
                        tint = if (isFlashOn) ProofOnFlash else ProofText,
                        modifier = Modifier.size(22.dp)
                    )
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
                                cameraProvider = providerFuture.get()
                                bindCameraInternal()
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isGhostOverlayEnabled && hasBeforeFile) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(beforeFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Khung mờ ảnh Trước",
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.28f),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    capturedPhotoPath?.let { path ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(path))
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
                                .clickable(role = Role.Button, onClick = onClose),
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
                                    capturedPhotoPath?.let { File(it).delete() }
                                    capturedPhotoPath = null
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

        if (showScreenFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.96f))
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
