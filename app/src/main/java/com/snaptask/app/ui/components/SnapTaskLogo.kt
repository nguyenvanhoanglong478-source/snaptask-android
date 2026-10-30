package com.snaptask.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    size: Dp = 32.dp,
    backgroundColor: Color = Color(0xFF10120A),
    ringColor: Color = MaterialTheme.snapColors.flash,
    diskColor: Color = MaterialTheme.snapColors.flash,
    tickColor: Color = Color(0xFF10120A)
) {
    val cornerRadius = size * (7.7f / 32f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val scale = w / 108f
            val center = Offset(w / 2f, this.size.height / 2f)

            drawCircle(
                color = ringColor,
                radius = 29.5f * scale,
                center = center,
                style = Stroke(width = 7f * scale)
            )

            drawCircle(
                color = diskColor,
                radius = 19f * scale,
                center = center
            )

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
}

@Composable
fun SnapTaskLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    backgroundColor: Color = Color(0xFF10120A)
) {
    SnapTaskLogo(
        modifier = modifier,
        size = size,
        backgroundColor = backgroundColor
    )
}
