package com.hanziwriter.app.domain.model.character

data class Character(
    val symbol: String,
    val strokes: List<Stroke>,
    val pinyin: String = "",
    val definition: String = ""
) {
    val strokeCount: Int get() = strokes.size
}
