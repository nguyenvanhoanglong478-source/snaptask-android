package com.snaptask.app.ui.screens.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snaptask.app.domain.model.Task
import com.snaptask.app.ui.components.RoundIconButton
import com.snaptask.app.ui.components.SnapDateHelper
import com.snaptask.app.ui.theme.SnapTaskIcons
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ShareTarget {
    STORY,
    ZALO,
    SAVE_GALLERY,
    MORE
}

@Composable
fun ShareCardScreen(
    task: Task,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var formatIndex by rememberSaveable { mutableIntStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }

    val beforeFile = remember(task.beforeImageUri) {
        if (task.beforeImageUri.isNotBlank()) File(task.beforeImageUri) else null
    }
    val afterFile = remember(task.afterImageUri) {
        if (!task.afterImageUri.isNullOrBlank()) File(task.afterImageUri) else null
    }

    val dateLabel = remember(task.completedAt, task.createdAt) {
        val df = SimpleDateFormat("dd/MM", Locale.getDefault())
        val mins = SnapDateHelper.durationMinutes(task)
        if (task.isCompleted) {
            "${df.format(Date(task.completedAt ?: task.createdAt))} · $mins phút"
        } else {
            df.format(Date(task.createdAt))
        }
    }

    fun exportAndDispatch(target: ShareTarget) {
        if (isExporting) return
        scope.launch {
            isExporting = true
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    renderSharePosterBitmap(
                        task = task,
                        dateLabel = dateLabel,
                        formatIndex = formatIndex
                    )
                }

                if (target == ShareTarget.SAVE_GALLERY) {
                    val savedOk = withContext(Dispatchers.IO) {
                        saveBitmapToGallery(context, bitmap, "snaptask_${task.id}_${System.currentTimeMillis()}.png")
                    }
                    bitmap.recycle()
                    val msg = if (savedOk) {
                        "Đã lưu thẻ Trước / Sau vào Thư viện ảnh!"
                    } else {
                        "Không thể lưu ảnh vào bộ nhớ thiết bị."
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    val uri = withContext(Dispatchers.IO) {
                        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
                        val outFile = File(dir, "snaptask_card_${task.id}.png")
                        FileOutputStream(outFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            outFile
                        )
                    }
                    bitmap.recycle()

                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, "${task.title} ($dateLabel)")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    when (target) {
                        ShareTarget.ZALO -> {
                            val hasZalo = context.packageManager.getLaunchIntentForPackage("com.zing.zalo") != null
                            if (hasZalo) {
                                sendIntent.setPackage("com.zing.zalo")
                                runCatching { context.startActivity(sendIntent) }.onFailure {
                                    sendIntent.setPackage(null)
                                    context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ qua Zalo"))
                                }
                            } else {
                                context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ thẻ Trước / Sau"))
                            }
                        }
                        ShareTarget.STORY -> {
                            context.startActivity(Intent.createChooser(sendIntent, "Đăng lên Story"))
                        }
                        else -> {
                            context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ thẻ Trước / Sau"))
                        }
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Không tạo được ảnh chia sẻ, vui lòng thử lại.", Toast.LENGTH_SHORT).show()
            } finally {
                isExporting = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A09))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                icon = SnapTaskIcons.Close,
                contentDescription = "Đóng",
                onClick = onClose,
                containerColor = Color(0xFF1F1F1C),
                contentColor = Color(0xFFFAFAF6)
            )
            Text(
                text = "Chia sẻ",
                color = Color(0xFFFAFAF6),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(44.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardAspect = if (formatIndex == 0) 9f / 13.8f else 1f
            Column(
                modifier = Modifier
                    .aspectRatio(cardAspect, matchHeightConstraintsFirst = formatIndex == 0)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFD4FF3A))
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "snaptask",
                        color = Color(0xFF10120A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = dateLabel,
                        color = Color(0xFF10120A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (formatIndex == 2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosterPhotoSlot(
                            file = beforeFile,
                            tagText = "TRƯỚC",
                            isAfter = false,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        PosterPhotoSlot(
                            file = afterFile ?: beforeFile,
                            tagText = if (afterFile != null && afterFile.exists()) "SAU" else "HIỆN TẠI",
                            isAfter = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosterPhotoSlot(
                            file = beforeFile,
                            tagText = "TRƯỚC",
                            isAfter = false,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        PosterPhotoSlot(
                            file = afterFile ?: beforeFile,
                            tagText = if (afterFile != null && afterFile.exists()) "SAU" else "HIỆN TẠI",
                            isAfter = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        color = Color(0xFF10120A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10120A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SnapTaskIcons.Check,
                            contentDescription = null,
                            tint = Color(0xFFD4FF3A),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xD910120A))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFD4FF3A),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Đang xuất ảnh…",
                            color = Color(0xFFFAFAF6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Dọc 9:16", "Vuông 1:1", "Cạnh nhau").forEachIndexed { idx, label ->
                    val selected = idx == formatIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color(0xFFFAFAF6) else Color(0xFF1F1F1C))
                            .border(
                                width = 1.dp,
                                color = if (selected) Color(0xFFFAFAF6) else Color(0xFF34342F),
                                shape = CircleShape
                            )
                            .clickable(role = Role.Tab) { formatIndex = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color(0xFF0F0F0E) else Color(0xFFFAFAF6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShareActionColumn(
                    icon = SnapTaskIcons.ShareUp,
                    label = "Story",
                    onClick = { exportAndDispatch(ShareTarget.STORY) }
                )
                ShareActionColumn(
                    icon = SnapTaskIcons.ShareUp,
                    label = "Zalo",
                    onClick = { exportAndDispatch(ShareTarget.ZALO) }
                )
                ShareActionColumn(
                    icon = SnapTaskIcons.Download,
                    label = "Lưu ảnh",
                    onClick = { exportAndDispatch(ShareTarget.SAVE_GALLERY) }
                )
                ShareActionColumn(
                    icon = SnapTaskIcons.MoreHorizontal,
                    label = "Khác",
                    onClick = { exportAndDispatch(ShareTarget.MORE) }
                )
            }
        }
    }
}

@Composable
private fun PosterPhotoSlot(
    file: File?,
    tagText: String,
    isAfter: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF171715))
    ) {
        if (file != null && file.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = tagText,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có ảnh",
                    color = Color(0xFFB0AFA7),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .height(22.dp)
                .clip(CircleShape)
                .background(if (isAfter) Color(0xFFD4FF3A) else Color(0xBF0F0F0E))
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tagText,
                color = if (isAfter) Color(0xFF10120A) else Color(0xFFFAFAF6),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ShareActionColumn(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF252522))
                .border(1.dp, Color(0xFF34342F), CircleShape)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFFAFAF6),
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            color = Color(0xFFB0AFA7),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun renderSharePosterBitmap(
    task: Task,
    dateLabel: String,
    formatIndex: Int
): Bitmap {
    val width = 1080
    val height = if (formatIndex == 0) 1680 else 1080
    val poster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(poster)

    val limePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD4FF3A.toInt() }
    val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10120A.toInt() }
    val slotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF171715.toInt() }

    canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 72f, 72f, limePaint)

    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF10120A.toInt()
        textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF10120A.toInt()
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }

    val pad = 52f
    canvas.drawText("snaptask", pad, 94f, brandPaint)
    canvas.drawText(dateLabel, width - pad, 92f, datePaint)

    val contentTop = 132f
    val contentBottom = height - 138f
    val gap = 24f

    val beforeBmp = decodeSampledFile(task.beforeImageUri, 1080)
    val afterPath = if (!task.afterImageUri.isNullOrBlank()) task.afterImageUri else task.beforeImageUri
    val afterBmp = decodeSampledFile(afterPath, 1080)
    val hasDistinctAfter = !task.afterImageUri.isNullOrBlank() && File(task.afterImageUri).exists()

    if (formatIndex == 2) {
        val slotW = (width - pad * 2 - gap) / 2f
        val leftRect = RectF(pad, contentTop, pad + slotW, contentBottom)
        val rightRect = RectF(pad + slotW + gap, contentTop, width - pad, contentBottom)
        drawPhotoSlotOnCanvas(canvas, leftRect, beforeBmp, "TRƯỚC", isAfter = false, slotBgPaint)
        drawPhotoSlotOnCanvas(
            canvas,
            rightRect,
            afterBmp,
            if (hasDistinctAfter) "SAU" else "HIỆN TẠI",
            isAfter = true,
            slotBgPaint
        )
    } else {
        val slotH = (contentBottom - contentTop - gap) / 2f
        val topRect = RectF(pad, contentTop, width - pad, contentTop + slotH)
        val bottomRect = RectF(pad, contentTop + slotH + gap, width - pad, contentBottom)
        drawPhotoSlotOnCanvas(canvas, topRect, beforeBmp, "TRƯỚC", isAfter = false, slotBgPaint)
        drawPhotoSlotOnCanvas(
            canvas,
            bottomRect,
            afterBmp,
            if (hasDistinctAfter) "SAU" else "HIỆN TẠI",
            isAfter = true,
            slotBgPaint
        )
    }

    beforeBmp?.recycle()
    afterBmp?.recycle()

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF10120A.toInt()
        textSize = 48f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val clippedTitle = if (task.title.length > 34) task.title.take(33) + "…" else task.title
    canvas.drawText(clippedTitle, pad, height - 58f, titlePaint)

    val badgeCx = width - pad - 34f
    val badgeCy = height - 74f
    canvas.drawCircle(badgeCx, badgeCy, 34f, inkPaint)
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD4FF3A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val tickPath = Path().apply {
        moveTo(badgeCx - 14f, badgeCy + 1f)
        lineTo(badgeCx - 3f, badgeCy + 12f)
        lineTo(badgeCx + 15f, badgeCy - 10f)
    }
    canvas.drawPath(tickPath, tickPaint)

    return poster
}

private fun drawPhotoSlotOnCanvas(
    canvas: Canvas,
    rect: RectF,
    bmp: Bitmap?,
    tag: String,
    isAfter: Boolean,
    bgPaint: Paint
) {
    val clipPath = Path().apply {
        addRoundRect(rect, 46f, 46f, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(clipPath)
    canvas.drawRect(rect, bgPaint)

    if (bmp != null) {
        val scale = max(rect.width() / bmp.width.toFloat(), rect.height() / bmp.height.toFloat())
        val dw = bmp.width * scale
        val dh = bmp.height * scale
        val left = rect.centerX() - dw / 2f
        val top = rect.centerY() - dh / 2f
        val dst = RectF(left, top, left + dw, top + dh)
        canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    val tagTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isAfter) 0xFF10120A.toInt() else 0xFFFAFAF6.toInt()
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textW = tagTextPaint.measureText(tag)
    val pillRect = RectF(
        rect.left + 22f,
        rect.top + 22f,
        rect.left + 22f + textW + 44f,
        rect.top + 80f
    )
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isAfter) 0xFFD4FF3A.toInt() else 0xCC0F0F0E.toInt()
    }
    canvas.drawRoundRect(pillRect, 29f, 29f, pillPaint)
    canvas.drawText(tag, pillRect.left + 22f, pillRect.centerY() + 10f, tagTextPaint)
    canvas.restore()
}

private fun decodeSampledFile(path: String?, maxDim: Int): Bitmap? {
    if (path.isNullOrBlank()) return null
    val f = File(path)
    if (!f.exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (bounds.outWidth / sample > maxDim * 2 || bounds.outHeight / sample > maxDim * 2) {
        sample *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(f.absolutePath, decodeOpts)
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    return runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SnapTask")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val doneValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, doneValues, null, null)
        }
        true
    }.getOrDefault(false)
}
