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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.components.ViewfinderCornerBrackets
import com.snaptask.app.ui.theme.SnapTaskIcons
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
    val scrollState = rememberScrollState()
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
                .setFlashMode(ImageCapture.FLASH_MODE_OFF) // Controlled via Torch-Assisted synchronization
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
                    // Front screen flash
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
                    // Rear torch-assisted flash: 350ms delay
                    try {
                        camera?.cameraControl?.enableTorch(true)
                    } catch (_: Exception) {}
                    delay(350L)
                }

                // Shutter feedback: Haptic + 120ms Shutter Flash
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

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: [✕] Left, Title Center, [⚡] Right (Page 5 style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
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

                // Center Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Chụp ảnh kết quả (SAU)",
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = task.title,
                        color = colors.flash,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Flash button
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

            // 2. 1:1 Viewfinder with Corner Brackets & Ghost Overlay (Page 5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, if (cameraState == CameraState.CAPTURED) colors.done else colors.border, RoundedCornerShape(24.dp))
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

                    // 4 Corner Brackets on Viewfinder
                    ViewfinderCornerBrackets(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    )

                    // HUD Top-Center: Ghost Overlay Toggle Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGhostOverlayEnabled) colors.flash else Color(0x99121110))
                            .border(1.dp, if (isGhostOverlayEnabled) colors.flash else Color(0x4DFFFFFF), RoundedCornerShape(12.dp))
                            .clickable { isGhostOverlayEnabled = !isGhostOverlayEnabled }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isGhostOverlayEnabled) "Ghost: BẬT" else "Ghost: TẮT",
                            color = if (isGhostOverlayEnabled) colors.onFlash else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Overlay Pill at bottom inside viewfinder (Page 5)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xB3121110))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Chụp kết quả thực tế để đối chiếu Before / After",
                            color = Color(0xFFF5F1E8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Photo Taken Preview in 1:1 Squircle
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(capturedPhotoPath!!))
                            .crossfade(true)
                            .build(),
                        contentDescription = "After Proof Result",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Top-Left Badge: "ĐÃ CHỤP AFTER"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xD9121110))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ĐÃ CHỤP AFTER",
                            color = colors.done,
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

            // 3. Caption Input Pill (Page 5 style: Pill with Pen icon)
            OutlinedTextField(
                value = completionCaption,
                onValueChange = { completionCaption = it },
                placeholder = { Text("Ghi chú kết quả, ví dụ: Đã dọn sạch!", color = colors.textMuted, fontSize = 14.sp) },
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

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Bottom Controls: Previewing Shutter OR Captured Save Action (Page 5)
            if (cameraState == CameraState.PREVIEWING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Dismiss Button (48dp circle)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.elevated)
                            .border(1.dp, colors.border, CircleShape)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Hủy",
                            tint = colors.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center: Big Shutter Button (72dp outer / 56dp inner Flash Yellow)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
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
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(colors.flash, CircleShape)
                            )
                        }
                    }

                    // Right: Flip Camera Button (48dp circle)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.elevated)
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
                            imageVector = Icons.Filled.FlipCameraAndroid,
                            contentDescription = "Đổi camera",
                            tint = colors.text,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                // Photo Captured: Retake on Left, Big Save Button in Center
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Retake Button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colors.elevated)
                            .border(1.5.dp, colors.urgent, CircleShape)
                            .clickable {
                                capturedPhotoPath?.let { File(it).delete() }
                                capturedPhotoPath = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Chụp lại",
                            tint = colors.urgent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Primary Save & Complete Button: Flash Yellow (#FFD23F)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.flash)
                            .clickable {
                                val finalPath = capturedPhotoPath ?: ""
                                onProofCaptured(finalPath, completionCaption)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = SnapTaskIcons.Check,
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
