package com.snaptask.app.ui.screens.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.theme.GlassmorphismDark
import com.snaptask.app.ui.theme.LocketAccent
import com.snaptask.app.ui.theme.NotionBorder
import com.snaptask.app.ui.theme.NotionCardSurface
import com.snaptask.app.ui.theme.NotionDarkBg
import com.snaptask.app.ui.theme.StatusDanger
import com.snaptask.app.ui.theme.StatusSuccess
import com.snaptask.app.ui.theme.TextMuted
import com.snaptask.app.ui.theme.TextPrimary
import com.snaptask.app.ui.theme.TextSecondary
import java.io.File

@Composable
fun FlipTaskCard3D(
    task: Task,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current.density

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "Card3DFlipRotation"
    )

    // Trigger haptic feedback when rotation passes the 90-degree midpoint
    var hasHapticTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(rotation) {
        if (rotation in 86f..96f && !hasHapticTriggered) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hasHapticTriggered = true
        } else if (rotation < 80f || rotation > 100f) {
            hasHapticTriggered = false
        }
    }

    val hasProof = task.hasProof
    val beforeFile = remember(task.beforeImageUri) { File(task.beforeImageUri) }
    val afterFile = remember(task.afterImageUri) {
        if (!task.afterImageUri.isNullOrBlank()) File(task.afterImageUri) else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
            },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = NotionCardSurface),
        border = BorderStroke(1.dp, if (task.isCompleted) StatusSuccess.copy(alpha = 0.5f) else NotionBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF1E1E1E))
        ) {
            if (rotation <= 90f) {
                // FRONT FACE:
                // If hasProof: Show AFTER photo as main face with Before PiP
                // Else: Show BEFORE photo as main face
                if (hasProof && afterFile != null && afterFile.exists()) {
                    AfterFaceContent(
                        task = task,
                        afterFile = afterFile,
                        beforeFile = beforeFile,
                        onToggleFlip = onToggleFlip,
                        onToggleStatus = onToggleStatus,
                        onTogglePin = onTogglePin
                    )
                } else {
                    StandardBeforeFaceContent(
                        task = task,
                        beforeFile = beforeFile,
                        onToggleStatus = onToggleStatus,
                        onTogglePin = onTogglePin
                    )
                }
            } else {
                // BACK FACE (Rotated 180f to cancel mirroring):
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = 180f
                        }
                ) {
                    BeforeFlippedFaceContent(
                        task = task,
                        beforeFile = beforeFile,
                        afterFile = afterFile,
                        onToggleFlip = onToggleFlip,
                        onToggleStatus = onToggleStatus,
                        onTogglePin = onTogglePin
                    )
                }
            }
        }
    }
}

/**
 * Mặt AFTER: Hiển thị khi Task đã hoàn thành và có ảnh kết quả.
 */
@Composable
private fun AfterFaceContent(
    task: Task,
    afterFile: File,
    beforeFile: File,
    onToggleFlip: () -> Unit,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main After Photo
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(afterFile)
                .crossfade(true)
                .build(),
            contentDescription = "After Proof",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Top Row: Badge 'AFTER • DONE' on Left + Mini PiP on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Badge AFTER
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .background(StatusSuccess, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✨ AFTER • DONE",
                        color = NotionDarkBg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.durationFormatted,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mini PiP (Shows Before Thumbnail with Flip icon)
            MiniPipThumbnail(
                imageFile = beforeFile,
                label = "BEFORE",
                onClick = onToggleFlip
            )
        }

        // Bottom Glassmorphism Scrim with Title & Caption
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x80101010),
                            GlassmorphismDark
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Hoàn thành",
                        tint = StatusSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!task.completionCaption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "\"${task.completionCaption}\"",
                            color = LocketAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xCC000000), CircleShape)
                ) {
                    Icon(
                        imageVector = if (task.isPinnedToWidget) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Ghim",
                        tint = if (task.isPinnedToWidget) LocketAccent else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Mặt BEFORE khi lật ngược lại xem lúc bắt đầu.
 */
@Composable
private fun BeforeFlippedFaceContent(
    task: Task,
    beforeFile: File,
    afterFile: File?,
    onToggleFlip: () -> Unit,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Before Photo
        if (beforeFile.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(beforeFile)
                    .crossfade(true)
                    .build(),
                contentDescription = "Before Original",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top Row: Badge 'BEFORE • START' + Mini PiP (Shows After photo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .background(LocketAccent, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📌 BEFORE • START",
                        color = NotionDarkBg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.createdAtTimeFormatted,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mini PiP (Shows After Thumbnail with Flip icon)
            afterFile?.let {
                MiniPipThumbnail(
                    imageFile = it,
                    label = "AFTER",
                    onClick = onToggleFlip
                )
            }
        }

        // Bottom Glassmorphism Scrim with Title & Original Description
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x80101010),
                            GlassmorphismDark
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Status",
                        tint = if (task.isCompleted) StatusSuccess else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Bắt đầu lúc: ${task.createdAtTimeFormatted}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Flip back action pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC000000))
                        .clickable(onClick = onToggleFlip)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Sync, contentDescription = "Lật lại", tint = LocketAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("After", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Mặt hiển thị mặc định khi Task chưa hoàn thành (chưa có ảnh Proof).
 */
@Composable
private fun StandardBeforeFaceContent(
    task: Task,
    beforeFile: File,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (beforeFile.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(beforeFile)
                    .crossfade(true)
                    .build(),
                contentDescription = task.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = task.tagEnum.displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (task.priorityEnum == TaskPriority.HIGH) {
                    Box(
                        modifier = Modifier
                            .background(StatusDanger, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "GẤP 🔥", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            IconButton(
                onClick = onTogglePin,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xCC000000), CircleShape)
            ) {
                Icon(
                    imageVector = if (task.isPinnedToWidget) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Ghim",
                    tint = if (task.isPinnedToWidget) LocketAccent else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom Glassmorphism Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(GlassmorphismDark)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleStatus,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = "Hoàn thành",
                    tint = if (task.isCompleted) StatusSuccess else TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (task.isCompleted) TextMuted else TextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.parsedSubtasks.isNotEmpty()) {
                    Text(
                        text = "Checklist: ${task.progressPercent}%",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Mini PiP Thumbnail (64dp x 64dp, bo góc 16dp, viền trắng 1.5dp, có đổ bóng, icon xoay nhỏ)
 */
@Composable
private fun MiniPipThumbnail(
    imageFile: File,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        if (imageFile.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageFile)
                    .crossfade(true)
                    .build(),
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay Icon 🔄
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .size(20.dp)
                .background(Color(0xCC000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = "Lật",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
