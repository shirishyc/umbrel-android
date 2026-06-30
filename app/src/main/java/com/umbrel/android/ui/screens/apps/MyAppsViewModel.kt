package com.umbrel.android.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.core.network.TrpcWebSocketClient
import com.umbrel.android.core.network.UmbrelEvents
import com.umbrel.android.data.api.AppsApi
import com.umbrel.android.data.local.CacheRepository
import com.umbrel.android.data.models.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyAppsUiState(
    val isLoading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MyAppsViewModel @Inject constructor(
    private val appsApi: AppsApi,
    private val cache: CacheRepository,
    private val wsClient: TrpcWebSocketClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyAppsUiState())
    val uiState: StateFlow<MyAppsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        // Receive live app state changes
        viewModelScope.launch {
            wsClient.events<Map<String, String>>(UmbrelEvents.APPS_STATE_CHANGED)
                .collect { loadApps(forceRefresh = true) }
        }
    }

    private fun loadApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            cache.getApps(forceRefresh).fold(
                onSuccess = { _uiState.value = MyAppsUiState(isLoading = false, apps = it) },
                onFailure = { _uiState.value = MyAppsUiState(isLoading = false, error = it.message) },
            )
        }
    }

    fun startApp(appId: String) = viewModelScope.launch { appsApi.start(appId) }
    fun stopApp(appId: String) = viewModelScope.launch { appsApi.stop(appId) }
    fun restartApp(appId: String) = viewModelScope.launch { appsApi.restart(appId) }
    fun uninstallApp(appId: String) = viewModelScope.launch { appsApi.uninstall(appId) }
}
