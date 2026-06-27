package com.hanziwriter.app.domain.model.character

import com.hanziwriter.app.domain.model.geometry.BinaryPathParser
import com.hanziwriter.app.domain.model.geometry.GeometryUtils
import com.hanziwriter.app.domain.model.geometry.PathSampler
import com.hanziwriter.app.domain.model.geometry.PathSegment
import com.hanziwriter.app.domain.model.geometry.Point

data class Stroke(
    val path: ByteArray,
    val points: List<Point>,
    val strokeNum: Int,
    val isInRadical: Boolean = false
) {
    private var outlinePoints: List<Point>? = null
    private var parsedPath: List<PathSegment>? = null

    val startingPoint: Point get() = points.first()
    val endingPoint: Point get() = points.last()
    val length: Double get() = GeometryUtils.length(points)
    val vectors: List<Point> get() = GeometryUtils.getEdgeVectors(points)

    fun getOutlinePoints(): List<Point>? = outlinePoints

    fun getParsedPath(): List<PathSegment>? = parsedPath

    fun parsePath(segmentsPerCurve: Int = 12): Boolean {
        if (path.isEmpty()) return false
        val parsed = BinaryPathParser.parse(path)
        if (parsed.isEmpty()) return false
        val sampled = PathSampler.sample(parsed, segmentsPerCurve)
        if (sampled.size < 2) return false
        this.parsedPath = parsed
        this.outlinePoints = sampled
        return true
    }

    fun getDistance(point: Point): Double =
        points.minOf { GeometryUtils.distance(it, point) }

    fun getAverageDistance(userPoints: List<Point>): Double {
        val total = userPoints.sumOf { getDistance(it) }
        return total / userPoints.size
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Stroke) return false
        return path.contentEquals(other.path) && points == other.points && strokeNum == other.strokeNum && isInRadical == other.isInRadical
    }

    override fun hashCode(): Int {
        var result = path.contentHashCode()
        result = 31 * result + points.hashCode()
        result = 31 * result + strokeNum
        result = 31 * result + isInRadical.hashCode()
        return result
    }
}
