package com.hanziwriter.app.util

import com.hanziwriter.app.domain.model.geometry.PathSegment
import com.hanziwriter.app.domain.model.geometry.SvgPathParser

fun svgToAndroidPath(svgData: String): android.graphics.Path {
    val path = android.graphics.Path()
    val segments = SvgPathParser.parse(svgData)
    var cx = 0f; var cy = 0f
    var startX = 0f; var startY = 0f
    var first = true

    fun applyRel(rel: Boolean, v: Double, cur: Float) = if (rel) cur + v.toFloat() else v.toFloat()

    for (seg in segments) {
        when (seg) {
            is PathSegment.MoveTo -> {
                val x = applyRel(seg.relative, seg.x, cx)
                val y = applyRel(seg.relative, seg.y, cy)
                if (first) { path.moveTo(x, y); first = false }
                else path.moveTo(x, y)
                startX = x; startY = y; cx = x; cy = y
            }
            is PathSegment.LineTo -> {
                val x = applyRel(seg.relative, seg.x, cx)
                val y = applyRel(seg.relative, seg.y, cy)
                path.lineTo(x, y); cx = x; cy = y
            }
            is PathSegment.HorizontalLineTo -> {
                val x = applyRel(seg.relative, seg.x, cx)
                path.lineTo(x, cy); cx = x
            }
            is PathSegment.VerticalLineTo -> {
                val y = applyRel(seg.relative, seg.y, cy)
                path.lineTo(cx, y); cy = y
            }
            is PathSegment.ClosePath -> {
                path.close(); cx = startX; cy = startY
            }
            is PathSegment.CubicCurve -> {
                val x1 = applyRel(seg.relative, seg.x1, cx); val y1 = applyRel(seg.relative, seg.y1, cy)
                val x2 = applyRel(seg.relative, seg.x2, cx); val y2 = applyRel(seg.relative, seg.y2, cy)
                val x = applyRel(seg.relative, seg.x, cx); val y = applyRel(seg.relative, seg.y, cy)
                path.cubicTo(x1, y1, x2, y2, x, y); cx = x; cy = y
            }
            is PathSegment.SmoothCubicCurve -> {
                val x2 = applyRel(seg.relative, seg.x2, cx); val y2 = applyRel(seg.relative, seg.y2, cy)
                val x = applyRel(seg.relative, seg.x, cx); val y = applyRel(seg.relative, seg.y, cy)
                path.cubicTo(cx, cy, x2, y2, x, y); cx = x; cy = y
            }
            is PathSegment.QuadraticCurve -> {
                val x1 = applyRel(seg.relative, seg.x1, cx); val y1 = applyRel(seg.relative, seg.y1, cy)
                val x = applyRel(seg.relative, seg.x, cx); val y = applyRel(seg.relative, seg.y, cy)
                path.quadTo(x1, y1, x, y); cx = x; cy = y
            }
            is PathSegment.SmoothQuadraticCurve -> {
                val x = applyRel(seg.relative, seg.x, cx); val y = applyRel(seg.relative, seg.y, cy)
                path.quadTo(cx, cy, x, y); cx = x; cy = y
            }
            is PathSegment.Arc -> {
                val x = applyRel(seg.relative, seg.x, cx); val y = applyRel(seg.relative, seg.y, cy)
                val rx = seg.rx.toFloat(); val ry = seg.ry.toFloat()
                val rect = android.graphics.RectF(cx - rx, cy - ry, cx + rx, cy + ry)
                val startAngle = 0f; val sweepAngle = if (seg.sweepFlag) 180f else -180f
                path.arcTo(rect, startAngle, sweepAngle, true)
                cx = x; cy = y
            }
        }
    }
    return path
}
