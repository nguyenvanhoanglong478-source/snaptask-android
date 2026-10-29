package com.snaptask.app.ui.screens.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
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
    val colors = MaterialTheme.snapColors
    val isDark = isSystemInDarkTheme()
    // If it's a Quick Note without an image, display the dedicated Quick Note card
    if (task.isQuickNote || task.beforeImageUri.isBlank()) {
        QuickNoteFeedCard(
            task = task,
            onToggleStatus = onToggleStatus,
            onTogglePin = onTogglePin,
            onClick = onClick,
            modifier = modifier
        )
        return
    }

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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, if (task.isCompleted) colors.done.copy(alpha = 0.4f) else colors.border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(colors.bg)
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
 * Dedicated Quick Note Card for text-only tasks
 */
@Composable
fun QuickNoteFeedCard(
    task: Task,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isCheckedInternal by remember(task.isCompleted) { mutableStateOf(task.isCompleted) }
    val checkScale by animateFloatAsState(
        targetValue = if (isCheckedInternal) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "CheckScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, if (task.isCompleted) colors.done.copy(alpha = 0.35f) else colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Folder + Priority + Reminder + Pin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Folder badge
                    Box(
                        modifier = Modifier
                            .background(colors.elevated, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = task.folderName,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Priority badge
                    if (task.priorityEnum == TaskPriority.HIGH) {
                        Box(
                            modifier = Modifier
                                .background(if (isDark) colors.urgent.copy(alpha = 0.16f) else colors.urgent, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isDark) colors.urgent else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Gấp",
                                color = if (isDark) colors.urgent else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Reminder badge
                    if (task.hasReminder) {
                        Box(
                            modifier = Modifier
                                .background(colors.elevated, RoundedCornerShape(8.dp))
                                .border(1.dp, colors.borderHighlight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(SnapTaskIcons.AlarmClock, contentDescription = null, tint = colors.text, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = task.reminderFormatted ?: "",
                                    color = colors.text,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Pin Button (36dp round button)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.elevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(onClick = onTogglePin),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.Pin,
                        contentDescription = "Ghim",
                        tint = if (task.isPinnedToWidget) colors.flash else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title and Interactive Checkbox (24dp spaced 12dp from title)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(24.dp)
                        .scale(checkScale)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isCheckedInternal = !isCheckedInternal
                            onToggleStatus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Trạng thái",
                        tint = if (task.isCompleted) colors.done else colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = if (task.isCompleted) colors.textMuted else colors.text,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Subtasks checklist preview (up to 3 items)
            if (task.parsedSubtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.elevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    task.parsedSubtasks.take(3).forEach { sub ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (sub.isDone) Icons.Filled.Check else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (sub.isDone) colors.done else colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sub.title,
                                color = if (sub.isDone) colors.textMuted else colors.textSecondary,
                                fontSize = 12.sp,
                                textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (task.parsedSubtasks.size > 3) {
                        Text(
                            text = "+${task.parsedSubtasks.size - 3} việc nhỏ khác",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
    val haptic = LocalHapticFeedback.current

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
                        .background(if (isDark) colors.done.copy(alpha = 0.16f) else colors.done, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AFTER • HOÀN THÀNH",
                        color = if (isDark) colors.done else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(10.dp)) // Dark scrim overlay for legibility on camera photos
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

        // Bottom Scrim with Title & Caption
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(colors.scrim)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleStatus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Hoàn thành",
                        tint = colors.done,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!task.completionCaption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "\"${task.completionCaption}\"",
                            color = colors.done,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.glass)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(onClick = onTogglePin),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SnapTaskIcons.Pin,
                        contentDescription = "Ghim",
                        tint = if (task.isPinnedToWidget) colors.flash else Color.White,
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
    val haptic = LocalHapticFeedback.current

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
                        .background(if (isDark) colors.flash.copy(alpha = 0.2f) else colors.flash, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BEFORE • BẮT ĐẦU",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(10.dp)) // Dark scrim overlay for legibility on camera photos
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

        // Bottom Scrim with Title & Original Description
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(colors.scrim)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleStatus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Status",
                        tint = if (task.isCompleted) colors.done else colors.text,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Bắt đầu lúc: ${task.createdAtTimeFormatted}",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }

                // Flip back action pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC000000)) // Dark placeholder backdrop
                        .clickable(onClick = onToggleFlip)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Sync, contentDescription = "Lật lại", tint = colors.flash, modifier = Modifier.size(14.dp))
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
    val haptic = LocalHapticFeedback.current

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
                        .background(Color(0xCC000000), RoundedCornerShape(10.dp)) // Dark scrim overlay for legibility on camera photos
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.folderName.ifBlank { task.tagEnum.displayName },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (task.priorityEnum == TaskPriority.HIGH) {
                    Box(
                        modifier = Modifier
                            .background(if (isDark) colors.urgent.copy(alpha = 0.16f) else colors.urgent, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Gấp", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (task.hasReminder) {
                    Box(
                        modifier = Modifier
                            .background(if (isDark) colors.flash.copy(alpha = 0.2f) else colors.flash, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(SnapTaskIcons.AlarmClock, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = task.reminderFormatted ?: "", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.glass)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(onClick = onTogglePin),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SnapTaskIcons.Pin,
                    contentDescription = "Ghim",
                    tint = if (task.isPinnedToWidget) colors.flash else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Bottom Scrim Bar: 24dp Checkmark spaced 12dp from title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(colors.scrim)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleStatus()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = "Hoàn thành",
                    tint = if (task.isCompleted) colors.done else colors.text,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (task.isCompleted) colors.textMuted else colors.text,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.parsedSubtasks.isNotEmpty()) {
                    Text(
                        text = "Checklist: ${task.progressPercent}%",
                        color = colors.textSecondary,
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

        // Overlay Icon
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .size(20.dp)
                .background(Color(0xCC000000), CircleShape), // Dark circular overlay for flip icon over thumbnail
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
