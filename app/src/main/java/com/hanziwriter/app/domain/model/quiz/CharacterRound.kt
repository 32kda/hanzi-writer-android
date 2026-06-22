package com.hanziwriter.app.domain.model.quiz

import com.hanziwriter.app.domain.model.character.HintLevel

data class CharacterRound(
    val unicode: Int,
    val hintLevel: HintLevel
)
