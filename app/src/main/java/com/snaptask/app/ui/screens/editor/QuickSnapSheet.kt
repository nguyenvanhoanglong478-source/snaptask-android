package com.snaptask.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.snaptask.app.ui.theme.AlmondHearth
import com.snaptask.app.ui.theme.BrandAmber
import com.snaptask.app.ui.theme.BrandGreen
import com.snaptask.app.ui.theme.BrandRed
import com.snaptask.app.ui.theme.ChipActiveBackground
import com.snaptask.app.ui.theme.ChipActiveContent
import com.snaptask.app.ui.theme.ChipInactiveBackground
import com.snaptask.app.ui.theme.ChipInactiveBorder
import com.snaptask.app.ui.theme.ChipInactiveContent
import com.snaptask.app.ui.theme.ObsidianElevated
import com.snaptask.app.ui.theme.ObsidianInk
import com.snaptask.app.ui.theme.TextHighContrast
import com.snaptask.app.ui.theme.TextMediumContrast
import com.snaptask.app.ui.theme.TextMuted
import com.snaptask.app.ui.theme.VelvetCurfewLight
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
        containerColor = Color(0xFF1E1719), // Dark plum charcoal
        contentColor = TextHighContrast,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Text(
                text = "Tạo việc cần làm mới",
                style = MaterialTheme.typography.headlineMedium,
                color = TextHighContrast,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Photo Preview & Smart Title Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val previewFile = when {
                    thumbnailPath.isNotBlank() && File(thumbnailPath).exists() -> File(thumbnailPath)
                    imagePath.isNotBlank() && File(imagePath).exists() -> File(imagePath)
                    else -> null
                }

                if (previewFile != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(previewFile)
                            .build(),
                        contentDescription = "Ảnh công việc",
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, AlmondHearth, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { 
                        Text(
                            text = "Bạn cần làm gì với ảnh này?", 
                            color = TextMuted,
                            fontSize = 14.sp
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextHighContrast,
                        unfocusedTextColor = TextHighContrast,
                        focusedContainerColor = Color(0xFF281C21),
                        unfocusedContainerColor = Color(0xFF281C21),
                        focusedBorderColor = AlmondHearth,
                        unfocusedBorderColor = ChipInactiveBorder,
                        cursorColor = AlmondHearth
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tag Selection (High-Contrast Pills)
            Text(
                text = "Danh mục",
                color = TextMediumContrast,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskTag.entries) { tag ->
                    val isSelected = selectedTag == tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ChipActiveBackground else ChipInactiveBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AlmondHearth else ChipInactiveBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTag = tag }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tag.displayName,
                            color = if (isSelected) ChipActiveContent else ChipInactiveContent,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Priority Selection (Clear 3-way toggle)
            Text(
                text = "Mức độ ưu tiên",
                color = TextMediumContrast,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TaskPriority.entries.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val (colorAccent, label) = when (priority) {
                        TaskPriority.LOW -> Pair(BrandGreen, "Thấp")
                        TaskPriority.MEDIUM -> Pair(BrandAmber, "Vừa")
                        TaskPriority.HIGH -> Pair(BrandRed, "GẤP")
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) colorAccent.copy(alpha = 0.22f) else ChipInactiveBackground
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colorAccent else ChipInactiveBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedPriority = priority }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colorAccent else ChipInactiveContent,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Save Action Button
            Button(
                onClick = {
                    val task = Task(
                        title = title.ifBlank { "Nhiệm vụ không tên" },
                        beforeImageUri = imagePath,
                        tag = selectedTag.name,
                        priority = selectedPriority.name
                    )
                    onSaveTask(task)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlmondHearth,
                    contentColor = ObsidianInk
                )
            ) {
                Text(
                    text = "Lưu & Ghim Lên Widget",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianInk
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
