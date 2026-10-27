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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskTag
import com.snaptask.app.ui.screens.home.components.TaskSnapCard
import com.snaptask.app.ui.theme.AlmondHearth
import com.snaptask.app.ui.theme.ChipActiveBackground
import com.snaptask.app.ui.theme.ChipActiveContent
import com.snaptask.app.ui.theme.ChipInactiveBackground
import com.snaptask.app.ui.theme.ChipInactiveBorder
import com.snaptask.app.ui.theme.ChipInactiveContent
import com.snaptask.app.ui.theme.ObsidianInk
import com.snaptask.app.ui.theme.SnapTaskVerticalGradient
import com.snaptask.app.ui.theme.TextHighContrast
import com.snaptask.app.ui.theme.TextMediumContrast
import com.snaptask.app.ui.theme.TextMuted
import com.snaptask.app.ui.theme.VelvetCurfew

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tasks: List<Task>,
    onOpenLiveCamera: () -> Unit,
    onOpenBoardView: () -> Unit,
    onToggleTaskStatus: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTag by remember { mutableStateOf<TaskTag?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, selectedTag, searchQuery) {
        tasks.filter { task ->
            val matchesTag = selectedTag == null || task.tag == selectedTag
            val matchesSearch = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true)
            matchesTag && matchesSearch
        }
    }

    val pendingCount = tasks.count { !it.isCompleted }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SnapTaskVerticalGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 8.dp)
                ) {
                    // Smart Top Row: Title + Quick View Switcher + Search
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SnapTask",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextHighContrast,
                                fontSize = 26.sp
                            )
                            Text(
                                text = if (pendingCount > 0) "$pendingCount việc cần giải quyết" else "Tất cả đã hoàn thành ✨",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumContrast,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }

                        // Smart Tool Bar Pill (Search & Board Switcher)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Search Toggle Button
                            IconButton(
                                onClick = { isSearchActive = !isSearchActive },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(ChipInactiveBackground, RoundedCornerShape(12.dp))
                                    .border(1.dp, ChipInactiveBorder, RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Tìm kiếm",
                                    tint = TextHighContrast,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // View Mode Pill (Switch to Board View)
                            Box(
                                modifier = Modifier
                                    .height(42.dp)
                                    .background(ChipInactiveBackground, RoundedCornerShape(12.dp))
                                    .border(1.dp, ChipInactiveBorder, RoundedCornerShape(12.dp))
                                    .clickable(onClick = onOpenBoardView)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.ViewKanban,
                                        contentDescription = "Bảng Cột",
                                        tint = AlmondHearth,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bảng Cột",
                                        color = TextHighContrast,
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
                // Centered Smart Shutter Action Bar (Locket-style)
                FloatingActionButton(
                    onClick = onOpenLiveCamera,
                    containerColor = AlmondHearth,
                    contentColor = ObsidianInk,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(68.dp)
                        .border(4.dp, VelvetCurfew, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Chụp việc mới",
                        modifier = Modifier.size(32.dp),
                        tint = ObsidianInk
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Expandable Search Input
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm kiếm việc cần làm...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextHighContrast,
                            unfocusedTextColor = TextHighContrast,
                            focusedContainerColor = Color(0xFF281C21),
                            unfocusedContainerColor = Color(0xFF281C21),
                            focusedBorderColor = AlmondHearth,
                            unfocusedBorderColor = ChipInactiveBorder
                        ),
                        singleLine = true
                    )
                }

                // High-Contrast Tag Filter Chips (Notion style)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isAllSelected = selectedTag == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAllSelected) ChipActiveBackground else ChipInactiveBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isAllSelected) AlmondHearth else ChipInactiveBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTag = null }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Tất cả",
                                color = if (isAllSelected) ChipActiveContent else ChipInactiveContent,
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
                                .background(if (isSelected) ChipActiveBackground else ChipInactiveBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AlmondHearth else ChipInactiveBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTag = tag }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = tag.displayName,
                                color = if (isSelected) ChipActiveContent else ChipInactiveContent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Grid View of Snap Cards
                if (filteredTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Chưa có việc nào ở đây",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextHighContrast,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Chạm vào máy ảnh phía dưới để chụp và ghim việc cần làm",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumContrast
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredTasks, key = { it.id }) { task ->
                            TaskSnapCard(
                                task = task,
                                onToggleStatus = { onToggleTaskStatus(task) },
                                onClick = { onTaskClick(task) }
                            )
                        }
                    }
                }
            }
        }
    }
}
