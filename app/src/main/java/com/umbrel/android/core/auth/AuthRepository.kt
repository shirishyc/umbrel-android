package com.umbrel.android.core.auth

import com.umbrel.android.core.network.FileTransferService
import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.core.network.TrpcWebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles authentication with the UmbrelOS server.
 *
 * Flow: Set URL → Login with password → Receive JWT → Store securely.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val trpcClient: TrpcClient,
    private val tokenStore: TokenStore,
    private val wsClient: TrpcWebSocketClient,
    private val fileTransferService: FileTransferService,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NeedsUrl)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Restore persisted session
        val savedUrl = tokenStore.serverUrl
        val savedToken = tokenStore.jwtToken

        when {
            savedUrl != null && savedToken != null -> {
                trpcClient.setBaseUrl(savedUrl)
                trpcClient.setAuthToken(savedToken)
                _authState.value = AuthState.Authenticated(savedToken)
            }
            savedUrl != null -> {
                trpcClient.setBaseUrl(savedUrl)
                _authState.value = AuthState.NeedsLogin
            }
            else -> {
                _authState.value = AuthState.NeedsUrl
            }
        }
    }

    /**
     * Set the UmbrelOS server URL and test the connection.
     */
    suspend fun configureServer(url: String): Result<Unit> {
        return runCatching {
            val cleanUrl = url.trimEnd('/')
            trpcClient.setBaseUrl(cleanUrl)

            // Step 1: Quick connectivity test
            val connectivityResult = trpcClient.checkConnectivity()
            if (connectivityResult.isFailure) {
                val error = connectivityResult.exceptionOrNull()
                val message = when {
                    error?.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out. Check that: \n• Your Umbrel is powered on\n• Both devices are on the same WiFi network\n• The URL is correct"
                    error?.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Could not find '$url'. Try using the IP address instead of the hostname."
                    error?.message?.contains("Connection refused", ignoreCase = true) == true ->
                        "Connection refused. Is UmbrelOS running on port 80?"
                    else -> error?.message ?: "Connection failed"
                }
                throw Exception(message)
            }

            // Step 2: Verify it's an UmbrelOS by hitting system.status
            val result = trpcClient.query<kotlinx.serialization.json.JsonObject>(
                procedure = "system.status",
                deserializer = kotlinx.serialization.serializer(),
            )
            result.getOrThrow()

            tokenStore.serverUrl = cleanUrl
            _authState.value = AuthState.NeedsLogin
        }
    }

    /**
     * Login with the administrator password.
     * Returns the JWT token on success.
     */
    suspend fun login(password: String): Result<String> {
        return runCatching {
            val params = mapOf("password" to JsonPrimitive(password))
            val data = trpcClient.mutation<kotlinx.serialization.json.JsonObject>(
                procedure = "user.login",
                params = params,
                deserializer = kotlinx.serialization.serializer(),
            ).getOrThrow()

            // Extract the token from the login response
            val token = data["token"]?.let {
                when (it) {
                    is JsonPrimitive -> it.content
                    else -> null
                }
            } ?: throw Exception("Login failed: no token in response")

            // Persist token
            tokenStore.jwtToken = token
            trpcClient.setAuthToken(token)
            fileTransferService.configure(trpcClient.getBaseUrl())
            wsClient.configure(trpcClient.getBaseUrl(), token)
            wsClient.connect()

            _authState.value = AuthState.Authenticated(token)
            token
        }
    }

    /**
     * Logout and clear stored credentials.
     */
    suspend fun logout() {
        try {
            trpcClient.mutation<JsonNull>(
                procedure = "user.logout",
                deserializer = serializer(),
            )
        } catch (_: Exception) {
            // Best-effort — still clear local state
        }

        tokenStore.clearAll()
        trpcClient.setAuthToken(null)
        wsClient.disconnect()
        _authState.value = AuthState.NeedsLogin
    }

    /**
     * Check if the current session is still valid.
     */
    suspend fun checkSession(): Boolean {
        return runCatching {
            trpcClient.query<Map<String, kotlinx.serialization.json.JsonElement>>(
                procedure = "user.isLoggedIn",
                deserializer = serializer(),
            )
            true
        }.getOrDefault(false)
    }
}
