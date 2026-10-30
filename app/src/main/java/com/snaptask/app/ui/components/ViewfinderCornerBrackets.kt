package com.snaptask.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4 L-shaped corner brackets (Khung ngắm 4 góc) for Camera Viewfinder
 * Directly matching Page 5 of the "Ink & Flash" design proposal.
 */
@Composable
fun ViewfinderCornerBrackets(
    modifier: Modifier = Modifier.fillMaxSize(),
    color: Color = Color(0xFFF5F1E8),
    strokeWidth: Dp = 5.dp,
    length: Dp = 49.dp,
    cornerRadius: Dp = 20.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val len = length.toPx()
        val r = cornerRadius.toPx()
        val w = size.width
        val h = size.height

        // Top-Left (TL)
        val pathTL = Path().apply {
            moveTo(0f, len)
            lineTo(0f, r)
            quadraticBezierTo(0f, 0f, r, 0f)
            lineTo(len, 0f)
        }
        drawPath(pathTL, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Top-Right (TR)
        val pathTR = Path().apply {
            moveTo(w - len, 0f)
            lineTo(w - r, 0f)
            quadraticBezierTo(w, 0f, w, r)
            lineTo(w, len)
        }
        drawPath(pathTR, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Bottom-Left (BL)
        val pathBL = Path().apply {
            moveTo(0f, h - len)
            lineTo(0f, h - r)
            quadraticBezierTo(0f, h, r, h)
            lineTo(len, h)
        }
        drawPath(pathBL, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Bottom-Right (BR)
        val pathBR = Path().apply {
            moveTo(w - len, h)
            lineTo(w - r, h)
            quadraticBezierTo(w, h, w, h - r)
            lineTo(w, h - len)
        }
        drawPath(pathBR, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
