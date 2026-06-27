package com.hanziwriter.app.domain.model.geometry

import java.nio.ByteBuffer
import java.nio.ByteOrder

object BinaryPathParser {

    private const val CMD_MOVE_TO: Byte = 0
    private const val CMD_LINE_TO: Byte = 1
    private const val CMD_QUAD_TO: Byte = 2
    private const val CMD_CUBIC_TO: Byte = 3
    private const val CMD_CLOSE: Byte = 4

    fun parse(data: ByteArray): List<PathSegment> {
        val segments = mutableListOf<PathSegment>()
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        while (buf.hasRemaining()) {
            val cmd = buf.get()
            when (cmd) {
                CMD_MOVE_TO -> {
                    val x = buf.getShort().toDouble()
                    val y = buf.getShort().toDouble()
                    segments.add(PathSegment.MoveTo(x, y))
                }
                CMD_LINE_TO -> {
                    val x = buf.getShort().toDouble()
                    val y = buf.getShort().toDouble()
                    segments.add(PathSegment.LineTo(x, y))
                }
                CMD_QUAD_TO -> {
                    val x1 = buf.getShort().toDouble()
                    val y1 = buf.getShort().toDouble()
                    val x = buf.getShort().toDouble()
                    val y = buf.getShort().toDouble()
                    segments.add(PathSegment.QuadraticCurve(x1, y1, x, y))
                }
                CMD_CUBIC_TO -> {
                    val x1 = buf.getShort().toDouble()
                    val y1 = buf.getShort().toDouble()
                    val x2 = buf.getShort().toDouble()
                    val y2 = buf.getShort().toDouble()
                    val x = buf.getShort().toDouble()
                    val y = buf.getShort().toDouble()
                    segments.add(PathSegment.CubicCurve(x1, y1, x2, y2, x, y))
                }
                CMD_CLOSE -> {
                    segments.add(PathSegment.ClosePath)
                }
            }
        }
        return segments
    }

    fun parseMedians(data: ByteArray): List<Point> {
        val points = mutableListOf<Point>()
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        while (buf.remaining() >= 4) {
            val x = buf.getShort().toDouble()
            val y = buf.getShort().toDouble()
            points.add(Point(x, y))
        }
        return points
    }
}
