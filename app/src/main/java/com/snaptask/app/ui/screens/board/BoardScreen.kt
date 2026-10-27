package com.snaptask.app.ui.screens.board

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskStatus
import com.snaptask.app.ui.screens.home.components.TaskSnapCard
import com.snaptask.app.ui.theme.AlmondHearth
import com.snaptask.app.ui.theme.ChipInactiveBackground
import com.snaptask.app.ui.theme.ChipInactiveBorder
import com.snaptask.app.ui.theme.SnapTaskVerticalGradient
import com.snaptask.app.ui.theme.TextHighContrast
import com.snaptask.app.ui.theme.TextMediumContrast
import com.snaptask.app.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BoardScreen(
    tasks: List<Task>,
    onToggleTaskStatus: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = TaskStatus.entries
    val pagerState = rememberPagerState(pageCount = { statuses.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SnapTaskVerticalGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Bảng Cột Tiến Độ (Board)",
                            fontWeight = FontWeight.Bold,
                            color = TextHighContrast,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = TextHighContrast
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // High-Contrast Tab Row
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    contentColor = AlmondHearth,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = AlmondHearth,
                            height = 3.dp
                        )
                    }
                ) {
                    statuses.forEachIndexed { index, status ->
                        val count = tasks.count { it.status == status }
                        val isSelected = pagerState.currentPage == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = status.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) TextHighContrast else TextMuted,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) AlmondHearth else ChipInactiveBackground,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF151311) else TextHighContrast
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val currentStatus = statuses[page]
                    val columnTasks = tasks.filter { it.status == currentStatus }

                    if (columnTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không có việc nào trong cột '${currentStatus.label}'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMediumContrast,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(columnTasks, key = { it.id }) { task ->
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
}
