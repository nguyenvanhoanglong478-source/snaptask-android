package com.snaptask.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.domain.model.TaskTag
import com.snaptask.app.ui.theme.ChipActiveBg
import com.snaptask.app.ui.theme.ChipActiveText
import com.snaptask.app.ui.theme.ChipInactiveBg
import com.snaptask.app.ui.theme.ChipInactiveBorder
import com.snaptask.app.ui.theme.ChipInactiveText
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

enum class ViewMode {
    BENTO_GRID,
    FEED_FULL
}

@Composable
fun HomeScreen(
    tasks: List<Task>,
    onOpenLiveCamera: () -> Unit,
    onToggleTaskStatus: (Task) -> Unit,
    onTogglePin: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(ViewMode.BENTO_GRID) }
    var selectedTag by remember { mutableStateOf<TaskTag?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, selectedTag, searchQuery) {
        val currentTag = selectedTag
        tasks.filter { task ->
            val matchesTag = currentTag == null || task.tag == currentTag.name
            val matchesSearch = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true)
            matchesTag && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NotionDarkBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 6.dp)
            ) {
                // Header Bar: App Name + View Switcher + Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SnapTask",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                        Text(
                            text = "${tasks.count { !it.isCompleted }} việc cần làm",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier
                                .size(40.dp)
                                .background(NotionCardSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, NotionBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "Tìm kiếm", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }

                        // View Mode Switcher Pill (Bento vs Full)
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .background(NotionCardSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, NotionBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewMode = if (viewMode == ViewMode.BENTO_GRID) ViewMode.FEED_FULL else ViewMode.BENTO_GRID
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (viewMode == ViewMode.BENTO_GRID) Icons.Filled.GridOn else Icons.Filled.ViewAgenda,
                                    contentDescription = "Chuyển view",
                                    tint = LocketAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (viewMode == ViewMode.BENTO_GRID) "Bento" else "Feed",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Centered Locket Shutter Action Button
            FloatingActionButton(
                onClick = onOpenLiveCamera,
                containerColor = LocketAccent,
                contentColor = NotionDarkBg,
                shape = CircleShape,
                modifier = Modifier
                    .size(66.dp)
                    .border(3.dp, NotionCardSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Chụp Task Mới",
                    modifier = Modifier.size(30.dp),
                    tint = NotionDarkBg
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Expandable Search
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm kiếm task...", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = NotionCardSurface,
                        unfocusedContainerColor = NotionCardSurface,
                        focusedBorderColor = LocketAccent,
                        unfocusedBorderColor = NotionBorder
                    ),
                    singleLine = true
                )
            }

            // Tags Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isAll = selectedTag == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAll) ChipActiveBg else ChipInactiveBg)
                            .border(1.dp, if (isAll) LocketAccent else ChipInactiveBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedTag = null }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Tất cả",
                            color = if (isAll) ChipActiveText else ChipInactiveText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Feed Content
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Chưa có nhiệm vụ nào", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Bấm nút Camera để chụp khoảnh khắc và tạo task", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                when (viewMode) {
                    ViewMode.BENTO_GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredTasks, key = { it.id }) { task ->
                                BentoTaskCard(
                                    task = task,
                                    onToggleStatus = { onToggleTaskStatus(task) },
                                    onTogglePin = { onTogglePin(task) },
                                    onClick = { onTaskClick(task) }
                                )
                            }
                        }
                    }
                    ViewMode.FEED_FULL -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredTasks, key = { it.id }) { task ->
                                FullFeedTaskCard(
                                    task = task,
                                    onToggleStatus = { onToggleTaskStatus(task) },
                                    onTogglePin = { onTogglePin(task) },
                                    onClick = { onTaskClick(task) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoTaskCard(
    task: Task,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = NotionCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NotionBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF222222))
        ) {
            val photoFile = File(task.beforeImageUri)
            if (photoFile.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = task.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Top Row: Tag badge + Pin Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(text = task.tagEnum.displayName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xCC000000), CircleShape)
                ) {
                    Icon(
                        imageVector = if (task.isPinnedToWidget) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Ghim",
                        tint = if (task.isPinnedToWidget) LocketAccent else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Bottom Glassmorphism Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(GlassmorphismDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Hoàn thành",
                        tint = if (task.isCompleted) StatusSuccess else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = task.title,
                    color = if (task.isCompleted) TextMuted else TextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FullFeedTaskCard(
    task: Task,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = NotionCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NotionBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF222222))
        ) {
            val photoFile = File(task.beforeImageUri)
            if (photoFile.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoFile)
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
}
