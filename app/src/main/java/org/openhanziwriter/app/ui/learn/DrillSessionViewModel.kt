package org.openhanziwriter.app.ui.learn

import org.openhanziwriter.app.data.local.AppPreferences
import org.openhanziwriter.app.data.repository.CharacterRepository
import org.openhanziwriter.app.data.repository.CharacterSetRepository
import org.openhanziwriter.app.data.repository.ProgressRepository
import org.openhanziwriter.app.domain.model.character.HintLevel
import org.openhanziwriter.app.domain.model.quiz.CharacterRound
import org.openhanziwriter.app.domain.sound.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DrillSessionViewModel @Inject constructor(
    characterRepository: CharacterRepository,
    progressRepository: ProgressRepository,
    soundManager: SoundManager,
    appPreferences: AppPreferences,
    characterSetRepository: CharacterSetRepository
) : BaseSessionViewModel(characterRepository, progressRepository, soundManager, appPreferences, characterSetRepository) {

    override val sessionType: String = "drill"

    override fun buildSessionPlan(unicodes: List<Int>): List<CharacterRound> {
        return unicodes.flatMap { u ->
            listOf(
                CharacterRound(u, HintLevel.GRAYED),
                CharacterRound(u, HintLevel.NONE),
            )
        }
    }
}
