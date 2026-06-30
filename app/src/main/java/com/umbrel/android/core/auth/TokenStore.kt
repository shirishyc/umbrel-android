package com.umbrel.android.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Securely stores the JWT token and server URL using EncryptedSharedPreferences.
 *
 * AES256-GCM encryption at rest on the device.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var jwtToken: String?
        get() = prefs.getString(KEY_JWT, null)
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_JWT).apply()
            } else {
                prefs.edit().putString(KEY_JWT, value).apply()
            }
        }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "umbrel_secure_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_JWT = "jwt_token"
    }
}
