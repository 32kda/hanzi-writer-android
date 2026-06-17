package com.hanziwriter.app.domain.model.geometry

import kotlin.math.roundToLong
import kotlin.math.sqrt

data class Point(val x: Double, val y: Double) {

    fun subtract(other: Point): Point = Point(x - other.x, y - other.y)

    fun magnitude(): Double = sqrt(x * x + y * y)

    fun multiply(scalar: Double): Point = Point(x * scalar, y * scalar)

    fun divide(scalar: Double): Point =
        if (scalar == 0.0) Point(0.0, 0.0) else Point(x / scalar, y / scalar)

    fun add(other: Point): Point = Point(x + other.x, y + other.y)

    fun distanceTo(other: Point): Double = subtract(other).magnitude()

    fun copy(): Point = Point(x, y)

    fun round(precision: Int): Point {
        val multiplier = precision * 10.0
        return Point(
            (multiplier * x).roundToLong() / multiplier,
            (multiplier * y).roundToLong() / multiplier
        )
    }

    companion object {
        val ZERO = Point(0.0, 0.0)
    }
}
