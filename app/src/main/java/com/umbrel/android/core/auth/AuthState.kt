package com.umbrel.android.core.auth

/**
 * Represents the authentication state for the UmbrelOS connection.
 */
sealed interface AuthState {
    /** No server URL has been configured yet */
    data object NeedsUrl : AuthState

    /** Server URL is set, but no password has been entered yet */
    data object NeedsLogin : AuthState

    /** Successfully authenticated */
    data class Authenticated(val token: String) : AuthState

    /** An error occurred */
    data class Error(val message: String) : AuthState
}
