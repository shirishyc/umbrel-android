package com.umbrel.android.ui.screens.backups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.data.api.BackupsApi
import com.umbrel.android.data.models.Backup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupsUiState(
    val isLoading: Boolean = true,
    val backups: List<Backup> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val backupsApi: BackupsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupsUiState())
    val uiState: StateFlow<BackupsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            backupsApi.list().fold(
                onSuccess = { _uiState.value = BackupsUiState(isLoading = false, backups = it) },
                onFailure = { _uiState.value = BackupsUiState(isLoading = false, error = it.message) },
            )
        }
    }

    fun createBackup() = viewModelScope.launch { backupsApi.create(); load() }
    fun restoreBackup(id: String) = viewModelScope.launch { backupsApi.restore(id); load() }
    fun deleteBackup(id: String) = viewModelScope.launch { backupsApi.delete(id); load() }
}
