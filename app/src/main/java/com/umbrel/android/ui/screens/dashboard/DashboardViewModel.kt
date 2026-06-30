package com.umbrel.android.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.core.network.TrpcWebSocketClient
import com.umbrel.android.core.network.UmbrelEvents
import com.umbrel.android.data.local.CacheRepository
import com.umbrel.android.data.models.AppInfo
import com.umbrel.android.data.models.HardwareInfo
import com.umbrel.android.data.models.SystemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val systemStatus: SystemStatus? = null,
    val hardware: HardwareInfo? = null,
    val installedApps: List<AppInfo>? = null,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val cache: CacheRepository,
    private val wsClient: TrpcWebSocketClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeRealTimeEvents()
    }

    fun loadDashboard(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Parallel cache reads (cache handles TTL + auto-refresh)
            val statusResult = cache.getSystemStatus(forceRefresh).getOrNull()
            val hardwareResult = cache.getHardware(forceRefresh).getOrNull()
            val appsResult = cache.getApps(forceRefresh).getOrNull()

            _uiState.value = DashboardUiState(
                isLoading = false,
                systemStatus = statusResult ?: _uiState.value.systemStatus,
                hardware = hardwareResult ?: _uiState.value.hardware,
                installedApps = appsResult ?: _uiState.value.installedApps,
                error = if (statusResult == null && hardwareResult == null && appsResult == null
                    && _uiState.value.systemStatus == null) "Failed to load data" else null,
            )
        }
    }

    /**
     * Subscribe to real-time WebSocket events for live dashboard updates.
     */
    private fun observeRealTimeEvents() {
        viewModelScope.launch {
            wsClient.events<Map<String, String>>(UmbrelEvents.APPS_STATE_CHANGED)
                .collect { loadDashboard(forceRefresh = true) }
        }

        viewModelScope.launch {
            wsClient.events<Map<String, String>>(UmbrelEvents.HARDWARE_UPDATED)
                .collect { loadDashboard(forceRefresh = true) }
        }

        viewModelScope.launch {
            wsClient.events<Map<String, String>>(UmbrelEvents.SYSTEM_UPDATE_STATUS)
                .collect { loadDashboard(forceRefresh = true) }
        }
    }
}
