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
    val diagnostics: List<String> = emptyList(),
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
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null, diagnostics = emptyList())
    }

    fun selectServer(server: DiscoveredServer) {
        _uiState.value = _uiState.value.copy(
            serverUrl = server.url,
            error = null,
            diagnostics = emptyList(),
        )
    }

    fun connect(onSuccess: () -> Unit) {
        val url = _uiState.value.serverUrl
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isConnecting = true,
                error = null,
                diagnostics = emptyList(),
            )

            val diag = mutableListOf<String>()

            // Step 1: Basic ping
            diag.add("🔍 Pinging $url ...")
            authRepository.pingServer(url).fold(
                onSuccess = { diag.add("  ✅ $it") },
                onFailure = { e ->
                    diag.add("  ❌ ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        diagnostics = diag,
                        error = e.message ?: "Server not reachable. Check:\n• Both devices on same WiFi\n• URL is correct\n• No VPN blocking",
                    )
                    return@launch
                },
            )

            // Step 2: Try tRPC call
            diag.add("🔍 Testing tRPC API at /trpc/system.status ...")
            authRepository.testTrpc(url).fold(
                onSuccess = { diag.add("  ✅ $it") },
                onFailure = { e ->
                    diag.add("  ❌ ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        diagnostics = diag,
                        error = "Server responded but tRPC failed.\n${e.message}",
                    )
                    return@launch
                },
            )

            // Step 3: Full login flow
            diag.add("🔍 Configuring server...")
            authRepository.configureServer(url).fold(
                onSuccess = {
                    diag.add("  ✅ Connected!")
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        diagnostics = diag,
                    )
                    onSuccess()
                },
                onFailure = { e ->
                    diag.add("  ❌ ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        diagnostics = diag,
                        error = e.message ?: "Connection failed",
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
