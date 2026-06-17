package com.hanziwriter.app.ui.home

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.local.CharacterSetLoader
import com.hanziwriter.app.data.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val setDisplayName: String = "",
    val streakText: String = "Streak: 0 days",
    val engagementText: String = "Today: 0 min — No activity",
    val nextLearningChar: Int? = null,
    val nextReviewChar: Int? = null,
    val nextQuizChar: Int? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progressRepository: ProgressRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val setName: String = savedStateHandle.get<String>("setName") ?: ""

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
        val sets = CharacterSetLoader.listAvailableSets(context.assets)
        val match = sets.find { it.dirName == setName }
        val displayName = match?.displayName ?: setName

        val entries = CharacterSetLoader.loadFromAssets(context.assets, setName)
        val firstUnicode = entries.firstOrNull()?.unicode

        _state.value = _state.value.copy(
            setDisplayName = displayName,
            nextLearningChar = firstUnicode,
            nextReviewChar = firstUnicode,
            nextQuizChar = firstUnicode
        )
    }
}
