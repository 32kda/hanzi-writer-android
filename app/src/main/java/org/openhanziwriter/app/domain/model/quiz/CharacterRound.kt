package org.openhanziwriter.app.domain.model.quiz

import org.openhanziwriter.app.domain.model.character.HintLevel

data class CharacterRound(
    val unicode: Int,
    val hintLevel: HintLevel,
    val pinyin: String = "",
    val definition: String = ""
)
