package com.hanziwriter.app.data.local

data class CharacterSetEntry(
    val character: String,
    val pinyin: String,
    val translation: String,
    val unicode: Int = character.firstOrNull()?.code ?: 0
)

data class CharacterSetInfo(
    val dirName: String,
    val displayName: String,
    val description: String
)
