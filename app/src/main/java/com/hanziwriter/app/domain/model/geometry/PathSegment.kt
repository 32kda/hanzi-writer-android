package com.hanziwriter.app.domain.model.geometry

sealed interface PathSegment {
    data class MoveTo(val x: Double, val y: Double, val relative: Boolean = false) : PathSegment
    data class LineTo(val x: Double, val y: Double, val relative: Boolean = false) : PathSegment
    data class HorizontalLineTo(val x: Double, val relative: Boolean = false) : PathSegment
    data class VerticalLineTo(val y: Double, val relative: Boolean = false) : PathSegment
    data class CubicCurve(
        val x1: Double, val y1: Double,
        val x2: Double, val y2: Double,
        val x: Double, val y: Double,
        val relative: Boolean = false
    ) : PathSegment
    data class SmoothCubicCurve(
        val x2: Double, val y2: Double,
        val x: Double, val y: Double,
        val relative: Boolean = false
    ) : PathSegment
    data class QuadraticCurve(
        val x1: Double, val y1: Double,
        val x: Double, val y: Double,
        val relative: Boolean = false
    ) : PathSegment
    data class SmoothQuadraticCurve(
        val x: Double, val y: Double,
        val relative: Boolean = false
    ) : PathSegment
    data class Arc(
        val rx: Double, val ry: Double,
        val xAxisRotation: Double,
        val largeArcFlag: Boolean,
        val sweepFlag: Boolean,
        val x: Double, val y: Double,
        val relative: Boolean = false
    ) : PathSegment
    data object ClosePath : PathSegment
}
