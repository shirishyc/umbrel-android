package com.umbrel.android.ui.screens.appstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.data.api.AppStoreApi
import com.umbrel.android.data.api.AppsApi
import com.umbrel.android.data.models.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val app: AppInfo? = null,
    val isInstalling: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val appStoreApi: AppStoreApi,
    private val appsApi: AppsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    fun loadApp(appId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            appStoreApi.getRegistry().fold(
                onSuccess = { apps ->
                    val app = apps.find { it.id == appId }
                    _uiState.value = AppDetailUiState(isLoading = false, app = app)
                },
                onFailure = { e ->
                    _uiState.value = AppDetailUiState(isLoading = false, error = e.message)
                },
            )
        }
    }

    fun installApp(appId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInstalling = true)
            appsApi.install(appId).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isInstalling = false) },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isInstalling = false, error = e.message)
                },
            )
        }
    }
}
