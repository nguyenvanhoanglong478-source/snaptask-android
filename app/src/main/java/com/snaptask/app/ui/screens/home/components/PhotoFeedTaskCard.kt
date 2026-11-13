package com.snaptask.app.ui.screens.home.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.data.preferences.RepeatCadence
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors
import com.snaptask.app.util.DateTimeUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PHOTO_OVERLAY_LIGHT = Color(0xFFFAFAF6)
private val PHOTO_OVERLAY_MUTED = Color(0xFFD6D5CD)
private val PHOTO_OVERLAY_URGENT_TEXT = Color(0xFFFF8074)
private val PHOTO_OVERLAY_BUBBLE = Color(0xC20F0F0E)

@Composable
fun PhotoFeedTaskCard(
    task: Task,
    folder: CustomFolder?,
    repeatCadence: RepeatCadence,
    nowMillis: Long,
    isStamped: Boolean,
    stampScaleProvider: () -> Float,
    onCardClick: () -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val context = LocalContext.current
    val imageFile = remember(task.beforeImageUri) { File(task.beforeImageUri) }
    val validReminder = task.reminderTime?.takeIf { it > 0L }
    val validDue = task.dueDate?.takeIf { it > 0L }
    val isHighPriority = task.priorityEnum == TaskPriority.HIGH
    val isReminderOverdue = validReminder != null && validReminder < nowMillis
    val isDueOverdue = DateTimeUtils.isOverdue(validDue, task.isCompleted, nowMillis)
    val isOverdue = isReminderOverdue || isDueOverdue
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val statusPillText = when {
        isReminderOverdue && validReminder != null -> "Trễ ${formatOverdueDuration(nowMillis - validReminder)}"
        isDueOverdue && validDue != null -> "Quá hạn · ${DateTimeUtils.formatDueLabel(validDue, nowMillis)}"
        validReminder != null -> timeFormat.format(Date(validReminder))
        validDue != null -> "Hạn ${DateTimeUtils.formatDueLabel(validDue, nowMillis)}"
        isHighPriority -> "Gấp"
        else -> null
    }
    val isUrgentPill = isOverdue || statusPillText == "Gấp"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(34.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(34.dp))
            .clickable(onClick = onCardClick)
    ) {
        if (imageFile.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageFile)
                    .size(720)
                    .crossfade(true)
                    .build(),
                contentDescription = task.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A18)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = SnapTaskIcons.Camera,
                    contentDescription = null,
                    tint = Color(0xFFB0AFA7),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ảnh gốc không còn trên thiết bị",
                    color = Color(0xFFC8C7BF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Multi-stop scrim keeps both top pills and bottom title readable over bright photos.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x730A0A09),
                        0.24f to Color(0x1F0A0A09),
                        0.50f to Color.Transparent,
                        0.72f to Color(0x660A0A09),
                        1.0f to Color(0xE60A0A09)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (statusPillText != null) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(if (isUrgentPill) colors.urgent else PHOTO_OVERLAY_BUBBLE)
                            .border(
                                width = 1.dp,
                                color = if (isUrgentPill) Color.Transparent else Color(0x28FAFAF6),
                                shape = CircleShape
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isHighPriority && !isUrgentPill) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF6B5B))
                                )
                            } else {
                                Icon(
                                    imageVector = SnapTaskIcons.Clock,
                                    contentDescription = null,
                                    tint = if (isUrgentPill) colors.onUrgent else PHOTO_OVERLAY_LIGHT,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = statusPillText,
                                color = if (isUrgentPill) colors.onUrgent else PHOTO_OVERLAY_LIGHT,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            if (repeatCadence != RepeatCadence.NONE) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(PHOTO_OVERLAY_BUBBLE)
                        .border(1.dp, Color(0x28FAFAF6), CircleShape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Repeat,
                            contentDescription = null,
                            tint = PHOTO_OVERLAY_LIGHT,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = repeatCadence.badgeLabel,
                            color = PHOTO_OVERLAY_LIGHT,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(PHOTO_OVERLAY_BUBBLE)
                        .border(1.dp, Color(0x28FAFAF6), CircleShape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(folder?.colorHex ?: 0xFFD4FF3A))
                        )
                        Text(
                            text = task.folderName,
                            color = PHOTO_OVERLAY_LIGHT,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PHOTO_OVERLAY_BUBBLE)
                    .border(1.dp, Color(0x24FAFAF6), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val subs = task.parsedSubtasks
                val metaLine = when {
                    validDue != null && validReminder != null -> "Hạn ${DateTimeUtils.formatDueLabel(validDue, nowMillis)}"
                    subs.isNotEmpty() -> "${subs.count { it.isDone }}/${subs.size} bước nhỏ"
                    else -> null
                }
                val isMetaOverdue = validDue != null && validReminder != null && isDueOverdue

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = task.title,
                        color = PHOTO_OVERLAY_LIGHT,
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (metaLine != null) {
                        Text(
                            text = metaLine,
                            color = if (isMetaOverdue) PHOTO_OVERLAY_URGENT_TEXT else PHOTO_OVERLAY_MUTED,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.flash)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Đánh dấu hoàn thành",
                        onClick = onCompleteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SnapTaskIcons.Check,
                    contentDescription = "Hoàn thành",
                    tint = colors.onFlash,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isStamped) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x550A0A09)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val s = stampScaleProvider()
                            scaleX = s
                            scaleY = s
                            rotationZ = -8f
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.flash)
                        .border(4.dp, colors.onFlash, RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "XONG!",
                        color = colors.onFlash,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

internal fun formatOverdueDuration(diffMs: Long): String {
    val mins = (diffMs / 60_000L).coerceAtLeast(1L)
    return when {
        mins < 60L -> "$mins phút"
        mins < 1440L -> "${mins / 60L} giờ"
        else -> "${mins / 1440L} ngày"
    }
}
