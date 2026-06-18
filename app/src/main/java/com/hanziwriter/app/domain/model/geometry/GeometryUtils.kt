package com.hanziwriter.app.domain.model.geometry

import kotlin.math.*

object GeometryUtils {

    fun distance(a: Point, b: Point): Double = a.distanceTo(b)

    fun length(points: List<Point>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            total += points[i].distanceTo(points[i - 1])
        }
        return total
    }

    fun getEdgeVectors(points: List<Point>): List<Point> {
        if (points.size < 2) return emptyList()
        val vectors = mutableListOf<Point>()
        for (i in 1 until points.size) {
            vectors.add(points[i].subtract(points[i - 1]))
        }
        return vectors
    }

    fun cosineSimilarity(a: Point, b: Point): Double {
        val dot = a.x * b.x + a.y * b.y
        val magA = a.magnitude()
        val magB = b.magnitude()
        if (magA == 0.0 || magB == 0.0) return 0.0
        return dot / (magA * magB)
    }

    fun stripDuplicates(points: List<Point>): List<Point> {
        if (points.isEmpty()) return points
        val result = mutableListOf(points[0])
        for (i in 1 until points.size) {
            if (points[i] != result.last()) {
                result.add(points[i])
            }
        }
        return result
    }

    fun normalizeCurve(points: List<Point>): List<Point> {
        if (points.isEmpty()) return points
        val cx = points.map { it.x }.average()
        val cy = points.map { it.y }.average()
        val translated = points.map { it.subtract(Point(cx, cy)) }
        val maxDist = translated.maxOfOrNull { it.magnitude() } ?: 1.0
        return if (maxDist == 0.0) translated else translated.map { it.divide(maxDist) }
    }

    fun frechetDist(p: List<Point>, q: List<Point>): Double {
        val m = p.size
        val n = q.size
        if (m == 0 || n == 0) return Double.MAX_VALUE
        val ca = Array(m) { DoubleArray(n) { -1.0 } }
        fun c(i: Int, j: Int): Double {
            if (ca[i][j] > -1) return ca[i][j]
            val d = p[i].distanceTo(q[j])
            ca[i][j] = when {
                i == 0 && j == 0 -> d
                i > 0 && j == 0 -> maxOf(c(i - 1, 0), d)
                i == 0 && j > 0 -> maxOf(c(0, j - 1), d)
                else -> maxOf(minOf(c(i - 1, j), c(i - 1, j - 1), c(i, j - 1)), d)
            }
            return ca[i][j]
        }
        return c(m - 1, n - 1)
    }

    fun resampleCurve(points: List<Point>, numSamples: Int): List<Point> {
        if (points.size < 2 || numSamples < 2) return points
        val totalLength = length(points)
        if (totalLength == 0.0) return points
        val result = mutableListOf(points[0])
        var accumulated = 0.0
        val segmentLength = totalLength / (numSamples - 1)
        for (i in 1 until points.size) {
            val segLen = points[i].distanceTo(points[i - 1])
            if (segLen == 0.0) continue
            while (accumulated + segLen >= segmentLength * (result.size)) {
                val t = (segmentLength * (result.size) - accumulated) / segLen
                val px = points[i - 1].x + (points[i].x - points[i - 1].x) * t
                val py = points[i - 1].y + (points[i].y - points[i - 1].y) * t
                result.add(Point(px, py))
                if (result.size >= numSamples) return result
            }
            accumulated += segLen
        }
        if (result.size < numSamples) result.add(points.last())
        return result
    }

    fun shapeFit(
        userPoints: List<Point>,
        strokePoints: List<Point>,
        leniency: Double = 1.0
    ): Boolean {
        val maxPoints = maxOf(userPoints.size, strokePoints.size).coerceAtLeast(16)
        val resampledUser = resampleCurve(userPoints, maxPoints)
        val resampledStroke = resampleCurve(strokePoints, maxPoints)
        val normUser = normalizeCurve(resampledUser)
        val normStroke = normalizeCurve(resampledStroke)
        val frechet = frechetDist(normUser, normStroke)
        val threshold = 0.4 * leniency / 0.75
        return frechet <= threshold
    }

    fun rotate(points: List<Point>, angleRad: Double): List<Point> {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return points.map { p ->
            Point(p.x * cosA - p.y * sinA, p.x * sinA + p.y * cosA)
        }
    }

    fun subdivideCurve(points: List<Point>, factor: Int = 4): List<Point> {
        if (points.size < 2) return points
        val result = mutableListOf(points[0])
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            for (j in 1..factor) {
                val t = j.toDouble() / factor
                result.add(Point(
                    prev.x + (curr.x - prev.x) * t,
                    prev.y + (curr.y - prev.y) * t
                ))
            }
        }
        return result
    }

    fun outlineCurve(points: List<Point>, offset: Double): List<Point> {
        if (points.size < 2) return points
        val result = mutableListOf<Point>()
        for (i in points.indices) {
            val prev = if (i > 0) points[i - 1] else points[i]
            val next = if (i < points.size - 1) points[i + 1] else points[i]
            val dx = next.x - prev.x
            val dy = next.y - prev.y
            val len = sqrt(dx * dx + dy * dy)
            if (len > 0) {
                result.add(Point(
                    points[i].x + (-dy / len) * offset,
                    points[i].y + (dx / len) * offset
                ))
            } else {
                result.add(points[i])
            }
        }
        return result
    }

    fun filterParallelPoints(points: List<Point>, angleThresholdDeg: Double = 15.0): List<Point> {
        if (points.size < 3) return points
        val result = mutableListOf(points[0], points[1])
        val thresholdRad = angleThresholdDeg * PI / 180.0
        for (i in 2 until points.size) {
            val v1 = points[i - 1].subtract(points[i - 2])
            val v2 = points[i].subtract(points[i - 1])
            val cosAngle = cosineSimilarity(v1, v2)
            val angle = acos(cosAngle.coerceIn(-1.0, 1.0))
            if (angle > thresholdRad) {
                result.add(points[i])
            }
        }
        return result
    }

    fun extendStart(points: List<Point>, distance: Double): List<Point> {
        if (points.size < 2) return points
        val start = points[0]
        val next = points[1]
        val dx = next.x - start.x
        val dy = next.y - start.y
        val len = sqrt(dx * dx + dy * dy)
        if (len == 0.0) return points
        val ext = Point(start.x - (dx / len) * distance, start.y - (dy / len) * distance)
        return listOf(ext) + points
    }

    fun extendEnd(points: List<Point>, distance: Double): List<Point> {
        if (points.size < 2) return points
        val end = points.last()
        val prev = points[points.size - 2]
        val dx = end.x - prev.x
        val dy = end.y - prev.y
        val len = sqrt(dx * dx + dy * dy)
        if (len == 0.0) return points
        val ext = Point(end.x + (dx / len) * distance, end.y + (dy / len) * distance)
        return points + ext
    }

    fun boundingBox(points: List<Point>): Pair<Point, Point> {
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }
        return Pair(Point(minX, minY), Point(maxX, maxY))
    }
}
