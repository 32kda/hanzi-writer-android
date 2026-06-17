package com.hanziwriter.app.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.repository.CharacterRepository
import com.hanziwriter.app.domain.model.quiz.Quiz
import com.hanziwriter.app.domain.model.state.RenderState
import com.hanziwriter.app.domain.sound.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearnUiState(
    val character: com.hanziwriter.app.domain.model.character.Character? = null,
    val quiz: Quiz? = null,
    val renderState: RenderState? = null,
    val isLoading: Boolean = true,
    val showNumbers: Boolean = true,
    val currentStrokeIndex: Int = 0,
    val userStrokes: List<com.hanziwriter.app.ui.components.DrawableUserStroke> = emptyList(),
    val currentUserPoints: List<androidx.compose.ui.geometry.Offset> = emptyList(),
    val isComplete: Boolean = false
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _state = MutableStateFlow(LearnUiState())
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    private var currentUnicode: Int = 0

    fun startLearn(unicode: Int) {
        currentUnicode = unicode
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val charEntity = characterRepository.getCharacterByUnicode(unicode)
            val strokeEntities = characterRepository.getStrokeData(unicode)
            if (charEntity != null) {
                val character = characterRepository.buildDomainCharacter(charEntity, strokeEntities)
                val renderState = RenderState(character)
                val quiz = Quiz(character, renderState)
                quiz.start(Quiz.QuizOptions(
                    acceptBackwardsStrokes = true,
                    onCorrectStroke = { result ->
                        _state.value = _state.value.copy(
                            currentStrokeIndex = result.strokeNum + 1
                        )
                    },
                    onMistake = {
                        soundManager.playMistakeSound()
                        soundManager.vibrate()
                    },
                    onComplete = {
                        soundManager.playCharacterCompleteSound()
                        _state.value = _state.value.copy(isComplete = true)
                    }
                ))
                _state.value = _state.value.copy(
                    character = character,
                    quiz = quiz,
                    renderState = renderState,
                    isLoading = false,
                    currentStrokeIndex = 0
                )
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun onStrokeStart(offset: androidx.compose.ui.geometry.Offset) {
        val quiz = _state.value.quiz ?: return
        val point = com.hanziwriter.app.domain.model.geometry.Point(
            offset.x.toDouble(),
            offset.y.toDouble()
        )
        quiz.startUserStroke(point)
        _state.value = _state.value.copy(
            currentUserPoints = listOf(offset)
        )
    }

    fun onStrokeMove(offset: androidx.compose.ui.geometry.Offset) {
        val quiz = _state.value.quiz ?: return
        val point = com.hanziwriter.app.domain.model.geometry.Point(
            offset.x.toDouble(),
            offset.y.toDouble()
        )
        quiz.continueUserStroke(point)
        _state.value = _state.value.copy(
            currentUserPoints = _state.value.currentUserPoints + offset
        )
    }

    fun onStrokeEnd() {
        val quiz = _state.value.quiz ?: return
        quiz.endUserStroke()
        _state.value = _state.value.copy(
            currentUserPoints = emptyList()
        )
    }

    fun playLessonCompleteSound() {
        soundManager.playLessonCompleteSound()
    }

    fun setShowNumbers(show: Boolean) {
        _state.value = _state.value.copy(showNumbers = show)
    }
}
