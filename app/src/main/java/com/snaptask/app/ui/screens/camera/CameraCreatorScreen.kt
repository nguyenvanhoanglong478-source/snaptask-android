package com.snaptask.app.ui.screens.camera

import android.content.Context
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.domain.model.TaskTag
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
import com.snaptask.app.util.ImageCompressor
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraCreatorScreen(
    onSaveTask: (Task) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    // Task fields
    var title by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(TaskTag.PERSONAL) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var isPinned by remember { mutableStateOf(true) }
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }

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
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Đóng", tint = TextPrimary)
            }

            Text(
                text = "Tạo Task Mới",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            IconButton(
                onClick = { isFlashEnabled = !isFlashEnabled },
                modifier = Modifier
                    .size(40.dp)
                    .background(NotionCardSurface, CircleShape)
            ) {
                Icon(
                    imageVector = if (isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Flash",
                    tint = if (isFlashEnabled) LocketAccent else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Camera Viewfinder 1:1 Aspect Ratio with 32dp Squircle Corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .border(2.dp, NotionBorder, RoundedCornerShape(32.dp))
                .background(Color.Black),
            contentAlignment = Alignment.BottomCenter
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(if (isFlashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                            .build()

                        imageCapture = capture

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Shutter Button overlaid at bottom of camera frame
            Box(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(68.dp)
                    .border(3.5.dp, Color.White, CircleShape)
                    .padding(5.dp)
                    .background(LocketAccent, CircleShape)
                    .clickable {
                        val capture = imageCapture ?: return@clickable
                        if (isCapturing) return@clickable
                        isCapturing = true

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
                                            isCapturing = false
                                        } catch (_: Exception) {
                                            isCapturing = false
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Notion-style Quick Input & Metadata
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

            // Notion Tags Selector
            Text(
                text = "Danh mục (Tags)",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskTag.entries) { tag ->
                    val isSelected = selectedTag == tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ChipActiveBg else ChipInactiveBg)
                            .border(1.dp, if (isSelected) LocketAccent else ChipInactiveBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedTag = tag }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag.displayName,
                            color = if (isSelected) ChipActiveText else ChipInactiveText,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Priority Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mức độ ưu tiên",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        val isSelected = selectedPriority == p
                        val color = when (p) {
                            TaskPriority.LOW -> StatusSuccess
                            TaskPriority.MEDIUM -> StatusAmber
                            TaskPriority.HIGH -> StatusDanger
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) color.copy(alpha = 0.25f) else ChipInactiveBg)
                                .border(1.dp, if (isSelected) color else ChipInactiveBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedPriority = p }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = p.label,
                                color = if (isSelected) color else ChipInactiveText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pin to Widget Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPinned = !isPinned },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Ghim",
                    tint = if (isPinned) LocketAccent else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ghim thẳng ra Widget màn hình chính",
                    color = if (isPinned) TextPrimary else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isPinned) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Action Button
            Button(
                onClick = {
                    val finalPhoto = capturedPhotoPath ?: ""
                    val task = Task(
                        title = title.ifBlank { "Việc cần làm" },
                        beforeImageUri = finalPhoto,
                        tag = selectedTag.name,
                        priority = selectedPriority.name,
                        isPinnedToWidget = isPinned
                    )
                    onSaveTask(task)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
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
}
