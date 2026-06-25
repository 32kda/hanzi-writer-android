package com.hanziwriter.app.ui.setselector

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanziwriter.app.data.local.AppPreferences
import com.hanziwriter.app.data.local.CharacterSetInfo
import com.hanziwriter.app.data.repository.CharacterSetRepository
import com.hanziwriter.app.data.repository.ImportPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetSelectorUiState(
    val sets: List<CharacterSetInfo> = emptyList(),
    val isLoading: Boolean = true
)

sealed class ImportState {
    data object Idle : ImportState()
    data class Preview(val preview: ImportPreview, val uri: Uri) : ImportState()
    data class Importing(val name: String) : ImportState()
    data class Error(val message: String) : ImportState()
}

@HiltViewModel
class SetSelectorViewModel @Inject constructor(
    private val repository: CharacterSetRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SetSelectorUiState())
    val state: StateFlow<SetSelectorUiState> = _state.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    private var collectCount = 0

    init {
        Log.d("SetSelectorVM", "init: viewModel=${this.hashCode()}")
        viewModelScope.launch {
            Log.d("SetSelectorVM", "collect started")
            repository.sets.collect { sets ->
                collectCount++
                val names = sets.joinToString { it.dirName }
                val prev = _state.value.sets.joinToString { it.dirName }
                Log.d("SetSelectorVM", "collect #$collectCount: ${sets.size} sets [$names] prev=[$prev] isLoading=${_state.value.isLoading}")
                val newState = SetSelectorUiState(sets = sets, isLoading = false)
                val isSame = _state.value == newState
                Log.d("SetSelectorVM", "collect #$collectCount: _state.value==newState=$isSame oldSize=${_state.value.sets.size} newSize=${sets.size}")
                if (!isSame) {
                    _state.value = newState
                }
            }
            Log.d("SetSelectorVM", "collect ended")
        }
    }

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            try {
                val preview = repository.previewImport(uri)
                _importState.value = ImportState.Preview(preview, uri)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun confirmImport(overwrite: Boolean) {
        val current = _importState.value as? ImportState.Preview ?: return
        viewModelScope.launch {
            _importState.value = ImportState.Importing(current.preview.name)
            val result = repository.confirmImport(current.uri, overwrite)
            if (result.isSuccess) {
                _snackbarEvent.tryEmit("Imported '${current.preview.name}'")
                _importState.value = ImportState.Idle
            } else {
                _importState.value = ImportState.Error(
                    result.exceptionOrNull()?.message ?: "Import failed"
                )
            }
        }
    }

    fun dismissImport() {
        _importState.value = ImportState.Idle
    }

    fun deleteSet(dirName: String) {
        viewModelScope.launch {
            val result = repository.deleteSet(dirName)
            if (result.isSuccess) {
                if (dirName == appPreferences.selectedSetName) {
                    appPreferences.selectedSetName = null
                }
                _snackbarEvent.tryEmit("Deleted '$dirName'")
            } else {
                _snackbarEvent.tryEmit(result.exceptionOrNull()?.message ?: "Delete failed")
            }
        }
    }
}
