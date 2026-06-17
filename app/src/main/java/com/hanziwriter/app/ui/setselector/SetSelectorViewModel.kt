package com.hanziwriter.app.ui.setselector

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.local.CharacterSetInfo
import com.hanziwriter.app.data.local.CharacterSetLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetSelectorUiState(
    val sets: List<CharacterSetInfo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SetSelectorViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SetSelectorUiState())
    val state: StateFlow<SetSelectorUiState> = _state.asStateFlow()

    init {
        loadSets()
    }

    private fun loadSets() {
        viewModelScope.launch {
            val sets = CharacterSetLoader.listAvailableSets(context.assets)
            _state.value = SetSelectorUiState(sets = sets, isLoading = false)
        }
    }
}
