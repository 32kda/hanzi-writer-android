package com.hanziwriter.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hanziwriter.app.domain.model.character.Character

import com.hanziwriter.app.domain.model.geometry.PathSegment
import com.hanziwriter.app.domain.model.geometry.Point
import com.hanziwriter.app.util.segmentsToAndroidPath

data class CanvasViewport(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

data class DrawableStroke(
    val segments: List<PathSegment>,
    val medianPoints: List<Point>,
    val color: Color,
    val opacity: Float,
    val drawPortion: Float,
    val strokeNum: Int
)

data class DrawableUserStroke(
    val points: List<Offset>,
    val color: Color
)

/**
 * Y-axis anchor for coordinate space conversion.
 *
 * The stroke dataset uses a Y range of roughly [-124, 900] with (0, 900) as
 * the upper-left origin and Y *decreasing* downward.  Per the dataset SVG spec
 * the correct Y-flip transform is:
 *
 *     translate(0, -900) scale(1, -1)    =>   y_screen = 900 - y_char
 *
 * Using 1024 here (the naively assumed canvas height) would shift every stroke
 * ~124 character-space units downward on screen.
 */
private const val CHAR_Y_FLIP_ANCHOR = 900f

/**
 * Character animation stroke width
 */
private const val STROKE_WIDTH = 100f

@Composable
fun WritingCanvas(
    character: Character?,
    referenceStrokes: List<DrawableStroke>,
    userStrokes: List<DrawableUserStroke>,
    currentUserPoints: List<Offset>,
    showGrid: Boolean = true,
    showNumbers: Boolean = false,
    currentStrokeIndex: Int = -1,
    animationProgress: Float = 1f,
    onStrokeStart: ((Offset) -> Unit)? = null,
    onStrokeMove: ((Offset) -> Unit)? = null,
    onStrokeEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun screenToChar(screenPos: Offset): Offset {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return screenPos
        val vp = computeViewport(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        return Offset(
            (screenPos.x - vp.offsetX) / vp.scale,
            CHAR_Y_FLIP_ANCHOR - (screenPos.y - vp.offsetY) / vp.scale
        )
    }

    Box(modifier = modifier) {
        if (showGrid) {
            TianZiGe(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .onGloballyPositioned { coordinates ->
                    canvasSize = coordinates.size
                }
                .then(
                    if (onStrokeStart != null) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onStrokeStart(screenToChar(offset))
                                },
                                onDrag = { change, _ ->
                                    onStrokeMove?.invoke(screenToChar(change.position))
                                },
                                onDragEnd = { onStrokeEnd?.invoke() }
                            )
                        }
                    } else Modifier
                )
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val viewport = computeViewport(canvasWidth, canvasHeight)

            // Draw reference strokes
            for (stroke in referenceStrokes) {
                if (stroke.opacity <= 0f) continue
                drawSvgStroke(
                    segments = stroke.segments,
                    medianPoints = stroke.medianPoints,
                    viewport = viewport,
                    color = stroke.color.copy(alpha = stroke.opacity),
                    drawPortion = stroke.drawPortion
                )
            }

            // Draw user strokes (convert char-space to screen-space)
            for (userStroke in userStrokes) {
                drawUserPath(userStroke.points, userStroke.color, viewport)
            }

            // Draw current in-progress stroke
            if (currentUserPoints.size > 1) {
                drawUserPath(currentUserPoints, primaryColor, viewport)
            }

            // Draw stroke number badges
            if (showNumbers && character != null) {
                for (i in character.strokes.indices) {
                    val stroke = character.strokes[i]
                    val startPoint = stroke.startingPoint
                    val mapped = mapPoint(startPoint, viewport)
                    val badgeVisible = animationProgress < 1f || i >= currentStrokeIndex

                    if (badgeVisible) {
                        val direction = StrokeBadge.computeDirection(stroke.points)
                        StrokeBadge.run {
                            draw(
                                number = i + 1,
                                centerX = mapped.x,
                                centerY = mapped.y,
                                color = primaryColor,
                                direction = direction
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun computeViewport(canvasWidth: Float, canvasHeight: Float): CanvasViewport {
    val charSize = 1024f
    val scale = minOf(canvasWidth, canvasHeight) / charSize
    val offsetX = (canvasWidth - charSize * scale) / 2f
    val offsetY = (canvasHeight - charSize * scale) / 2f
    return CanvasViewport(scale, offsetX, offsetY)
}

private fun mapPoint(point: Point, vp: CanvasViewport): Offset {
    return Offset(
        x = point.x.toFloat() * vp.scale + vp.offsetX,
        y = (CHAR_Y_FLIP_ANCHOR - point.y.toFloat()) * vp.scale + vp.offsetY
    )
}

private fun DrawScope.drawSvgStroke(
    segments: List<PathSegment>,
    medianPoints: List<Point>,
    viewport: CanvasViewport,
    color: Color,
    drawPortion: Float = 1f
) {
    val path = android.graphics.Path()
    val scale = viewport.scale
    val ox = viewport.offsetX
    val oy = viewport.offsetY

    try {
        val nativePath = segmentsToAndroidPath(segments)
        path.set(nativePath)
        // SVG data uses Y-up (Cartesian), flip to screen Y-down
        val matrix = android.graphics.Matrix()
        matrix.postScale(scale, -scale)
        matrix.postTranslate(ox, oy + CHAR_Y_FLIP_ANCHOR * scale)
        path.transform(matrix)

        val fillPaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        val strokePaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            strokeWidth = 3f * scale
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        val canvas = drawContext.canvas.nativeCanvas

        val isPartial = drawPortion > 0f && drawPortion < 1f
        if (drawPortion <= 0f) return

        if (isPartial && medianPoints.size >= 2) {
            drawAnimatedStroke(path, medianPoints, fillPaint, strokePaint, drawPortion, scale, ox, oy)
        } else if (!isPartial) {
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
    } catch (_: Exception) {
        // Fallback: skip if path parsing fails
    }
}

private fun DrawScope.drawAnimatedStroke(
    outlinePath: android.graphics.Path,
    medianPoints: List<Point>,
    fillPaint: android.graphics.Paint,
    strokePaint: android.graphics.Paint,
    drawPortion: Float,
    scale: Float,
    ox: Float,
    oy: Float
) {
    val canvas = drawContext.canvas.nativeCanvas

    canvas.drawPath(outlinePath, strokePaint)

    val medPath = android.graphics.Path()
    val f = medianPoints[0]
    medPath.moveTo(
        f.x.toFloat() * scale + ox,
        (CHAR_Y_FLIP_ANCHOR - f.y.toFloat()) * scale + oy
    )

    val segLens = DoubleArray(medianPoints.size - 1)
    var totalLength = 0.0
    for (k in 1 until medianPoints.size) {
        val dx = medianPoints[k].x - medianPoints[k - 1].x
        val dy = medianPoints[k].y - medianPoints[k - 1].y
        segLens[k - 1] = kotlin.math.sqrt(dx * dx + dy * dy)
        totalLength += segLens[k - 1]
    }

    val animLen = totalLength * drawPortion
    var acc = 0.0
    for (k in 1 until medianPoints.size) {
        if (acc + segLens[k - 1] > animLen && animLen > acc) {
            val frac = (animLen - acc) / segLens[k - 1]
            val pt = medianPoints[k]
            val px = medianPoints[k - 1].x + (pt.x - medianPoints[k - 1].x) * frac
            val py = medianPoints[k - 1].y + (pt.y - medianPoints[k - 1].y) * frac
            medPath.lineTo(
                px.toFloat() * scale + ox,
                (CHAR_Y_FLIP_ANCHOR - py.toFloat()) * scale + oy
            )
            break
        }
        val p = medianPoints[k]
        medPath.lineTo(
            p.x.toFloat() * scale + ox,
            (CHAR_Y_FLIP_ANCHOR - p.y.toFloat()) * scale + oy
        )
        acc += segLens[k - 1]
    }

    val thickStroke = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH * scale
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    val clipPath = android.graphics.Path()
    thickStroke.getFillPath(medPath, clipPath)

    val saveCount = canvas.save()
    canvas.clipPath(clipPath)
    canvas.drawPath(outlinePath, fillPaint)
    canvas.restoreToCount(saveCount)
}

private fun DrawScope.drawUserPath(points: List<Offset>, color: Color, viewport: CanvasViewport? = null) {
    if (points.size < 2) return
    val path = Path()
    val first = if (viewport != null) {
        Offset(
            points.first().x * viewport.scale + viewport.offsetX,
            (CHAR_Y_FLIP_ANCHOR - points.first().y) * viewport.scale + viewport.offsetY
        )
    } else points.first()
    path.moveTo(first.x, first.y)
    for (i in 1 until points.size) {
        val p = if (viewport != null) {
            Offset(
                points[i].x * viewport.scale + viewport.offsetX,
                (CHAR_Y_FLIP_ANCHOR - points[i].y) * viewport.scale + viewport.offsetY
            )
        } else points[i]
        path.lineTo(p.x, p.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 16f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}



