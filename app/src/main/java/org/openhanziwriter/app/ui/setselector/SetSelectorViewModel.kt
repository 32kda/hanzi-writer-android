package org.openhanziwriter.app.ui.setselector

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.openhanziwriter.app.R
import org.openhanziwriter.app.data.local.AppPreferences
import org.openhanziwriter.app.data.local.CharacterSetInfo
import org.openhanziwriter.app.data.repository.CharacterSetRepository
import org.openhanziwriter.app.data.repository.ImportException
import org.openhanziwriter.app.data.repository.ImportPreview
import org.openhanziwriter.app.ui.components.UiText
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
    data class Error(val message: UiText) : ImportState()
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

    private val _snackbarEvent = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val snackbarEvent: SharedFlow<UiText> = _snackbarEvent.asSharedFlow()

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
            } catch (e: ImportException) {
                _importState.value = ImportState.Error(UiText(e.resId, *e.args.toTypedArray()))
            } catch (e: Exception) {
                _importState.value = ImportState.Error(UiText(R.string.set_selector_import_failed))
            }
        }
    }

    fun confirmImport(overwrite: Boolean) {
        val current = _importState.value as? ImportState.Preview ?: return
        viewModelScope.launch {
            _importState.value = ImportState.Importing(current.preview.name)
            val result = repository.confirmImport(current.uri, overwrite)
            if (result.isSuccess) {
                _snackbarEvent.tryEmit(UiText(R.string.set_selector_imported, current.preview.name))
                _importState.value = ImportState.Idle
            } else {
                val e = result.exceptionOrNull()
                val message = if (e is ImportException) {
                    UiText(e.resId, *e.args.toTypedArray())
                } else {
                    UiText(R.string.set_selector_import_failed)
                }
                _importState.value = ImportState.Error(message)
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
                _snackbarEvent.tryEmit(UiText(R.string.set_selector_deleted, dirName))
            } else {
                val e = result.exceptionOrNull()
                val message = if (e is ImportException) {
                    UiText(e.resId, *e.args.toTypedArray())
                } else {
                    UiText(R.string.set_selector_delete_failed)
                }
                _snackbarEvent.tryEmit(message)
            }
        }
    }
}
