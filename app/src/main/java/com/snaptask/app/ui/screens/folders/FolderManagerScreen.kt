package com.snaptask.app.ui.screens.folders

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.app.data.preferences.CustomFolder
import com.snaptask.app.data.preferences.FolderPreferencesRepository
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.ui.components.DockTab
import com.snaptask.app.ui.components.FloatingBottomDock
import com.snaptask.app.ui.components.RoundIconButton
import com.snaptask.app.ui.components.SnapSwitch
import com.snaptask.app.ui.components.SquareIconButton
import com.snaptask.app.ui.theme.SnapTaskIcons
import com.snaptask.app.ui.theme.snapColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagerScreen(
    tasks: List<Task>,
    folders: List<CustomFolder>,
    smartUrgentEnabled: Boolean,
    smartProofEnabled: Boolean,
    onToggleSmartUrgent: (Boolean) -> Unit,
    onToggleSmartProof: (Boolean) -> Unit,
    onMoveFolder: (index: Int, delta: Int) -> Unit,
    onSaveFolder: (editingId: String?, name: String, iconKey: String, colorHex: Long, makeDefault: Boolean) -> Unit,
    onDeleteFolder: (id: String) -> Unit,
    onOpenCamera: () -> Unit,
    onSelectDockTab: (DockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.snapColors
    val now = System.currentTimeMillis()

    val urgentCount = remember(tasks, now / 30_000L) {
        tasks.count {
            !it.isCompleted && (it.priorityEnum == TaskPriority.HIGH || (it.reminderTime != null && it.reminderTime < now))
        }
    }
    val proofCount = remember(tasks) {
        tasks.count { it.hasProof }
    }

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var editIconKey by remember { mutableStateOf("home") }
    var editColorHex by remember { mutableLongStateOf(0xFFD4FF3A) }
    var editMakeDefault by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    fun openNewFolderSheet() {
        editingId = null
        editName = ""
        editIconKey = FolderPreferencesRepository.ICON_KEYS.first()
        editColorHex = FolderPreferencesRepository.COLOR_PRESETS.first().hex
        editMakeDefault = folders.isEmpty()
        confirmingDelete = false
        showEditorSheet = true
    }

    fun openEditFolderSheet(folder: CustomFolder) {
        editingId = folder.id
        editName = folder.name
        editIconKey = folder.iconKey
        editColorHex = folder.colorHex
        editMakeDefault = folder.isDefault
        confirmingDelete = false
        showEditorSheet = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 136.dp)
        ) {
            item(key = "folders_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thư mục",
                        color = colors.text,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp
                    )

                    Box(
                        modifier = Modifier
                            .width(116.dp)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(colors.flash)
                            .clickable(role = Role.Button, onClick = ::openNewFolderSheet),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Tạo mới",
                            color = colors.onFlash,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            item(key = "smart_folders_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "THƯ MỤC THÔNG MINH (TỰ ĐỘNG GOM)",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(colors.urgentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = SnapTaskIcons.Clock,
                                    contentDescription = null,
                                    tint = colors.urgent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gấp & Trễ hạn",
                                    color = colors.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "$urgentCount việc đang cần chú ý",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            SnapSwitch(
                                checked = smartUrgentEnabled,
                                onCheckedChange = onToggleSmartUrgent,
                                contentDescription = "Bật thư mục thông minh Gấp & Trễ hạn"
                            )
                        }

                        HorizontalDivider(color = colors.border, thickness = 1.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(colors.elevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = SnapTaskIcons.CompareArrows,
                                    contentDescription = null,
                                    tint = colors.text,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Đủ ảnh Trước / Sau",
                                    color = colors.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "$proofCount cặp ảnh đã hoàn thành",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            SnapSwitch(
                                checked = smartProofEnabled,
                                onCheckedChange = onToggleSmartProof,
                                contentDescription = "Bật thư mục thông minh Trước / Sau"
                            )
                        }
                    }
                }
            }

            item(key = "custom_folders_title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THƯ MỤC CỦA TÔI · ${folders.size}",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Mũi tên để đổi thứ tự",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            itemsIndexed(folders, key = { _, f -> f.id }) { idx, folder ->
                val folderTasks = remember(tasks, folder.name) {
                    tasks.filter { it.folderName.equals(folder.name, ignoreCase = true) }
                }
                val openInFolder = remember(folderTasks) {
                    folderTasks.count { !it.isCompleted }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(46.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.BottomStart)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.elevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = SnapTaskIcons.folderIconByKey(folder.iconKey),
                                contentDescription = null,
                                tint = colors.text,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(Color(folder.colorHex))
                                .border(1.5.dp, colors.surface, CircleShape)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = folder.name,
                                color = colors.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (folder.isDefault) {
                                Box(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.flash)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Mặc định",
                                        color = colors.onFlash,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${folderTasks.size} việc · $openInFolder cần làm",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SquareIconButton(
                            icon = SnapTaskIcons.ChevronUp,
                            contentDescription = "Chuyển lên",
                            onClick = { onMoveFolder(idx, -1) },
                            size = 34.dp,
                            iconSize = 15.dp,
                            enabled = idx > 0
                        )
                        SquareIconButton(
                            icon = SnapTaskIcons.ChevronDown,
                            contentDescription = "Chuyển xuống",
                            onClick = { onMoveFolder(idx, 1) },
                            size = 34.dp,
                            iconSize = 15.dp,
                            enabled = idx < folders.lastIndex
                        )
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(colors.elevated)
                                .clickable(role = Role.Button) { openEditFolderSheet(folder) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sửa",
                                color = colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(key = "add_folder_dashed") {
                val dashColor = colors.borderHighlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button, onClick = ::openNewFolderSheet),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = dashColor,
                            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Plus,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Thêm thư mục mới",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(108.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.bg.copy(alpha = 0.94f), colors.bg)
                    )
                )
        )

        FloatingBottomDock(
            activeTab = DockTab.FOLDERS,
            onSelectTab = onSelectDockTab,
            onShutterClick = onOpenCamera,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )
    }

    if (showEditorSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showEditorSheet = false },
            sheetState = sheetState,
            containerColor = colors.surface,
            scrimColor = Color(0x99000000),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingId == null) "Thư mục mới" else "Chỉnh sửa thư mục",
                        color = colors.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    RoundIconButton(
                        icon = SnapTaskIcons.Close,
                        contentDescription = "Đóng",
                        onClick = { showEditorSheet = false },
                        size = 36.dp,
                        iconSize = 18.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.bg)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Xem trước nhãn",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .width(152.dp)
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(colors.elevated)
                            .border(1.dp, colors.border, CircleShape)
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
                                    .background(Color(editColorHex))
                            )
                            Icon(
                                imageVector = SnapTaskIcons.folderIconByKey(editIconKey),
                                contentDescription = null,
                                tint = colors.text,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = editName.trim().ifBlank { "Thư mục mới" },
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TÊN THƯ MỤC",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = editName,
                    onValueChange = { if (it.length <= 22) editName = it },
                    placeholder = {
                        Text(
                            text = "Ví dụ: Góc học tập",
                            color = colors.textMuted,
                            fontSize = 15.sp
                        )
                    },
                    textStyle = TextStyle(
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedContainerColor = colors.elevated,
                        unfocusedContainerColor = colors.elevated,
                        focusedBorderColor = colors.text,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "BIỂU TƯỢNG",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FolderPreferencesRepository.ICON_KEYS.chunked(4).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                val selected = key == editIconKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) colors.text else colors.elevated)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) colors.text else colors.border,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(role = Role.RadioButton) { editIconKey = key },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = SnapTaskIcons.folderIconByKey(key),
                                        contentDescription = key,
                                        tint = if (selected) colors.bg else colors.text,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MÀU NHẬN DIỆN",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FolderPreferencesRepository.COLOR_PRESETS.forEach { preset ->
                        val selected = preset.hex == editColorHex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(preset.hex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) colors.text else colors.border,
                                    shape = CircleShape
                                )
                                .clickable(
                                    role = Role.RadioButton,
                                    onClickLabel = preset.label
                                ) { editColorHex = preset.hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10120A))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.elevated)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mặc định khi mở Camera",
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gắn sẵn nhãn này cho ảnh việc mới",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    SnapSwitch(
                        checked = editMakeDefault,
                        onCheckedChange = { editMakeDefault = it },
                        contentDescription = "Đặt làm thư mục mặc định"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (confirmingDelete && editingId != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.urgentSoft)
                            .border(1.dp, colors.urgent, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Xóa thư mục này?",
                            color = colors.urgent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Các việc bên trong sẽ chuyển sang \"${FolderPreferencesRepository.UNASSIGNED_FOLDER_NAME}\", không mất ảnh hay lịch nhắc.",
                            color = colors.text,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, CircleShape)
                                    .clickable(role = Role.Button) { confirmingDelete = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Giữ lại",
                                    color = colors.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.urgent)
                                    .clickable(role = Role.Button) {
                                        val idToRemove = editingId
                                        showEditorSheet = false
                                        if (idToRemove != null) {
                                            onDeleteFolder(idToRemove)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Xác nhận xóa",
                                    color = colors.onUrgent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (editingId != null && folders.size > 1) {
                            Box(
                                modifier = Modifier
                                    .width(104.dp)
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .background(colors.urgentSoft)
                                    .border(1.dp, colors.urgent, CircleShape)
                                    .clickable(role = Role.Button) { confirmingDelete = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Xóa",
                                    color = colors.urgent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(colors.flash)
                                .clickable(role = Role.Button) {
                                    onSaveFolder(
                                        editingId,
                                        editName,
                                        editIconKey,
                                        editColorHex,
                                        editMakeDefault
                                    )
                                    showEditorSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Lưu thư mục",
                                color = colors.onFlash,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
