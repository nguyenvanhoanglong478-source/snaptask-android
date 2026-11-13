package com.snaptask.app.ui.screens.home.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors

@Composable
fun FeedEmptyCard(
    isFirstRun: Boolean,
    filterKey: String,
    onClearFilter: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenQuickNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val isFilteredEmpty = !isFirstRun && filterKey != "all"
    val emptyIcon = when {
        filterKey == "done" -> SnapTaskIcons.Check
        filterKey == "urgent" -> SnapTaskIcons.Clock
        filterKey == "proof" -> SnapTaskIcons.CompareArrows
        filterKey.startsWith("folder:") -> SnapTaskIcons.Folder
        else -> SnapTaskIcons.Camera
    }
    val (headline, body) = when {
        isFirstRun -> Pair(
            "Chưa có việc nào",
            "Chụp nhanh góc cần làm hoặc tạo một ghi chú ngắn để bắt đầu ngày mới."
        )
        filterKey == "done" -> Pair(
            "Chưa có việc hoàn thành",
            "Đánh dấu xong một việc bất kỳ để lưu kết quả và ảnh Trước / Sau tại đây."
        )
        filterKey == "urgent" -> Pair(
            "Không có việc gấp hay trễ hạn",
            "Mọi lịch nhắc và hạn chót hiện tại đều đang ổn định."
        )
        filterKey == "proof" -> Pair(
            "Chưa có cặp ảnh Trước / Sau",
            "Chụp thêm ảnh kết quả khi hoàn thành để lưu thành thẻ so sánh."
        )
        filterKey.startsWith("folder:") -> Pair(
            "Thư mục này còn trống",
            "Chụp ảnh việc mới vào thư mục hoặc xóa bộ lọc để xem toàn bộ danh sách."
        )
        else -> Pair(
            "Hôm nay chưa có việc mới",
            "Các việc trước đây đã xong hết. Chụp việc mới hoặc thêm ghi chú để tiếp tục."
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.elevated)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = emptyIcon,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = headline,
            color = colors.text,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            color = colors.textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isFilteredEmpty) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.flash)
                        .clickable(role = Role.Button, onClick = onClearFilter),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Xóa bộ lọc",
                        color = colors.onFlash,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.elevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(role = Role.Button, onClick = onOpenCamera),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chụp việc mới",
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.flash)
                        .clickable(role = Role.Button, onClick = onOpenCamera),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chụp việc mới",
                        color = colors.onFlash,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.elevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(role = Role.Button, onClick = onOpenQuickNote),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ghi chú chữ",
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
