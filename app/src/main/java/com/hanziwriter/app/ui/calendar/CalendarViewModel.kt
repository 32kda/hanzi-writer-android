package com.hanziwriter.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _practicedDays = MutableStateFlow<List<Int>>(emptyList())
    val practicedDays: StateFlow<List<Int>> = _practicedDays.asStateFlow()

    init {
        viewModelScope.launch {
            _practicedDays.value = progressRepository.getAllDaysPracticed()
        }
    }
}
