package com.hanziwriter.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.local.AppPreferences
import com.hanziwriter.app.data.local.CharacterSetLoader
import com.hanziwriter.app.data.repository.CharacterSetRepository
import com.hanziwriter.app.data.repository.ProgressRepository
import com.hanziwriter.app.domain.algorithm.CharacterSelector
import com.hanziwriter.app.domain.algorithm.ProgressInfo
import com.hanziwriter.app.domain.model.quiz.QuizCard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class HomeUiState(
    val setDisplayName: String = "",
    val hasValidSet: Boolean = true,
    val streakText: String = "Streak: 0 days",
    val engagementText: String = "Today: 0 min — No activity",
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
    appPreferences: AppPreferences,
    private val repository: CharacterSetRepository
) : ViewModel() {

    private val setName: String = appPreferences.selectedSetName ?: ""

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            loadSetInfo()
            val streak = progressRepository.getStreak()
            val streakDays = streak?.currentStreak ?: 0
            _state.value = _state.value.copy(
                streakText = if (streakDays > 0) "\uD83D\uDD25 Streak: $streakDays days" else "Start your streak!",
                engagementText = "Today: 0 min — Ready to practice"
            )
        }
    }

    private suspend fun loadSetInfo() {
        val setInfo = repository.findSetInfo(setName)

        if (setInfo == null) {
            _state.value = _state.value.copy(
                setDisplayName = "No set selected",
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

        val allUnicodes = cards.map { it.character.first().code }
        val allProgress = progressRepository.getAllProgressForSet(setName)
        val progressMap = allProgress.associate { it.unicode to ProgressInfo(it.lastPracticed, it.timesPracticed) }

        val learnUnicodes = CharacterSelector.select(allUnicodes, progressMap, count = 2)
        val drillUnicodes = CharacterSelector.select(allUnicodes, progressMap, count = 5)
        val quizUnicodes = CharacterSelector.select(allUnicodes, progressMap, count = 10)

        _state.value = _state.value.copy(
            setDisplayName = displayName,
            hasValidSet = true,
            nextLearningChars = learnUnicodes,
            nextReviewChars = drillUnicodes,
            nextQuizChars = quizUnicodes,
            learnCharacters = cards.filter { it.character.first().code in learnUnicodes },
            drillCharacters = cards.filter { it.character.first().code in drillUnicodes },
            quizCharacters = cards.filter { it.character.first().code in quizUnicodes }
        )
    }
}
