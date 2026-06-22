package com.hanziwriter.app.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.repository.CharacterRepository
import com.hanziwriter.app.data.repository.ProgressRepository
import com.hanziwriter.app.domain.model.character.Character
import com.hanziwriter.app.domain.model.character.HintLevel
import com.hanziwriter.app.domain.model.quiz.CharacterRound
import com.hanziwriter.app.domain.model.quiz.Quiz
import com.hanziwriter.app.domain.model.state.RenderState
import com.hanziwriter.app.domain.sound.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val currentRoundIndex: Int = 0,
    val totalRounds: Int = 0
)

abstract class BaseSessionViewModel(
    protected val characterRepository: CharacterRepository,
    protected val progressRepository: ProgressRepository,
    protected val soundManager: SoundManager
) : ViewModel() {

    protected val _state = MutableStateFlow(LearnUiState())
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    protected var sessionPlan: List<CharacterRound> = emptyList()
    private var currentUnicode: Int = 0

    abstract fun buildSessionPlan(unicodes: List<Int>): List<CharacterRound>

    fun startSession(unicodes: List<Int>) {
        sessionPlan = buildSessionPlan(unicodes)
        _state.value = _state.value.copy(
            currentRoundIndex = 0,
            totalRounds = sessionPlan.size
        )
        if (sessionPlan.isNotEmpty()) {
            loadCharacterRound(0)
        } else {
            _state.value = _state.value.copy(isComplete = true)
        }
    }

    private fun loadCharacterRound(roundIndex: Int) {
        val round = sessionPlan.getOrNull(roundIndex) ?: return
        currentUnicode = round.unicode
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                isComplete = false,
                currentStrokeIndex = 0,
                userStrokes = emptyList(),
                currentUserPoints = emptyList(),
                showNumbers = round.hintLevel != HintLevel.NONE
            )
            val charEntity = characterRepository.getCharacterByUnicode(round.unicode)
            val strokeEntities = characterRepository.getStrokeData(round.unicode)
            if (charEntity != null) {
                val character = characterRepository.buildDomainCharacter(charEntity, strokeEntities)
                val renderState = RenderState(character)
                val quiz = Quiz(character, renderState)
                quiz.start(Quiz.QuizOptions(
                    acceptBackwardsStrokes = true,
                    showHintAfterMisses = null,
                    onCorrectStroke = { result ->
                        _state.value = _state.value.copy(
                            currentStrokeIndex = result.strokeNum + 1
                        )
                    },
                    onMistake = { _ ->
                        soundManager.playMistakeSound()
                        soundManager.vibrate()
                        handleMistake(quiz, renderState)
                    },
                    onComplete = {
                        soundManager.playCharacterCompleteSound()
                        advanceToNextRound()
                    }
                ))
                configureRenderState(renderState, character, round.hintLevel)
                _state.value = _state.value.copy(
                    character = character,
                    quiz = quiz,
                    renderState = renderState,
                    isLoading = false
                )
            } else {
                advanceToNextRound()
            }
        }
    }

    private fun configureRenderState(
        renderState: RenderState,
        character: Character,
        hintLevel: HintLevel
    ) {
        when (hintLevel) {
            HintLevel.FULL, HintLevel.GRAYED -> { }
            HintLevel.HALF -> {
                val hiddenFrom = character.strokeCount / 2
                for (i in hiddenFrom until character.strokeCount) {
                    renderState.setStrokeOpacity("main", i.toString(), 0.0)
                }
            }
            HintLevel.NONE -> {
                for (i in 0 until character.strokeCount) {
                    renderState.setStrokeOpacity("main", i.toString(), 0.0)
                }
            }
        }
    }

    private fun handleMistake(quiz: Quiz, renderState: RenderState) {
        val strokeKey = quiz.currentStrokeIndex.toString()
        val mistakeCount = quiz.mistakesOnStroke
        val currentState = _state.value
        val mainOpacity = renderState.mainStrokes[strokeKey]?.opacity ?: 0.0

        if (mistakeCount >= 2 && mainOpacity <= 0.0 && !currentState.showNumbers) {
            _state.value = currentState.copy(showNumbers = true)
        }
        if (mistakeCount >= 4) {
            renderState.setStrokeOpacity("main", strokeKey, 0.3)
        }
    }

    private fun advanceToNextRound() {
        val nextIndex = _state.value.currentRoundIndex + 1
        if (nextIndex < sessionPlan.size) {
            _state.value = _state.value.copy(currentRoundIndex = nextIndex)
            loadCharacterRound(nextIndex)
        } else {
            _state.value = _state.value.copy(isComplete = true)
        }
    }

    fun onStrokeStart(offset: androidx.compose.ui.geometry.Offset) {
        val quiz = _state.value.quiz ?: return
        val point = com.hanziwriter.app.domain.model.geometry.Point(
            offset.x.toDouble(), offset.y.toDouble()
        )
        quiz.startUserStroke(point)
        _state.value = _state.value.copy(currentUserPoints = listOf(offset))
    }

    fun onStrokeMove(offset: androidx.compose.ui.geometry.Offset) {
        val quiz = _state.value.quiz ?: return
        val point = com.hanziwriter.app.domain.model.geometry.Point(
            offset.x.toDouble(), offset.y.toDouble()
        )
        quiz.continueUserStroke(point)
        _state.value = _state.value.copy(
            currentUserPoints = _state.value.currentUserPoints + offset
        )
    }

    fun onStrokeEnd() {
        val quiz = _state.value.quiz ?: return
        val unicode = currentUnicode
        quiz.endUserStroke { result ->
            viewModelScope.launch {
                progressRepository.saveStrokeAttempt(unicode, result.isCorrect, System.currentTimeMillis())
            }
        }
        _state.value = _state.value.copy(currentUserPoints = emptyList())
    }

    fun playLessonCompleteSound() {
        soundManager.playLessonCompleteSound()
    }

    fun setShowNumbers(show: Boolean) {
        _state.value = _state.value.copy(showNumbers = show)
    }
}
