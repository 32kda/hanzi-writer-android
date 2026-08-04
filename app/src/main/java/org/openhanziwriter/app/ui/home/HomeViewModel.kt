package org.openhanziwriter.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.openhanziwriter.app.R
import org.openhanziwriter.app.data.local.AppPreferences
import org.openhanziwriter.app.data.local.CharacterSetLoader
import org.openhanziwriter.app.data.repository.CharacterSetRepository
import org.openhanziwriter.app.data.repository.ProgressRepository
import org.openhanziwriter.app.domain.algorithm.CharacterSelector
import org.openhanziwriter.app.domain.model.quiz.QuizCard
import org.openhanziwriter.app.ui.components.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val setDisplayName: String = "",
    val hasValidSet: Boolean = true,
    val streakText: UiText = UiText(R.string.home_streak_start),
    val engagementText: UiText = UiText(R.string.home_today_ready),
    val nextLearningChars: List<Int> = emptyList(),
    val nextReviewChars: List<Int> = emptyList(),
    val nextQuizChars: List<Int> = emptyList(),
    val learnCharacters: List<QuizCard> = emptyList(),
    val drillCharacters: List<QuizCard> = emptyList(),
    val quizCharacters: List<QuizCard> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progressRepository: ProgressRepository,
    private val appPreferences: AppPreferences,
    private val repository: CharacterSetRepository
) : ViewModel() {

    private val setName: String = appPreferences.selectedSetName ?: ""

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var allUnicodes: List<Int> = emptyList()
    private var allCards: List<QuizCard> = emptyList()

    init {
        viewModelScope.launch {
            progressRepository.loadAllProgress()
            loadSetInfo()
        }
        viewModelScope.launch {
            progressRepository.checkStreakOnStartup()
        }
        viewModelScope.launch {
            progressRepository.observeStreak().collect { streak ->
                val streakDays = streak?.currentStreak ?: 0
                _state.value = _state.value.copy(
                    streakText = if (streakDays > 0) UiText(R.string.home_streak_fmt, streakDays) else UiText(R.string.home_streak_start)
                )
            }
        }
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val minutes = progressRepository.getTotalMinutesForDate(today)
            _state.value = _state.value.copy(
                engagementText = if (minutes > 0) UiText(R.string.home_today_fmt, minutes) else UiText(R.string.home_today_ready)
            )
        }
    }

    fun refreshSetInfo() {
        viewModelScope.launch {
            val currentName = appPreferences.selectedSetName ?: ""
            if (currentName != setName) {
                _state.value = _state.value.copy(
                    setDisplayName = context.getString(R.string.home_no_set_selected),
                    hasValidSet = false
                )
            } else {
                val setInfo = repository.findSetInfo(setName)
                if (setInfo == null) {
                    _state.value = _state.value.copy(
                        setDisplayName = context.getString(R.string.home_no_set_selected),
                        hasValidSet = false
                    )
                }
            }
        }
    }

    fun refreshSelections() {
        if (allUnicodes.isEmpty()) return
        viewModelScope.launch {
            val progressMap = progressRepository.getProgressInfo(allUnicodes)

            val (learnUnicodes, drillUnicodes, quizUnicodes) = withContext(Dispatchers.Default) {
                Triple(
                    CharacterSelector.select(allUnicodes, progressMap, count = 2),
                    CharacterSelector.selectFromPracticed(allUnicodes, progressMap, count = 5),
                    CharacterSelector.selectFromPracticed(allUnicodes, progressMap, count = 10)
                )
            }

            _state.value = _state.value.copy(
                nextLearningChars = learnUnicodes,
                nextReviewChars = drillUnicodes,
                nextQuizChars = quizUnicodes,
                learnCharacters = allCards.filter { it.character.first().code in learnUnicodes },
                drillCharacters = allCards.filter { it.character.first().code in drillUnicodes },
                quizCharacters = allCards.filter { it.character.first().code in quizUnicodes }
            )
        }
    }

    private suspend fun loadSetInfo() {
        val setInfo = repository.findSetInfo(setName)

        if (setInfo == null) {
            _state.value = _state.value.copy(
                setDisplayName = context.getString(R.string.home_no_set_selected),
                hasValidSet = false
            )
            return
        }

        val displayName = setInfo.displayName

        val entries = withContext(Dispatchers.IO) {
            if (setInfo.isBuiltIn) {
                CharacterSetLoader.loadFromAssets(context.assets, setName)
            } else {
                val csvFile = File(repository.getCustomSetsDir(), "$setName/$setName.csv")
                CharacterSetLoader.loadFromCsv(csvFile)
            }
        }

        val cards = entries.map { entry ->
            QuizCard(
                character = entry.character,
                pinyin = entry.pinyin,
                translation = entry.translation
            )
        }

        allCards = cards
        allUnicodes = cards.map { it.character.first().code }

        _state.value = _state.value.copy(
            setDisplayName = displayName,
            hasValidSet = true
        )
        refreshSelections()
    }
}
