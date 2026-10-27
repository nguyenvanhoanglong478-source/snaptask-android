package com.snaptask.app.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.domain.model.TaskStatus
import com.snaptask.app.ui.theme.AlmondHearth
import com.snaptask.app.ui.theme.BrandAmber
import com.snaptask.app.ui.theme.BrandGreen
import com.snaptask.app.ui.theme.BrandRed
import com.snaptask.app.ui.theme.ObsidianElevated
import com.snaptask.app.ui.theme.VelvetCurfew
import com.snaptask.app.ui.theme.VelvetCurfewLight
import java.io.File

@Composable
fun TaskSnapCard(
    task: Task,
    onToggleStatus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ObsidianElevated.copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.dp, VelvetCurfewLight.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 1. Photo Container (Locket snapshot style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(VelvetCurfew)
            ) {
                if (task.thumbnailPath.isNotBlank() && File(task.thumbnailPath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(task.thumbnailPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = task.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.1f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Tag Chip over image (Notion style)
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Color(task.tag.hexColor).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.tag.displayName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Priority Indicator
                if (task.priority == TaskPriority.HIGH) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .background(BrandRed, shape = CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "GẤP",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 2. Task Details & Notion Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick completion toggle button (48x48dp touch area)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onToggleStatus),
                        contentAlignment = Alignment.Center
                    ) {
                        when (task.status) {
                            TaskStatus.DONE -> {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Hoàn thành",
                                    tint = BrandGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            TaskStatus.IN_PROGRESS -> {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = "Đang thực hiện",
                                    tint = BrandAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            TaskStatus.TODO -> {
                                Icon(
                                    imageVector = Icons.Outlined.Circle,
                                    contentDescription = "Cần làm",
                                    tint = AlmondHearth.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = task.title.ifBlank { "Nhiệm vụ không tên" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) AlmondHearth.copy(alpha = 0.45f) else AlmondHearth,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Subtasks counter if available
                if (task.subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tiến độ: ${task.completedSubtasksCount}/${task.subtasks.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlmondHearth.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
