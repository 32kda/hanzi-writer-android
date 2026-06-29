package com.hanziwriter.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.hanziwriter.app.domain.model.geometry.Point

object StrokeBadge {

    fun computeDirection(points: List<Point>): Offset? {
        if (points.size < 2) return null
        val sample = points.take(4)
        var sumDx = 0.0
        var sumDy = 0.0
        for (j in 1 until sample.size) {
            sumDx += sample[j].x - sample[j - 1].x
            sumDy += sample[j].y - sample[j - 1].y
        }
        val len = kotlin.math.sqrt(sumDx * sumDx + sumDy * sumDy)
        if (len < 0.001) return null
        return Offset((sumDx / len).toFloat(), (-sumDy / len).toFloat())
    }

    fun DrawScope.draw(
        number: Int,
        centerX: Float,
        centerY: Float,
        color: Color,
        radius: Float = 28f,
        direction: Offset? = null
    ) {
        if (direction != null) {
            val arrowColor = Color(0xFF2196F3)
            val arrowLength = 24f
            val gap = 6f
            val headLength = 16f
            val headWidth = 16f

            val tailX = centerX + direction.x * (radius + gap)
            val tailY = centerY + direction.y * (radius + gap)
            val tipX = centerX + direction.x * (radius + gap + arrowLength)
            val tipY = centerY + direction.y * (radius + gap + arrowLength)

            drawLine(
                color = arrowColor,
                start = Offset(tailX, tailY),
                end = Offset(tipX, tipY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            val perpX = -direction.y
            val perpY = direction.x
            drawLine(
                color = arrowColor,
                start = Offset(tipX, tipY),
                end = Offset(
                    tipX - direction.x * headLength + perpX * headWidth / 2,
                    tipY - direction.y * headLength + perpY * headWidth / 2
                ),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = arrowColor,
                start = Offset(tipX, tipY),
                end = Offset(
                    tipX - direction.x * headLength - perpX * headWidth / 2,
                    tipY - direction.y * headLength - perpY * headWidth / 2
                ),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            color = Color.White,
            radius = radius,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 4f)
        )
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            number.toString(),
            centerX,
            centerY + paint.textSize / 3f,
            paint
        )
    }
}
