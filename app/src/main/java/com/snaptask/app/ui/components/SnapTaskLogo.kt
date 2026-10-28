package com.snaptask.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snaptask.app.ui.theme.AccentPrimary
import com.snaptask.app.ui.theme.StatusSuccess
import com.snaptask.app.ui.theme.TextPrimary

@Composable
fun SnapTaskLogo(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    bracketColor: Color = TextPrimary,
    lensColor: Color = AccentPrimary,
    checkColor: Color = StatusSuccess
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val strokeWidth = (w * 0.085f).coerceAtLeast(1.8f)
        val cornerArm = w * 0.24f
        val cornerArc = w * 0.10f

        val strokeStyle = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // 1. Top-Left Viewfinder Bracket
        val pathTL = Path().apply {
            moveTo(cornerArm, strokeWidth / 2)
            lineTo(cornerArc, strokeWidth / 2)
            quadraticBezierTo(strokeWidth / 2, strokeWidth / 2, strokeWidth / 2, cornerArc)
            lineTo(strokeWidth / 2, cornerArm)
        }
        drawPath(pathTL, bracketColor, style = strokeStyle)

        // 2. Top-Right Viewfinder Bracket
        val pathTR = Path().apply {
            moveTo(w - cornerArm, strokeWidth / 2)
            lineTo(w - cornerArc, strokeWidth / 2)
            quadraticBezierTo(w - strokeWidth / 2, strokeWidth / 2, w - strokeWidth / 2, cornerArc)
            lineTo(w - strokeWidth / 2, cornerArm)
        }
        drawPath(pathTR, bracketColor, style = strokeStyle)

        // 3. Bottom-Right Viewfinder Bracket
        val pathBR = Path().apply {
            moveTo(w - strokeWidth / 2, h - cornerArm)
            lineTo(w - strokeWidth / 2, h - cornerArc)
            quadraticBezierTo(w - strokeWidth / 2, h - strokeWidth / 2, w - cornerArc, h - strokeWidth / 2)
            lineTo(w - cornerArm, h - strokeWidth / 2)
        }
        drawPath(pathBR, bracketColor, style = strokeStyle)

        // 4. Bottom-Left Viewfinder Bracket
        val pathBL = Path().apply {
            moveTo(strokeWidth / 2, h - cornerArm)
            lineTo(strokeWidth / 2, h - cornerArc)
            quadraticBezierTo(strokeWidth / 2, h - strokeWidth / 2, cornerArc, h - strokeWidth / 2)
            lineTo(cornerArm, h - strokeWidth / 2)
        }
        drawPath(pathBL, bracketColor, style = strokeStyle)

        // 5. Center Camera Lens Ring (Electric Indigo)
        val center = Offset(w / 2, h / 2)
        val lensRadius = w * 0.25f
        drawCircle(
            color = lensColor,
            radius = lensRadius,
            center = center,
            style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
        )

        // 6. Center Task Checkmark (Emerald Mint)
        val checkPath = Path().apply {
            moveTo(w * 0.43f, h * 0.50f)
            lineTo(w * 0.49f, h * 0.57f)
            lineTo(w * 0.61f, h * 0.43f)
        }
        drawPath(
            checkPath,
            color = checkColor,
            style = Stroke(
                width = strokeWidth * 1.1f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
