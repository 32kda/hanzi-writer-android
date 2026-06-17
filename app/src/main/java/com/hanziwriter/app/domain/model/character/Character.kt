package com.hanziwriter.app.domain.model.character

data class Character(
    val symbol: String,
    val strokes: List<Stroke>
) {
    val strokeCount: Int get() = strokes.size
}
