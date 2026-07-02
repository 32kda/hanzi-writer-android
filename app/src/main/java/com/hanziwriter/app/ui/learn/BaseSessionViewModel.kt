package com.hanziwriter.app.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.local.AppPreferences
import com.hanziwriter.app.data.repository.CharacterRepository
import com.hanziwriter.app.data.repository.CharacterSetRepository
import com.hanziwriter.app.data.repository.ProgressRepository
import com.hanziwriter.app.data.repository.SessionCharacterStats
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
import java.time.LocalDate

data class CharacterResult(
    val totalAttempts: Int,
    val correctAttempts: Int
)

data class DemoState(
    val strokeIndex: Int,
    val progress: Float
)

data class LearnUiState(
    val character: Character? = null,
    val quiz: Quiz? = null,
    val renderState: RenderState? = null,
    val isLoading: Boolean = true,
    val showNumbers: Boolean = true,
    val currentStrokeIndex: Int = 0,
    val userStrokes: List<com.hanziwriter.app.ui.components.DrawableUserStroke> = emptyList(),
    val currentUserPoints: List<androidx.compose.ui.geometry.Offset> = emptyList(),
    val isComplete: Boolean = false,
    val currentRoundIndex: Int = 0,
    val totalRounds: Int = 0,
    val sessionResults: Map<Int, CharacterResult> = emptyMap(),
    val sessionCharacters: List<Character> = emptyList(),
    val demoState: DemoState? = null
)

abstract class BaseSessionViewModel(
    protected val characterRepository: CharacterRepository,
    protected val progressRepository: ProgressRepository,
    protected val soundManager: SoundManager,
    protected val appPreferences: AppPreferences,
    protected val characterSetRepository: CharacterSetRepository
) : ViewModel() {

    protected val _state = MutableStateFlow(LearnUiState())
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    protected var sessionPlan: List<CharacterRound> = emptyList()
    private var currentUnicode: Int = 0
    private var csvEntryMap: Map<Int, com.hanziwriter.app.data.local.CharacterSetEntry> = emptyMap()

    private val sessionStats = mutableMapOf<Int, SessionCharacterStats>()
    private val loadedCharacters = mutableListOf<Character>()
    private val loadedUnicodes = mutableSetOf<Int>()
    private val animatedUnicodes = mutableSetOf<Int>()
    private var consecutiveCorrect: Int = 0
    private var sessionStartTime: Long = 0L

    abstract fun buildSessionPlan(unicodes: List<Int>): List<CharacterRound>

    abstract val sessionType: String

    fun startSession(unicodes: List<Int>) {
        sessionPlan = buildSessionPlan(unicodes)
        sessionStats.clear()
        loadedCharacters.clear()
        loadedUnicodes.clear()
        animatedUnicodes.clear()
        consecutiveCorrect = 0
        sessionStartTime = System.currentTimeMillis()
        csvEntryMap = emptyMap()
        _state.value = _state.value.copy(
            currentRoundIndex = 0,
            totalRounds = sessionPlan.size,
            sessionResults = emptyMap()
        )
        if (sessionPlan.isEmpty()) {
            _state.value = _state.value.copy(isComplete = true)
            return
        }
        viewModelScope.launch {
            val setName = appPreferences.selectedSetName ?: ""
            if (setName.isNotEmpty()) {
                csvEntryMap = characterSetRepository.loadCsvEntriesForSet(setName)
                    .associateBy { it.unicode }
            }
            loadCharacterRound(0)
        }
    }

    private fun loadCharacterRound(roundIndex: Int) {
        val round = sessionPlan.getOrNull(roundIndex) ?: return
        currentUnicode = round.unicode
        consecutiveCorrect = 0
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
                val csvEntry = csvEntryMap[round.unicode]
                val finalCharacter = if (csvEntry != null) {
                    character.copy(
                        pinyin = csvEntry.pinyin.ifBlank { character.pinyin },
                        definition = csvEntry.translation.ifBlank { character.definition }
                    )
                } else {
                    character
                }
                if (loadedUnicodes.add(round.unicode)) {
                    loadedCharacters.add(finalCharacter)
                }
                if (sessionType == "learn" && animatedUnicodes.add(round.unicode)) {
                    val renderState = RenderState(finalCharacter)
                    configureRenderState(renderState, finalCharacter, round.hintLevel)
                    _state.value = _state.value.copy(
                        character = finalCharacter,
                        renderState = renderState,
                        quiz = null,
                        isLoading = false,
                        demoState = DemoState(0, 0f)
                    )
                } else {
                    startQuizForRound(finalCharacter, round, RenderState(finalCharacter))
                }
            } else {
                advanceToNextRound()
            }
        }
    }

    private fun startQuizForRound(character: Character, round: CharacterRound, renderState: RenderState) {
        val quiz = Quiz(character, renderState)
        quiz.start(Quiz.QuizOptions(
            acceptBackwardsStrokes = true,
            showHintAfterMisses = null,
            onCorrectStroke = { result ->
                if (_state.value.quiz == quiz &&
                    result.strokeNum + 1 < character.strokeCount
                ) {
                    _state.value = _state.value.copy(
                        currentStrokeIndex = result.strokeNum + 1
                    )
                }
            },
            onMistake = { _ ->
                soundManager.playMistakeSound()
                soundManager.vibrate()
                handleMistake(quiz, renderState)
            },
            onComplete = {
                soundManager.playCharacterCompleteSound()
                viewModelScope.launch {
                    kotlinx.coroutines.delay(300L)
                    advanceToNextRound()
                }
            }
        ))
        configureRenderState(renderState, character, round.hintLevel)
        _state.value = _state.value.copy(
            character = character,
            quiz = quiz,
            renderState = renderState,
            isLoading = false,
            demoState = null
        )
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

    fun skipDemo() {
        val currentState = _state.value
        val roundIndex = currentState.currentRoundIndex
        val round = sessionPlan.getOrNull(roundIndex) ?: return
        val character = currentState.character ?: return
        startQuizForRound(character, round, RenderState(character))
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
            _state.value = _state.value.copy(
                currentRoundIndex = nextIndex,
                currentStrokeIndex = 0,
                isLoading = true,
                quiz = null
            )
            loadCharacterRound(nextIndex)
        } else {
            _state.value = _state.value.copy(
                isComplete = true,
                sessionResults = sessionStats.mapValues { (_, stats) ->
                    CharacterResult(stats.totalAttempts, stats.correctAttempts)
                },
                sessionCharacters = loadedCharacters.toList()
            )
        }
    }

    fun onStrokeStart(offset: androidx.compose.ui.geometry.Offset) {
        if (_state.value.demoState != null) return
        val quiz = _state.value.quiz ?: return
        val point = com.hanziwriter.app.domain.model.geometry.Point(
            offset.x.toDouble(), offset.y.toDouble()
        )
        quiz.startUserStroke(point)
        _state.value = _state.value.copy(currentUserPoints = listOf(offset))
    }

    fun onStrokeMove(offset: androidx.compose.ui.geometry.Offset) {
        if (_state.value.demoState != null) return
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
        if (_state.value.demoState != null) return
        val quiz = _state.value.quiz ?: return
        val unicode = currentUnicode
        quiz.endUserStroke { result ->
            val stats = sessionStats.getOrPut(unicode) {
                SessionCharacterStats(unicode, 0, 0)
            }
            if (result.isCorrect) {
                consecutiveCorrect++
                sessionStats[unicode] = stats.copy(
                    totalAttempts = stats.totalAttempts + 1,
                    correctAttempts = stats.correctAttempts + 1
                )
            } else {
                consecutiveCorrect = 0
                sessionStats[unicode] = stats.copy(
                    totalAttempts = stats.totalAttempts + 1
                )
            }
        }
        _state.value = _state.value.copy(currentUserPoints = emptyList())
    }

    fun endSession() {
        val elapsedMs = System.currentTimeMillis() - sessionStartTime
        val minutes = (elapsedMs / 60_000).toInt().coerceAtLeast(1)
        val today = LocalDate.now().toString()
        val setName = appPreferences.selectedSetName ?: ""
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            progressRepository.endSession(
                setName = setName,
                characterStats = sessionStats.values.toList(),
                activityType = sessionType,
                sessionMinutes = minutes,
                date = today,
                timestamp = now
            )
        }
    }

    fun playLessonCompleteSound() {
        soundManager.playLessonCompleteSound()
    }

    fun setShowNumbers(show: Boolean) {
        _state.value = _state.value.copy(showNumbers = show)
    }
}
