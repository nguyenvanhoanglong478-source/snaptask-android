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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.snaptask.app.ui.components.ViewfinderCornerBrackets
import com.snaptask.app.ui.theme.BeVietnamPro
import com.snaptask.app.ui.theme.Bricolage
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun CameraAfterProofScreen(
    task: Task,
    onProofCaptured: (afterImagePath: String, completionCaption: String) -> Unit,
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
    var isGhostOverlayEnabled by remember { mutableStateOf(true) }

    // Instant Feedback & Screen Flash States
    var showShutterFlash by remember { mutableStateOf(false) }
    var showScreenFlash by remember { mutableStateOf(false) }
    var originalScreenBrightness by remember { mutableStateOf<Float?>(null) }

    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var completionCaption by remember { mutableStateOf(task.completionCaption ?: "") }

    val beforeFile = remember(task.beforeImageUri) { File(task.beforeImageUri) }
    val cameraState = if (capturedPhotoPath != null) CameraState.CAPTURED else CameraState.PREVIEWING

    // Rebind Camera whenever cameraSelector changes with ViewPort matching PreviewView
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
                .setFlashMode(ImageCapture.FLASH_MODE_OFF) // Controlled via Torch-Assisted synchronization
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

    fun takeProofPhoto() {
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
                    } catch (_: Exception) {}
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0B0A))
    ) {
        val screenHeight = maxHeight
        val viewfinderHeight = screenHeight * 0.76f

        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topBarBottom = statusBarHeight + 8.dp + 44.dp
        val bracketsTop = topBarBottom + 36.dp
        val bracketsBottom = viewfinderHeight - 92.dp
        val bracketsHeight = (bracketsBottom - bracketsTop).coerceAtLeast(120.dp)

        // ==========================================
        // LỚP 1: KHUNG NGẮM TRÀN VIỀN (CAO 76%)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewfinderHeight)
                .align(Alignment.TopCenter)
                .background(Color.Black)
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

                // Ghost Alignment Overlay (Before photo superimposed with alpha 0.28f)
                if (isGhostOverlayEnabled && beforeFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(beforeFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Ghost Overlay Before",
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
                        contentDescription = "Ảnh kết quả After",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Dải mờ 200dp từ trong suốt sang #0C0B0A ở đáy khung ngắm
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF0C0B0A))
                        )
                    )
            )

            // Shutter Flash Feedback Overlay (120ms)
            if (showShutterFlash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.88f))
                )
            }
        }

        // ==========================================
        // LỚP 3: 4 GÓC NGẮM & CHIP GỢI Ý / ĐÃ CHỤP AFTER
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = bracketsTop)
                .height(bracketsHeight)
        ) {
            ViewfinderCornerBrackets(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF5F1E8),
                strokeWidth = 5.dp,
                length = 49.dp,
                cornerRadius = 20.dp
            )

            if (cameraState == CameraState.PREVIEWING) {
                // HUD Top: Ghost Overlay Toggle Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(if (isGhostOverlayEnabled) colors.flash else Color(0x9E0C0B0A))
                        .border(
                            1.dp,
                            if (isGhostOverlayEnabled) Color.Transparent else Color(0xFF34312B),
                            CircleShape
                        )
                        .clickable { isGhostOverlayEnabled = !isGhostOverlayEnabled }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isGhostOverlayEnabled) "Ghost: BẬT" else "Ghost: TẮT",
                        color = if (isGhostOverlayEnabled) colors.onFlash else Color(0xFFF5F1E8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Chip gợi ý: cao 32, padding ngang 14, nền rgba(12,11,10,0.62), chữ 13 Medium
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x9E0C0B0A))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chụp kết quả thực tế để đối chiếu Before / After",
                        color = Color(0xFFF5F1E8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Chip nhãn khi đã chụp: kiểu chip gợi ý "ĐÃ CHỤP AFTER", bỏ viền xanh lá quanh ảnh
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x9E0C0B0A))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ĐÃ CHỤP AFTER",
                        color = colors.done,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ==========================================
        // LỚP 2: THANH TRÊN (STATUS BAR + 8DP, 44DP)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 20.dp, end = 20.dp)
                .align(Alignment.TopCenter)
        ) {
            // Nút đóng: 44dp tròn, nền rgba(12,11,10,0.6), KHÔNG viền, icon 22dp
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x990C0B0A))
                    .clickable(onClick = onClose)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Đóng",
                    tint = Color(0xFFF5F1E8),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Tiêu đề: "Chụp ảnh kết quả (SAU)" Bricolage SemiBold 17/24 + tên việc cỡ 12 màu flash
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chụp ảnh kết quả (SAU)",
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFFF5F1E8),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = task.title,
                    fontFamily = BeVietnamPro,
                    fontSize = 12.sp,
                    color = colors.flash,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Nút flash: 44dp tròn, KHÔNG viền
            val isFlashOn = flashMode != CameraFlashMode.OFF
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isFlashOn) colors.flash else Color(0x990C0B0A))
                    .clickable { applyFlashMode(flashMode.next()) }
                    .align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        CameraFlashMode.OFF -> SnapTaskIcons.FlashOff
                        CameraFlashMode.ON, CameraFlashMode.TORCH -> SnapTaskIcons.FlashOn
                        CameraFlashMode.AUTO -> SnapTaskIcons.FlashAuto
                    },
                    contentDescription = "Flash: ${flashMode.name}",
                    tint = if (isFlashOn) colors.onFlash else Color(0xFFF5F1E8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ==========================================
        // LỚP 4: PANEL DƯỚI (BOTTOMCENTER, NAV PADDING)
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Ô ghi chú kết quả: cao 52, bo 16, nền rgba(27,26,23,0.92), viền 1dp #34312B, padding ngang 16, icon bút 20 #8F897B
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEB1B1A17))
                    .border(1.dp, Color(0xFF34312B), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Color(0xFF8F897B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (completionCaption.isEmpty()) {
                            Text(
                                text = "Ghi chú kết quả, ví dụ: Đã dọn sạch!",
                                color = Color(0xFF8F897B),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = completionCaption,
                            onValueChange = { completionCaption = it },
                            textStyle = TextStyle(
                                color = Color(0xFFF5F1E8),
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(colors.flash),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Hàng nút: Previewing Shutter HOẶC [Nút chụp lại 52x52] + [Nút Lưu & Hoàn Thành weight 1]
            if (cameraState == CameraState.PREVIEWING) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút phụ trái 52x52 bo 16: Đóng
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF26241F))
                            .border(1.dp, Color(0xFF34312B), RoundedCornerShape(16.dp))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Hủy",
                            tint = Color(0xFFF5F1E8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Nút chụp giữa 88dp (viền 4dp flash, đĩa trong 66dp flash)
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(4.dp, colors.flash, CircleShape)
                            .clickable(enabled = !isCapturing) {
                                takeProofPhoto()
                            },
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
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(colors.flash)
                            )
                        }
                    }

                    // Nút phụ phải 52x52 bo 16: Đổi camera
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF26241F))
                            .border(1.dp, Color(0xFF34312B), RoundedCornerShape(16.dp))
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
                            imageVector = Icons.Filled.FlipCameraAndroid,
                            contentDescription = "Đổi camera",
                            tint = Color(0xFFF5F1E8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                // CAPTURED: [Nút chụp lại 52x52 viền #34312B (KHÔNG viền đỏ)] + [Nút "Lưu & Hoàn Thành" weight 1]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút chụp lại 52x52 bo 16 nền #26241F viền #34312B
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF26241F))
                            .border(1.dp, Color(0xFF34312B), RoundedCornerShape(16.dp))
                            .clickable {
                                capturedPhotoPath?.let { File(it).delete() }
                                capturedPhotoPath = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Chụp lại",
                            tint = Color(0xFFF5F1E8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Nút "Lưu & Hoàn Thành" nền flash, chữ onFlash 15 Bold, cao 52, bo 16, weight 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.flash)
                            .clickable {
                                val finalPath = capturedPhotoPath ?: ""
                                onProofCaptured(finalPath, completionCaption)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = colors.onFlash,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lưu & Hoàn Thành",
                                color = colors.onFlash,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@ComposePreview(widthDp = 390, heightDp = 844)
@Composable
fun PreviewCameraAfterProofScreen() {
    SnapTaskTheme(darkTheme = true) {
        CameraAfterProofScreen(
            task = Task(
                id = 1,
                title = "Tưới cây ban công",
                beforeImageUri = "",
                folderName = "Nhà"
            ),
            onProofCaptured = { _, _ -> },
            onClose = {}
        )
    }
}
