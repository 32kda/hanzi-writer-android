package com.hanziwriter.app.domain.model.geometry

import kotlin.math.*

object PathSampler {

    fun sample(segments: List<PathSegment>, segmentsPerCurve: Int = 12): List<Point> {
        val points = mutableListOf<Point>()
        var currentX = 0.0
        var currentY = 0.0
        var lastControlX = 0.0
        var lastControlY = 0.0

        for (seg in segments) {
            when (seg) {
                is PathSegment.MoveTo -> {
                    currentX = seg.x; currentY = seg.y
                }
                is PathSegment.LineTo -> {
                    points.add(Point(currentX, currentY))
                    points.add(Point(seg.x, seg.y))
                    currentX = seg.x; currentY = seg.y
                }
                is PathSegment.HorizontalLineTo -> {
                    points.add(Point(currentX, currentY))
                    points.add(Point(seg.x, currentY))
                    currentX = seg.x
                }
                is PathSegment.VerticalLineTo -> {
                    points.add(Point(currentX, currentY))
                    points.add(Point(currentX, seg.y))
                    currentY = seg.y
                }
                is PathSegment.CubicCurve -> {
                    val samples = sampleCubicBezier(
                        Point(currentX, currentY),
                        Point(seg.x1, seg.y1),
                        Point(seg.x2, seg.y2),
                        Point(seg.x, seg.y),
                        segmentsPerCurve
                    )
                    points.addAll(samples)
                    lastControlX = seg.x2; lastControlY = seg.y2
                    currentX = seg.x; currentY = seg.y
                }
                is PathSegment.SmoothCubicCurve -> {
                    val reflectX = currentX + (currentX - lastControlX)
                    val reflectY = currentY + (currentY - lastControlY)
                    val samples = sampleCubicBezier(
                        Point(currentX, currentY),
                        Point(reflectX, reflectY),
                        Point(seg.x2, seg.y2),
                        Point(seg.x, seg.y),
                        segmentsPerCurve
                    )
                    points.addAll(samples)
                    lastControlX = seg.x2; lastControlY = seg.y2
                    currentX = seg.x; currentY = seg.y
                }
                is PathSegment.QuadraticCurve -> {
                    val samples = sampleQuadraticBezier(
                        Point(currentX, currentY),
                        Point(seg.x1, seg.y1),
                        Point(seg.x, seg.y),
                        segmentsPerCurve
                    )
                    points.addAll(samples)
                    lastControlX = seg.x1; lastControlY = seg.y1
                    currentX = seg.x; currentY = seg.y
                }
                is PathSegment.SmoothQuadraticCurve -> {
                    val reflectX = currentX + (currentX - lastControlX)
                    val reflectY = currentY + (currentY - lastControlY)
                    val samples = sampleQuadraticBezier(
                        Point(currentX, currentY),
                        Point(reflectX, reflectY),
                        Point(seg.x, seg.y),
                        segmentsPerCurve
                    )
                    points.addAll(samples)
                    lastControlX = reflectX; lastControlY = reflectY
                    currentX = seg.x; currentY = seg.y
                }
                is PathSegment.Arc -> {
                    val samples = sampleArc(
                        Point(currentX, currentY),
                        seg.rx, seg.ry, seg.xAxisRotation,
                        seg.largeArcFlag, seg.sweepFlag,
                        Point(seg.x, seg.y),
                        segmentsPerCurve
                    )
                    points.addAll(samples)
                    currentX = seg.x; currentY = seg.y
                }
                is PathSegment.ClosePath -> { }
            }
        }
        return stripNearDuplicates(points)
    }

    fun sampleCubicBezier(p0: Point, p1: Point, p2: Point, p3: Point, n: Int): List<Point> {
        val points = mutableListOf<Point>()
        for (i in 1..n) {
            val t = i.toDouble() / n
            val mt = 1.0 - t
            val x = mt * mt * mt * p0.x + 3 * mt * mt * t * p1.x + 3 * mt * t * t * p2.x + t * t * t * p3.x
            val y = mt * mt * mt * p0.y + 3 * mt * mt * t * p1.y + 3 * mt * t * t * p2.y + t * t * t * p3.y
            points.add(Point(x, y))
        }
        return points
    }

    fun sampleQuadraticBezier(p0: Point, p1: Point, p2: Point, n: Int): List<Point> {
        val points = mutableListOf<Point>()
        for (i in 1..n) {
            val t = i.toDouble() / n
            val mt = 1.0 - t
            val x = mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x
            val y = mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y
            points.add(Point(x, y))
        }
        return points
    }

    private fun sampleArc(
        start: Point, rx: Double, ry: Double, xAxisRotation: Double,
        largeArc: Boolean, sweep: Boolean, end: Point, n: Int
    ): List<Point> {
        val points = mutableListOf<Point>()
        val dx = (start.x - end.x) / 2.0
        val dy = (start.y - end.y) / 2.0
        val cosPhi = cos(xAxisRotation * PI / 180.0)
        val sinPhi = sin(xAxisRotation * PI / 180.0)
        val x1p = cosPhi * dx + sinPhi * dy
        val y1p = -sinPhi * dx + cosPhi * dy

        var rxx = abs(rx)
        var ryy = abs(ry)
        val lambda = (x1p * x1p) / (rxx * rxx) + (y1p * y1p) / (ryy * ryy)
        if (lambda > 1) { rxx *= sqrt(lambda); ryy *= sqrt(lambda) }

        val sign = if (largeArc != sweep) 1.0 else -1.0
        val sq = ((rxx * rxx * ryy * ryy - rxx * rxx * y1p * y1p - ryy * ryy * x1p * x1p) /
                (rxx * rxx * y1p * y1p + ryy * ryy * x1p * x1p))
        val base = if (sq < 0) 0.0 else sqrt(sq)
        var cxp = sign * base * rxx * y1p / ryy
        var cyp = sign * -base * ryy * x1p / rxx

        val cx = cosPhi * cxp - sinPhi * cyp + (start.x + end.x) / 2.0
        val cy = sinPhi * cxp + cosPhi * cyp + (start.y + end.y) / 2.0

        fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
            val dot = ux * vx + uy * vy
            val cross = ux * vy - uy * vx
            return atan2(cross, dot)
        }

        val startAngle = angle(1.0, 0.0, (x1p - cxp) / rxx, (y1p - cyp) / ryy)
        var endAngle = angle(
            (-x1p - cxp) / rxx, (-y1p - cyp) / ryy,
            (x1p - cxp) / rxx, (y1p - cyp) / ryy
        )
        while (sweep && endAngle < startAngle) endAngle += 2 * PI
        while (!sweep && endAngle > startAngle) endAngle -= 2 * PI

        val delta = endAngle - startAngle
        val segs = maxOf(1.0, (abs(delta) / (PI / 4))).toInt().coerceAtLeast(n / 4)

        for (i in 1..segs) {
            val t = i.toDouble() / segs
            val theta = startAngle + delta * t
            val px = cx + rxx * cosPhi * cos(theta) - ryy * sinPhi * sin(theta)
            val py = cy + rxx * sinPhi * cos(theta) + ryy * cosPhi * sin(theta)
            points.add(Point(px, py))
        }
        return points
    }

    private fun stripNearDuplicates(points: List<Point>, minDist: Double = 0.5): List<Point> {
        if (points.isEmpty()) return points
        val result = mutableListOf(points[0])
        for (i in 1 until points.size) {
            if (points[i].distanceTo(result.last()) >= minDist) {
                result.add(points[i])
            }
        }
        return result
    }
}
