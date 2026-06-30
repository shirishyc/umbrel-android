package com.umbrel.android.ui.screens.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.core.network.TrpcWebSocketClient
import com.umbrel.android.core.network.UmbrelEvents
import com.umbrel.android.data.api.SystemApi
import com.umbrel.android.data.local.CacheRepository
import com.umbrel.android.data.models.HardwareInfo
import com.umbrel.android.data.models.SystemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.serializer
import javax.inject.Inject

data class SystemStatusUiState(
    val isLoading: Boolean = true,
    val systemStatus: SystemStatus? = null,
    val hardware: HardwareInfo? = null,
    val error: String? = null,
)

@HiltViewModel
class SystemStatusViewModel @Inject constructor(
    private val systemApi: SystemApi,
    private val cache: CacheRepository,
    private val wsClient: TrpcWebSocketClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemStatusUiState())
    val uiState: StateFlow<SystemStatusUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            wsClient.events(UmbrelEvents.HARDWARE_UPDATED, serializer<Map<String, String>>())
                .collect { load(forceRefresh = true) }
        }
        viewModelScope.launch {
            wsClient.events(UmbrelEvents.SYSTEM_UPDATE_STATUS, serializer<Map<String, String>>())
                .collect { load(forceRefresh = true) }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val status = cache.getSystemStatus(forceRefresh).getOrNull()
            val hardware = cache.getHardware(forceRefresh).getOrNull()
            _uiState.value = SystemStatusUiState(
                isLoading = false,
                systemStatus = status,
                hardware = hardware,
            )
        }
    }

    fun reboot() = viewModelScope.launch { systemApi.reboot() }
    fun shutdown() = viewModelScope.launch { systemApi.shutdown() }
    fun applyUpdate() = viewModelScope.launch { systemApi.applyUpdate() }
}
