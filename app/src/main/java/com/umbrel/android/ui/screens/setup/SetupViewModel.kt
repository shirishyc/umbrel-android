package com.umbrel.android.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.core.auth.AuthRepository
import com.umbrel.android.core.discovery.DiscoveredServer
import com.umbrel.android.core.discovery.ServerDiscovery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val serverUrl: String = "http://umbrel.local",
    val isConnecting: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val isScanning: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverDiscovery: ServerDiscovery,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        startDiscovery()
    }

    private fun startDiscovery() {
        viewModelScope.launch {
            // Collect discovered servers
            serverDiscovery.discoverServers().collect { server ->
                val current = _uiState.value.discoveredServers
                if (current.none { it.ipAddress == server.ipAddress }) {
                    _uiState.value = _uiState.value.copy(
                        discoveredServers = current + server,
                        isScanning = false,
                    )
                }
            }
        }

        // Also try resolving umbrel.local directly as fallback
        viewModelScope.launch {
            val resolved = serverDiscovery.resolveUmbrelLocal()
            if (resolved != null) {
                val current = _uiState.value.discoveredServers
                if (current.none { it.ipAddress == resolved.ipAddress }) {
                    _uiState.value = _uiState.value.copy(
                        discoveredServers = current + resolved,
                        isScanning = false,
                    )
                }
            } else {
                // No direct resolution, scanning may find it
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun selectServer(server: DiscoveredServer) {
        _uiState.value = _uiState.value.copy(
            serverUrl = server.url,
            error = null,
        )
    }

    fun connect(onSuccess: () -> Unit) {
        val url = _uiState.value.serverUrl
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)
            authRepository.configureServer(url).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isConnecting = false)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = e.message ?: "Connection failed. Check the URL and try again.",
                    )
                },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        serverDiscovery.stop()
    }
}
