package com.hanziwriter.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun StrokeNumberBadge(
    number: Int,
    anchorX: Float,
    anchorY: Float,
    visible: Boolean,
    circleColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 300 else 200),
        label = "badgeAlpha"
    )

    val circleRadius = 14.dp
    val textSize = 14.sp

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (anchorX - circleRadius.toPx()).roundToInt(),
                    (anchorY - circleRadius.toPx()).roundToInt()
                )
            }
            .size(circleRadius * 2)
            .alpha(alpha)
    ) {
        // Draw circle + border using Canvas
        Canvas(modifier = Modifier.size(circleRadius * 2)) {
            val center = Offset(size.width / 2, size.height / 2)
            // White fill
            drawCircle(
                color = Color.White,
                radius = size.width / 2,
                center = center
            )
            // Colored border
            drawCircle(
                color = circleColor,
                radius = size.width / 2,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Number text centered in circle
        Text(
            text = number.toString(),
            color = circleColor,
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (0.5).dp, y = (0.5).dp)
        )
    }
}
