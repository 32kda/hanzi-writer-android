package com.hanziwriter.app.domain.model.state

import kotlin.math.roundToInt

data class Color(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float = 1.0f
) {
    constructor(r: Int, g: Int, b: Int, a: Float = 1.0f) : this(r / 255f, g / 255f, b / 255f, a)

    fun toArgb(): Int {
        val a = (alpha * 255).roundToInt().coerceIn(0, 255)
        val r = (red * 255).roundToInt().coerceIn(0, 255)
        val g = (green * 255).roundToInt().coerceIn(0, 255)
        val b = (blue * 255).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    companion object {
        fun parse(hex: String): Color {
            val h = hex.removePrefix("#")
            return when (h.length) {
                6 -> Color(
                    h.substring(0, 2).toInt(16) / 255f,
                    h.substring(2, 4).toInt(16) / 255f,
                    h.substring(4, 6).toInt(16) / 255f
                )
                8 -> Color(
                    h.substring(2, 4).toInt(16) / 255f,
                    h.substring(4, 6).toInt(16) / 255f,
                    h.substring(6, 8).toInt(16) / 255f,
                    h.substring(0, 2).toInt(16) / 255f
                )
                else -> Color(0f, 0f, 0f)
            }
        }
    }
}
