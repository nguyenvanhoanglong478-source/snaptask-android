package com.snaptask.app.ui.screens.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.ui.theme.ChipInactiveBg
import com.snaptask.app.ui.theme.ChipInactiveBorder
import com.snaptask.app.ui.theme.LocketAccent
import com.snaptask.app.ui.theme.NotionBorder
import com.snaptask.app.ui.theme.NotionCardSurface
import com.snaptask.app.ui.theme.NotionDarkBg
import com.snaptask.app.ui.theme.StatusDanger
import com.snaptask.app.ui.theme.StatusSuccess
import com.snaptask.app.ui.theme.TextMuted
import com.snaptask.app.ui.theme.TextPrimary
import com.snaptask.app.ui.theme.TextSecondary
import com.snaptask.app.util.DateTimeUtils
import com.snaptask.app.util.NotificationPermissionHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerDialog(
    initialReminderTime: Long?,
    initialReminderType: String,
    onConfirm: (reminderTime: Long?, reminderType: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf(initialReminderTime) }
    var selectedType by remember { mutableStateOf(if (initialReminderType == "NONE") "NOTIFICATION" else initialReminderType) }
    val notificationsEnabled = remember { NotificationPermissionHelper.areNotificationsEnabled(context) }

    fun setQuickSeconds(offsetSeconds: Int) {
        selectedTime = System.currentTimeMillis() + offsetSeconds * 1000L
    }

    fun setQuickTime(offsetMinutes: Int) {
        selectedTime = System.currentTimeMillis() + offsetMinutes * 60 * 1000L
    }

    fun setTargetTime(hour: Int, minute: Int, nextDayIfPast: Boolean) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (nextDayIfPast && timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        selectedTime = cal.timeInMillis
    }

    fun openDateTimePicker() {
        val currentCal = Calendar.getInstance().apply {
            selectedTime?.let { timeInMillis = it }
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timeCal = Calendar.getInstance()
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val picked = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                        }
                        selectedTime = picked.timeInMillis
                    },
                    currentCal.get(Calendar.HOUR_OF_DAY),
                    currentCal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NotionCardSurface,
        scrimColor = Color(0x99000000),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hẹn giờ nhắc nhở ⏰",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Đóng", tint = TextSecondary)
                }
            }

            // Notification permission warning if turned off in system settings
            if (!notificationsEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StatusDanger.copy(alpha = 0.15f))
                        .border(1.dp, StatusDanger, RoundedCornerShape(12.dp))
                        .clickable { NotificationPermissionHelper.openNotificationSettings(context) }
                        .padding(10.dp)
                ) {
                    Text(
                        text = "⚠️ Quyền thông báo đang TẮT. Chạm vào đây để bật trong Cài đặt!",
                        color = StatusDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Mode Selector (Notification vs Alarm)
            Text(
                text = "HÌNH THỨC NHẮC",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option Notification
                val isNotif = selectedType == "NOTIFICATION"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNotif) LocketAccent else ChipInactiveBg)
                        .border(1.dp, if (isNotif) LocketAccent else ChipInactiveBorder, RoundedCornerShape(14.dp))
                        .clickable { selectedType = "NOTIFICATION" }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notif",
                            tint = if (isNotif) NotionDarkBg else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Thông báo nhẹ",
                            color = if (isNotif) NotionDarkBg else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Option Alarm
                val isAlarm = selectedType == "ALARM"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isAlarm) LocketAccent else ChipInactiveBg)
                        .border(1.dp, if (isAlarm) LocketAccent else ChipInactiveBorder, RoundedCornerShape(14.dp))
                        .clickable { selectedType = "ALARM" }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Alarm,
                            contentDescription = "Alarm",
                            tint = if (isAlarm) NotionDarkBg else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Báo thức lớn 🔔",
                            color = if (isAlarm) NotionDarkBg else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Quick Presets
            Text(
                text = "CHỌN NHANH THỜI GIAN",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Highlighted 5s Quick Test Button (Step 5)
                val is5sSelected = selectedTime != null && (selectedTime!! - System.currentTimeMillis()) in 1..8000
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (is5sSelected) LocketAccent else LocketAccent.copy(alpha = 0.16f))
                        .border(1.5.dp, LocketAccent, RoundedCornerShape(12.dp))
                        .clickable { setQuickSeconds(5) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚡ TEST THỬ (SAU 5 GIÂY)",
                            color = if (is5sSelected) NotionDarkBg else LocketAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (is5sSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "✓ Đã chọn", color = NotionDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChipInactiveBg)
                            .border(1.dp, ChipInactiveBorder, RoundedCornerShape(12.dp))
                            .clickable { setQuickTime(15) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+15 phút", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChipInactiveBg)
                            .border(1.dp, ChipInactiveBorder, RoundedCornerShape(12.dp))
                            .clickable { setQuickTime(60) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+1 giờ", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChipInactiveBg)
                            .border(1.dp, ChipInactiveBorder, RoundedCornerShape(12.dp))
                            .clickable { setTargetTime(20, 0, nextDayIfPast = true) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tối nay (20:00)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChipInactiveBg)
                            .border(1.dp, ChipInactiveBorder, RoundedCornerShape(12.dp))
                            .clickable { setTargetTime(8, 0, nextDayIfPast = true) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sáng mai (08:00)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Custom Date/Time Picker Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NotionDarkBg)
                    .border(1.dp, NotionBorder, RoundedCornerShape(14.dp))
                    .clickable { openDateTimePicker() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Lịch", tint = LocketAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTime != null) "Thời gian: ${DateTimeUtils.formatDateTime(selectedTime!!)}" else "Tự chọn ngày & giờ...",
                            color = if (selectedTime != null) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTime != null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text("Thay đổi", color = LocketAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (!NotificationPermissionHelper.areNotificationsEnabled(context)) {
                        Toast.makeText(context, "⚠️ Cần bật quyền thông báo để chuông hoạt động!", Toast.LENGTH_LONG).show()
                        NotificationPermissionHelper.openNotificationSettings(context)
                    }
                    onConfirm(selectedTime, selectedType)
                },
                enabled = selectedTime != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocketAccent,
                    contentColor = NotionDarkBg,
                    disabledContainerColor = ChipInactiveBg,
                    disabledContentColor = TextMuted
                )
            ) {
                Text("Xác nhận hẹn giờ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (selectedTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        selectedTime = null
                        onConfirm(null, "NONE")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NotionBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Tắt báo thức / nhắc nhở", fontSize = 13.sp)
                }
            }
        }
    }
}
