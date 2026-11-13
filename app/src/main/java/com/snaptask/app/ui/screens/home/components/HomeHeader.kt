package com.snaptask.app.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.ui.components.RoundIconButton
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors

@Composable
fun HomeHeader(
    nowMillis: Long,
    isDarkTheme: Boolean,
    onOpenCalendar: (Long) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenRecap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { onOpenCalendar(nowMillis) }
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = SnapDateHelper.formatVietnameseDate(nowMillis),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = SnapTaskIcons.ChevronRight,
                    contentDescription = "Mở lịch tháng",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = "Hôm nay",
                color = colors.text,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.7).sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                icon = if (isDarkTheme) SnapTaskIcons.Sun else SnapTaskIcons.Moon,
                contentDescription = if (isDarkTheme) "Chuyển giao diện sáng" else "Chuyển giao diện tối",
                onClick = onToggleTheme,
                size = 40.dp,
                iconSize = 20.dp
            )
            RoundIconButton(
                icon = SnapTaskIcons.Flame,
                contentDescription = "Nhìn lại tuần",
                onClick = onOpenRecap,
                size = 40.dp,
                iconSize = 20.dp
            )
        }
    }
}
