package com.umbrel.android.ui.screens.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.data.api.WifiApi
import com.umbrel.android.data.models.WifiConnectionStatus
import com.umbrel.android.data.models.WifiNetwork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WifiUiState(
    val isLoading: Boolean = true,
    val networks: List<WifiNetwork> = emptyList(),
    val currentConnection: WifiConnectionStatus? = null,
    val error: String? = null,
)

@HiltViewModel
class WifiViewModel @Inject constructor(
    private val wifiApi: WifiApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val networks = wifiApi.getNetworks().getOrNull() ?: emptyList()
            val connected = wifiApi.getConnected().getOrNull()
            _uiState.value = WifiUiState(isLoading = false, networks = networks, currentConnection = connected)
        }
    }
}
