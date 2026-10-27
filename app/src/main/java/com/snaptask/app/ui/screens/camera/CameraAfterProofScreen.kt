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
import com.snaptask.app.ui.theme.ChipInactiveBg
import com.snaptask.app.ui.theme.LocketAccent
import com.snaptask.app.ui.theme.NotionBorder
import com.snaptask.app.ui.theme.NotionCardSurface
import com.snaptask.app.ui.theme.NotionDarkBg
import com.snaptask.app.ui.theme.StatusSuccess
import com.snaptask.app.ui.theme.TextMuted
import com.snaptask.app.ui.theme.TextPrimary
import com.snaptask.app.ui.theme.TextSecondary
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

    // Instant Feedback States
    var showShutterFlash by remember { mutableStateOf(false) }
    var showScreenFlash by remember { mutableStateOf(false) }

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

    fun takeProofPhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true

        scope.launch {
            // Front camera screen flash
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

            val rawFile = File(context.cacheDir, "proof_${System.currentTimeMillis()}.jpg")
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
                    .background(NotionCardSurface, CircleShape)
                    .border(1.dp, NotionBorder, CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Đóng", tint = TextPrimary)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Xác nhận hoàn thành",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = task.title,
                    color = LocketAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            // Balance placeholder
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1:1 Camera Viewfinder with Squircle 32dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .border(2.dp, if (cameraState == CameraState.CAPTURED) StatusSuccess else NotionBorder, RoundedCornerShape(32.dp))
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

                // Ghost Overlay Toggle Pill (Top-Right over Viewfinder)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGhostOverlayEnabled) LocketAccent else Color(0xCC141414))
                        .border(1.dp, if (isGhostOverlayEnabled) LocketAccent else NotionBorder, RoundedCornerShape(12.dp))
                        .clickable { isGhostOverlayEnabled = !isGhostOverlayEnabled }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isGhostOverlayEnabled) "👻 Ghost: BẬT" else "👻 Ghost: TẮT",
                        color = if (isGhostOverlayEnabled) NotionDarkBg else TextPrimary,
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

                // Top Badge: "✅ AFTER PROOF READY"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✨ ĐÃ CHỤP AFTER",
                        color = StatusSuccess,
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
                            takeProofPhoto()
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

        // 3. Completion Caption Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NotionCardSurface, RoundedCornerShape(20.dp))
                .border(1.dp, NotionBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "GHI CHÚ KẾT QUẢ",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = completionCaption,
                onValueChange = { completionCaption = it },
                placeholder = { Text("Ví dụ: Đã dọn sạch góc học tập! ✨", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = NotionDarkBg,
                    unfocusedContainerColor = NotionDarkBg,
                    focusedBorderColor = LocketAccent,
                    unfocusedBorderColor = NotionBorder
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save & Complete Button (Fixed 52dp height, corner 16dp)
            Button(
                onClick = {
                    val finalPath = capturedPhotoPath ?: ""
                    onProofCaptured(finalPath, completionCaption)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = capturedPhotoPath != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocketAccent,
                    contentColor = NotionDarkBg,
                    disabledContainerColor = ChipInactiveBg,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Hoàn thành", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lưu & Hoàn Thành (After Proof) 🚀",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
