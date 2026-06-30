package com.umbrel.android.ui.screens.appstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.data.api.AppStoreApi
import com.umbrel.android.data.models.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppStoreUiState(
    val isLoading: Boolean = true,
    val registry: List<AppInfo> = emptyList(),
    val searchResults: List<AppInfo> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class AppStoreViewModel @Inject constructor(
    private val appStoreApi: AppStoreApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppStoreUiState())
    val uiState: StateFlow<AppStoreUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            appStoreApi.getRegistry().fold(
                onSuccess = { apps ->
                    _uiState.value = AppStoreUiState(isLoading = false, registry = apps)
                },
                onFailure = { e ->
                    _uiState.value = AppStoreUiState(isLoading = false, error = e.message)
                },
            )
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            appStoreApi.search(query).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(searchResults = it) },
                onFailure = {},
            )
        }
    }
}
