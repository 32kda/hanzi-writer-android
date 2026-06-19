package com.hanziwriter.app.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.repository.CharacterRepository
import com.hanziwriter.app.data.repository.ProgressRepository
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
    val isComplete: Boolean = false,
    // Track progress through multiple characters
    val currentCharacterIndex: Int = 0,
    val totalCharacterCount: Int = 0
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val progressRepository: ProgressRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _state = MutableStateFlow(LearnUiState())
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    private var pendingUnicodes: List<Int> = emptyList()
    private var currentUnicode: Int = 0

    /**
     * Start learning with a list of character Unicode code points.
     * Characters are learned one at a time; when one is completed,
     * the ViewModel automatically loads the next in the list.
     * When all are done, isComplete becomes true.
     */
    fun startLearn(unicodes: List<Int>) {
        pendingUnicodes = unicodes
        _state.value = _state.value.copy(
            currentCharacterIndex = 0,
            totalCharacterCount = unicodes.size
        )
        if (unicodes.isNotEmpty()) {
            loadCharacter(unicodes[0])
        } else {
            // Empty list — nothing to learn, mark complete immediately
            _state.value = _state.value.copy(isComplete = true)
        }
    }

    /**
     * Load a single character by its Unicode code point and set up the Quiz.
     * This is called once per character in the list.
     */
    private fun loadCharacter(unicode: Int) {
        currentUnicode = unicode
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                isComplete = false,
                currentStrokeIndex = 0,
                userStrokes = emptyList(),
                currentUserPoints = emptyList()
            )
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
                        // One character finished — advance to the next
                        soundManager.playCharacterCompleteSound()
                        advanceToNextCharacter()
                    }
                ))
                _state.value = _state.value.copy(
                    character = character,
                    quiz = quiz,
                    renderState = renderState,
                    isLoading = false
                )
            } else {
                // Character not found in the repository — skip it
                advanceToNextCharacter()
            }
        }
    }

    /**
     * Move to the next character in the list, or mark the session as complete
     * if there are no more characters to learn.
     */
    private fun advanceToNextCharacter() {
        val nextIndex = _state.value.currentCharacterIndex + 1
        if (nextIndex < pendingUnicodes.size) {
            _state.value = _state.value.copy(currentCharacterIndex = nextIndex)
            loadCharacter(pendingUnicodes[nextIndex])
        } else {
            _state.value = _state.value.copy(isComplete = true)
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
        val unicode = currentUnicode
        // Save each stroke attempt to the progress database so the
        // CharacterSelector knows which characters have been practiced.
        quiz.endUserStroke { result ->
            viewModelScope.launch {
                progressRepository.saveStrokeAttempt(unicode, result.isCorrect, System.currentTimeMillis())
            }
        }
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
