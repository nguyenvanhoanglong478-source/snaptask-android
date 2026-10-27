package com.snaptask.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.domain.model.TaskTag
import com.snaptask.app.ui.theme.BrandAmber
import com.snaptask.app.ui.theme.BrandGreen
import com.snaptask.app.ui.theme.BrandRed
import com.snaptask.app.ui.theme.Neutral100
import com.snaptask.app.ui.theme.Neutral500
import com.snaptask.app.ui.theme.Neutral700
import com.snaptask.app.ui.theme.Neutral900
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSnapSheet(
    imagePath: String,
    thumbnailPath: String,
    onSaveTask: (Task) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    var title by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(TaskTag.PERSONAL) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Tạo việc cần làm mới",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Photo Preview & Title Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (thumbnailPath.isNotBlank() && File(thumbnailPath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(thumbnailPath))
                            .build(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Bạn cần làm gì với ảnh này?") },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag Selection (Notion style)
            Text(
                text = "Danh mục",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 13.sp,
                color = Neutral700
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskTag.entries) { tag ->
                    val isSelected = selectedTag == tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Neutral900 else Neutral100)
                            .clickable { selectedTag = tag }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag.displayName,
                            color = if (isSelected) Color.White else Neutral700,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selection
            Text(
                text = "Mức độ ưu tiên",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 13.sp,
                color = Neutral700
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val (activeBg, activeFg) = when (priority) {
                        TaskPriority.LOW -> Pair(BrandGreen.copy(alpha = 0.15f), BrandGreen)
                        TaskPriority.MEDIUM -> Pair(BrandAmber.copy(alpha = 0.15f), BrandAmber)
                        TaskPriority.HIGH -> Pair(BrandRed.copy(alpha = 0.15f), BrandRed)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeBg else Neutral100)
                            .clickable { selectedPriority = priority }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = priority.label,
                            color = if (isSelected) activeFg else Neutral700,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    val task = Task(
                        title = title.ifBlank { "Việc cần làm" },
                        imagePath = imagePath,
                        thumbnailPath = thumbnailPath,
                        tag = selectedTag,
                        priority = selectedPriority
                    )
                    onSaveTask(task)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Neutral900)
            ) {
                Text(
                    text = "Lưu & Ghim vào Widget",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
