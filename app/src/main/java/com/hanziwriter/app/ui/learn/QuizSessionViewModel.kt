package com.hanziwriter.app.ui.learn

import com.hanziwriter.app.data.repository.CharacterRepository
import com.hanziwriter.app.data.repository.ProgressRepository
import com.hanziwriter.app.domain.model.character.HintLevel
import com.hanziwriter.app.domain.model.quiz.CharacterRound
import com.hanziwriter.app.domain.sound.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QuizSessionViewModel @Inject constructor(
    characterRepository: CharacterRepository,
    progressRepository: ProgressRepository,
    soundManager: SoundManager
) : BaseSessionViewModel(characterRepository, progressRepository, soundManager) {

    override fun buildSessionPlan(unicodes: List<Int>): List<CharacterRound> {
        return unicodes.map { CharacterRound(it, HintLevel.NONE) }
    }
}
