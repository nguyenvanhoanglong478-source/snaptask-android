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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
                    // 1. FRONT CAMERA SCREEN FLASH: Maximize screen brightness
                    val window = activity?.window
                    val layoutParams = window?.attributes
                    if (layoutParams != null) {
                        originalBrightness = layoutParams.screenBrightness
                        originalScreenBrightness = originalBrightness
                        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                        window.attributes = layoutParams
                    }
                    showScreenFlash = true
                    // Wait 350ms for display to reach peak illumination & sensor AE/AF convergence
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

                val rawFile = File(context.cacheDir, "proof_${System.currentTimeMillis()}.jpg")
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

                            // Compress and display the illuminated after proof photo
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

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
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
                        .background(colors.surface, CircleShape)
                        .border(1.dp, colors.border, CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Đóng", tint = colors.text)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Xác nhận hoàn thành",
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = task.title,
                        color = colors.flash,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // Balance placeholder
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1:1 Camera Viewfinder with Squircle 32dp and HUD controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, if (cameraState == CameraState.CAPTURED) colors.done else colors.border, RoundedCornerShape(32.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (cameraState == CameraState.PREVIEWING) {
                    // Live Camera View
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

                    // HUD Top-Center: Ghost Overlay Toggle Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGhostOverlayEnabled) colors.flash else Color(0x80000000)) // Semi-transparent black scrim for ghost toggle over preview
                            .border(1.dp, if (isGhostOverlayEnabled) colors.flash else Color(0x33FFFFFF), RoundedCornerShape(12.dp)) // Translucent white border for ghost toggle over preview
                            .clickable { isGhostOverlayEnabled = !isGhostOverlayEnabled }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isGhostOverlayEnabled) "Ghost: BẬT" else "Ghost: TẮT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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

                    // Top Badge: "ĐÃ CHỤP AFTER"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xCC000000)) // Camera viewfinder dark backdrop
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

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Camera Controls: 3 Circular Buttons (Flash - Shutter/Retake - Flip) spaced by 32dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Flash Button (46x46 circular)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
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
                        tint = if (flashMode != CameraFlashMode.OFF) colors.flash else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center: Shutter (88x88) or Retake (56x56)
                if (cameraState == CameraState.PREVIEWING) {
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
                                    .background(colors.flash, CircleShape)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.5.dp, colors.urgent, CircleShape)
                            .clickable {
                                capturedPhotoPath?.let { File(it).delete() }
                                capturedPhotoPath = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Retake,
                            contentDescription = "Chụp lại",
                            tint = colors.urgent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Right: Flip Camera Button (46x46 circular)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(enabled = !isCapturing && cameraState == CameraState.PREVIEWING) {
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
                        tint = if (cameraState == CameraState.PREVIEWING) Color.White else colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Completion Caption Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(20.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "GHI CHÚ KẾT QUẢ",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = completionCaption,
                    onValueChange = { completionCaption = it },
                    placeholder = { Text("Ví dụ: Đã dọn sạch góc học tập!", color = colors.textMuted, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedContainerColor = colors.elevated,
                        unfocusedContainerColor = colors.elevated,
                        focusedBorderColor = colors.flash,
                        unfocusedBorderColor = colors.border
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Save & Complete Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (capturedPhotoPath != null) SolidColor(colors.flash) else SolidColor(colors.elevated))
                        .clickable(enabled = capturedPhotoPath != null) {
                            val finalPath = capturedPhotoPath ?: ""
                            onProofCaptured(finalPath, completionCaption)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = SnapTaskIcons.Check,
                            contentDescription = "Hoàn thành",
                            tint = if (capturedPhotoPath != null) colors.onFlash else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lưu & Hoàn Thành",
                            color = if (capturedPhotoPath != null) colors.onFlash else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
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
}
