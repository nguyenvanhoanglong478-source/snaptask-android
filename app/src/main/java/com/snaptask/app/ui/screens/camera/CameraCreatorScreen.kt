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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.components.ViewfinderCornerBrackets
import com.snaptask.app.ui.screens.editor.ReminderPickerDialog
import com.snaptask.app.ui.screens.home.components.QuickNoteBottomSheet
import com.snaptask.app.ui.theme.Bricolage
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.SnapTaskTheme
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
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

private fun formatReminderChipLabel(reminderTime: Long?): String {
    if (reminderTime == null) return "Đặt giờ nhắc"
    val cal = Calendar.getInstance()
    val nowDay = cal.get(Calendar.DAY_OF_YEAR)
    val nowYear = cal.get(Calendar.YEAR)

    cal.timeInMillis = reminderTime
    val remDay = cal.get(Calendar.DAY_OF_YEAR)
    val remYear = cal.get(Calendar.YEAR)

    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminderTime))
    return when {
        nowYear == remYear && nowDay == remDay -> "Hôm nay $timeStr"
        nowYear == remYear && remDay == nowDay + 1 -> "Ngày mai $timeStr"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(reminderTime))
    }
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

    // Form data
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    val folderOptions = remember { listOf("Cá nhân", "Công việc", "Nhà") }
    var selectedFolder by remember { mutableStateOf("Cá nhân") }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var reminderType by remember { mutableStateOf("NOTIFICATION") }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showQuickNoteSheet by remember { mutableStateOf(false) }

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
            } else {
                capturedPhotoPath?.let { path ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(path))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Ảnh đã chụp",
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
        // LỚP 3: 4 GÓC NGẮM & CHIP GỢI Ý
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
                        text = "Chụp thứ cần làm, đặt tên sau cũng được",
                        color = Color(0xFFF5F1E8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Chip nhãn khi đã chụp: kiểu chip gợi ý
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
                        text = "Đã chụp",
                        color = colors.flash,
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

            // Tiêu đề: Bricolage SemiBold 17/24
            Text(
                text = "Việc mới",
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = Color(0xFFF5F1E8),
                modifier = Modifier.align(Alignment.Center)
            )

            // Nút flash: 44dp tròn, KHÔNG viền, bật = nền flash + icon onFlash, tắt = nền rgba(12,11,10,0.6) + icon trắng
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
            // 1. Ô tên việc: cao 52, bo 16, nền rgba(27,26,23,0.92), viền 1dp #34312B, padding ngang 16, icon bút 20 #8F897B
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
                        if (title.isEmpty()) {
                            Text(
                                text = "Tên việc, ví dụ: Tưới cây",
                                color = Color(0xFF8F897B),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
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

            // 2. Hàng chip: cao 36, bo TRÒN ĐẦY ĐỦ, padding ngang 14, chữ 13sp, cách nhau 8
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip 1: Giờ nhắc (icon vector 16dp, BỎ emoji)
                val hasReminder = reminderTime != null
                val reminderLabel = formatReminderChipLabel(reminderTime)

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(if (hasReminder) colors.flash else Color(0xFF26241F))
                        .border(
                            1.dp,
                            if (hasReminder) Color.Transparent else Color(0xFF34312B),
                            CircleShape
                        )
                        .clickable { showReminderPicker = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = if (hasReminder) colors.onFlash else Color(0xFFF5F1E8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = reminderLabel,
                            color = if (hasReminder) colors.onFlash else Color(0xFFF5F1E8),
                            fontSize = 13.sp,
                            fontWeight = if (hasReminder) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }

                // Chip 2: Ưu tiên (trung tính #26241F, chữ #F5F1E8, KHÔNG vàng cho "Vừa")
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF26241F))
                        .border(1.dp, Color(0xFF34312B), CircleShape)
                        .clickable {
                            val nextIndex = (selectedPriority.ordinal + 1) % TaskPriority.entries.size
                            selectedPriority = TaskPriority.entries[nextIndex]
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ưu tiên: ${selectedPriority.label}",
                        color = Color(0xFFF5F1E8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Chip 3: Thư mục (trung tính #26241F, chữ #F5F1E8)
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF26241F))
                        .border(1.dp, Color(0xFF34312B), CircleShape)
                        .clickable {
                            val nextIdx = (folderOptions.indexOf(selectedFolder) + 1) % folderOptions.size
                            selectedFolder = folderOptions[nextIdx]
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedFolder,
                        color = Color(0xFFF5F1E8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 3. Hàng nút chụp (padding trên 6, ngang 8, trải đều): nút phụ 52x52 bo 16 nền #26241F viền #34312B, nút chụp giữa 88dp, nút phụ 52x52
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút phụ trái 52x52 bo 16
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF26241F))
                        .border(1.dp, Color(0xFF34312B), RoundedCornerShape(16.dp))
                        .clickable {
                            if (cameraState == CameraState.CAPTURED) {
                                capturedPhotoPath?.let { File(it).delete() }
                                capturedPhotoPath = null
                            } else {
                                showQuickNoteSheet = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (cameraState == CameraState.CAPTURED) Icons.Filled.Refresh else Icons.Filled.Notes,
                        contentDescription = if (cameraState == CameraState.CAPTURED) "Chụp lại" else "Ghi chú nhanh",
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
                            if (cameraState == CameraState.PREVIEWING) {
                                takePhoto()
                            } else {
                                val finalPath = capturedPhotoPath ?: ""
                                onSaveTask(
                                    Task(
                                        id = 0,
                                        title = title.ifBlank { "Việc mới ${DateTimeUtils.formatTime(System.currentTimeMillis())}" },
                                        beforeImageUri = finalPath,
                                        folderName = selectedFolder,
                                        priority = selectedPriority.name,
                                        reminderTime = reminderTime,
                                        reminderType = reminderType,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                onClose()
                            }
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
                                .background(colors.flash),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cameraState == CameraState.CAPTURED) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Lưu việc",
                                    tint = colors.onFlash,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Nút phụ phải 52x52 bo 16 (Đổi camera)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF26241F))
                        .border(1.dp, Color(0xFF34312B), RoundedCornerShape(16.dp))
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
                        imageVector = Icons.Filled.FlipCameraAndroid,
                        contentDescription = "Đổi camera",
                        tint = if (cameraState == CameraState.PREVIEWING) Color(0xFFF5F1E8) else Color(0xFF8F897B),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showReminderPicker) {
        ReminderPickerDialog(
            initialReminderTime = reminderTime,
            initialReminderType = reminderType,
            onDismiss = { showReminderPicker = false },
            onConfirm = { time, type ->
                reminderTime = time
                reminderType = type
                showReminderPicker = false
            }
        )
    }

    if (showQuickNoteSheet) {
        QuickNoteBottomSheet(
            onDismiss = { showQuickNoteSheet = false },
            onSave = { quickTask ->
                onSaveTask(quickTask)
                showQuickNoteSheet = false
                onClose()
            }
        )
    }
}

@ComposePreview(widthDp = 390, heightDp = 844)
@Composable
fun PreviewCameraCreatorScreen() {
    SnapTaskTheme(darkTheme = true) {
        CameraCreatorScreen(
            onSaveTask = {},
            onClose = {}
        )
    }
}
