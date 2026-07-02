package com.hanziwriter.app.ui.learn

import com.hanziwriter.app.data.local.AppPreferences
import com.hanziwriter.app.data.repository.CharacterRepository
import com.hanziwriter.app.data.repository.CharacterSetRepository
import com.hanziwriter.app.data.repository.ProgressRepository
import com.hanziwriter.app.domain.model.character.HintLevel
import com.hanziwriter.app.domain.model.quiz.CharacterRound
import com.hanziwriter.app.domain.sound.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LearnSessionViewModel @Inject constructor(
    characterRepository: CharacterRepository,
    progressRepository: ProgressRepository,
    soundManager: SoundManager,
    appPreferences: AppPreferences,
    characterSetRepository: CharacterSetRepository
) : BaseSessionViewModel(characterRepository, progressRepository, soundManager, appPreferences, characterSetRepository) {

    override val sessionType: String = "learn"

    override fun buildSessionPlan(unicodes: List<Int>): List<CharacterRound> {
        return unicodes.flatMap { u ->
            listOf(
                CharacterRound(u, HintLevel.FULL),
                CharacterRound(u, HintLevel.FULL),
                CharacterRound(u, HintLevel.HALF),
                CharacterRound(u, HintLevel.HALF),
                CharacterRound(u, HintLevel.NONE),
                CharacterRound(u, HintLevel.NONE),
            )
        }
    }
}
