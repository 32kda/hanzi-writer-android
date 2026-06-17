package com.hanziwriter.app.domain.model.geometry

object SvgPathParser {

    fun parse(d: String): List<PathSegment> {
        val segments = mutableListOf<PathSegment>()
        val tokens = tokenize(d)
        if (tokens.isEmpty()) return segments

        var i = 0
        var currentCommand = 'M'
        var relative = false
        var currentX = 0.0
        var currentY = 0.0
        var lastControlX = 0.0
        var lastControlY = 0.0
        var lastSmooth = false

        fun nextNumber(): Double {
            val token = tokens[i++]
            return token.toDouble()
        }

        fun hasNumbers(): Boolean = i < tokens.size

        fun isCommand(t: String): Boolean = t[0] in "MmZzLlHhVvCcSsQqTtAa"

        while (i < tokens.size) {
            val token = tokens[i]
            if (isCommand(token)) {
                currentCommand = token[0]
                relative = currentCommand.isLowerCase()
                i++
                when (currentCommand.uppercaseChar()) {
                    'Z' -> {
                        segments.add(PathSegment.ClosePath)
                        currentCommand = 'M'
                        lastSmooth = false
                    }
                }
                lastSmooth = when (currentCommand.uppercaseChar()) {
                    'C', 'S', 'Q', 'T' -> false
                    else -> lastSmooth
                }
            }

            when (currentCommand.uppercaseChar()) {
                'M' -> {
                    if (!hasNumbers()) break
                    val x = nextNumber()
                    val y = nextNumber()
                    val mx = if (relative) currentX + x else x
                    val my = if (relative) currentY + y else y
                    segments.add(PathSegment.MoveTo(mx, my, relative))
                    currentX = mx; currentY = my
                    currentCommand = if (relative) 'l' else 'L'
                    lastSmooth = false
                }
                'L' -> {
                    if (!hasNumbers()) break
                    val x = nextNumber()
                    val y = nextNumber()
                    val lx = if (relative) currentX + x else x
                    val ly = if (relative) currentY + y else y
                    segments.add(PathSegment.LineTo(lx, ly, relative))
                    currentX = lx; currentY = ly
                    lastSmooth = false
                }
                'H' -> {
                    if (!hasNumbers()) break
                    val x = nextNumber()
                    val hx = if (relative) currentX + x else x
                    segments.add(PathSegment.HorizontalLineTo(hx, relative))
                    currentX = hx
                    lastSmooth = false
                }
                'V' -> {
                    if (!hasNumbers()) break
                    val y = nextNumber()
                    val vy = if (relative) currentY + y else y
                    segments.add(PathSegment.VerticalLineTo(vy, relative))
                    currentY = vy
                    lastSmooth = false
                }
                'C' -> {
                    var argsRemaining = 3
                    while (hasNumbers() && argsRemaining > 0) {
                        if (tokens[i][0] in "CcSsQqTtMmZzLlHhVvAa") break
                        val x1 = nextNumber(); val y1 = nextNumber()
                        val x2 = nextNumber(); val y2 = nextNumber()
                        val x = nextNumber(); val y = nextNumber()
                        val cx1 = if (relative) currentX + x1 else x1
                        val cy1 = if (relative) currentY + y1 else y1
                        val cx2 = if (relative) currentX + x2 else x2
                        val cy2 = if (relative) currentY + y2 else y2
                        val cx = if (relative) currentX + x else x
                        val cy = if (relative) currentY + y else y
                        segments.add(PathSegment.CubicCurve(cx1, cy1, cx2, cy2, cx, cy, relative))
                        lastControlX = cx2; lastControlY = cy2
                        currentX = cx; currentY = cy
                        argsRemaining--
                    }
                    lastSmooth = true
                }
                'S' -> {
                    var argsRemaining = 2
                    while (hasNumbers() && argsRemaining > 0) {
                        if (tokens[i][0] in "CcSsQqTtMmZzLlHhVvAa") break
                        val x2 = nextNumber(); val y2 = nextNumber()
                        val x = nextNumber(); val y = nextNumber()
                        val reflectX = currentX + (currentX - lastControlX)
                        val reflectY = currentY + (currentY - lastControlY)
                        val cx2 = if (relative) currentX + x2 else x2
                        val cy2 = if (relative) currentY + y2 else y2
                        val cx = if (relative) currentX + x else x
                        val cy = if (relative) currentY + y else y
                        segments.add(PathSegment.SmoothCubicCurve(cx2, cy2, cx, cy, relative))
                        lastControlX = cx2; lastControlY = cy2
                        currentX = cx; currentY = cy
                        argsRemaining--
                    }
                    lastSmooth = true
                }
                'Q' -> {
                    var argsRemaining = 2
                    while (hasNumbers() && argsRemaining > 0) {
                        if (tokens[i][0] in "CcSsQqTtMmZzLlHhVvAa") break
                        val x1 = nextNumber(); val y1 = nextNumber()
                        val x = nextNumber(); val y = nextNumber()
                        val qx1 = if (relative) currentX + x1 else x1
                        val qy1 = if (relative) currentY + y1 else y1
                        val qx = if (relative) currentX + x else x
                        val qy = if (relative) currentY + y else y
                        segments.add(PathSegment.QuadraticCurve(qx1, qy1, qx, qy, relative))
                        lastControlX = qx1; lastControlY = qy1
                        currentX = qx; currentY = qy
                        argsRemaining--
                    }
                    lastSmooth = true
                }
                'T' -> {
                    while (hasNumbers()) {
                        if (tokens[i][0] in "CcSsQqTtMmZzLlHhVvAa") break
                        val x = nextNumber(); val y = nextNumber()
                        val reflectX = currentX + (currentX - lastControlX)
                        val reflectY = currentY + (currentY - lastControlY)
                        val tx = if (relative) currentX + x else x
                        val ty = if (relative) currentY + y else y
                        segments.add(PathSegment.SmoothQuadraticCurve(tx, ty, relative))
                        lastControlX = reflectX; lastControlY = reflectY
                        currentX = tx; currentY = ty
                    }
                    lastSmooth = true
                }
                'A' -> {
                    while (hasNumbers()) {
                        if (tokens[i][0] in "CcSsQqTtMmZzLlHhVvAa") break
                        val rx = nextNumber(); val ry = nextNumber()
                        val rot = nextNumber()
                        val laf = nextNumber().toInt() != 0
                        val sf = nextNumber().toInt() != 0
                        val x = nextNumber(); val y = nextNumber()
                        val ax = if (relative) currentX + x else x
                        val ay = if (relative) currentY + y else y
                        segments.add(PathSegment.Arc(rx, ry, rot, laf, sf, ax, ay, relative))
                        currentX = ax; currentY = ay
                    }
                    lastSmooth = false
                }
                else -> break
            }
        }
        return segments
    }

    private fun tokenize(d: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inNumber = false
        var i = 0
        while (i < d.length) {
            val c = d[i]
            when {
                c in "MmZzLlHhVvCcSsQqTtAa" -> {
                    if (current.isNotEmpty()) { tokens.add(current.toString()); current.clear() }
                    tokens.add(c.toString()); inNumber = false
                }
                c in "-" || c == '.' || c.isDigit() -> {
                    if (c == '-' && current.isNotEmpty() && current.last() != 'e' && current.last() != 'E') {
                        tokens.add(current.toString()); current.clear()
                    }
                    current.append(c); inNumber = true
                }
                c == 'e' || c == 'E' -> {
                    current.append(c); inNumber = true
                }
                c.isWhitespace() || c == ',' -> {
                    if (current.isNotEmpty()) { tokens.add(current.toString()); current.clear() }
                    inNumber = false
                }
                else -> { current.append(c); inNumber = true }
            }
            i++
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }
}
