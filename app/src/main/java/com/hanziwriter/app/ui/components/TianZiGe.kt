package com.hanziwriter.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect

@Composable
fun TianZiGe(
    modifier: Modifier = Modifier,
    gridColor: Color = Color(0xFFE0E0E0),
    borderColor: Color = Color(0xFF9E9E9E)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val halfW = w / 2
        val halfH = h / 2

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        // Outer border
        drawRect(
            color = borderColor,
            topLeft = Offset.Zero,
            size = size,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Center vertical line (dashed)
        drawLine(
            color = gridColor,
            start = Offset(halfW, 0f),
            end = Offset(halfW, h),
            strokeWidth = 1f,
            pathEffect = dashEffect
        )

        // Center horizontal line (dashed)
        drawLine(
            color = gridColor,
            start = Offset(0f, halfH),
            end = Offset(w, halfH),
            strokeWidth = 1f,
            pathEffect = dashEffect
        )

        // Diagonal lines (dashed, lighter)
        drawLine(
            color = gridColor.copy(alpha = 0.5f),
            start = Offset.Zero,
            end = Offset(w, h),
            strokeWidth = 1f,
            pathEffect = dashEffect
        )
        drawLine(
            color = gridColor.copy(alpha = 0.5f),
            start = Offset(w, 0f),
            end = Offset(0f, h),
            strokeWidth = 1f,
            pathEffect = dashEffect
        )
    }
}
