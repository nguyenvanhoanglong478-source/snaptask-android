package com.snaptask.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snaptask.app.ui.theme.snapColors

@Composable
fun SnapTaskLogo(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    ringColor: Color = MaterialTheme.snapColors.flash,
    diskColor: Color = MaterialTheme.snapColors.flash,
    tickColor: Color = MaterialTheme.snapColors.onFlash
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val scale = w / 108f
        val center = Offset(w / 2f, h / 2f)

        // 1. Outer Ring (circle cx=54 cy=54 r=29.5, stroke 7)
        drawCircle(
            color = ringColor,
            radius = 29.5f * scale,
            center = center,
            style = Stroke(width = 7f * scale)
        )

        // 2. Inner Disk (circle cx=54 cy=54 r=19, fill)
        drawCircle(
            color = diskColor,
            radius = 19f * scale,
            center = center
        )

        // 3. Cutout Tick Mark (path "M45 55 L51.5 61.5 L63.5 47", stroke 6.5)
        val tickPath = Path().apply {
            moveTo(45f * scale, 55f * scale)
            lineTo(51.5f * scale, 61.5f * scale)
            lineTo(63.5f * scale, 47f * scale)
        }
        drawPath(
            tickPath,
            color = tickColor,
            style = Stroke(
                width = 6.5f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun SnapTaskLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    backgroundColor: Color = Color(0xFF17140A), // Ink background
    logoSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        SnapTaskLogo(size = logoSize)
    }
}
