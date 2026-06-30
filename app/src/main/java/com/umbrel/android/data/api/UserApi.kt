package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.UserProfile
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun getProfile(): Result<UserProfile> = runCatching {
        trpc.query("user.get", deserializer = serializer()).getOrThrow()
    }

    suspend fun setName(name: String): Result<Unit> = runCatching {
        trpc.mutation(
            "user.setName",
            params = mapOf("name" to JsonPrimitive(name)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun setPassword(password: String): Result<Unit> = runCatching {
        trpc.mutation(
            "user.setPassword",
            params = mapOf("password" to JsonPrimitive(password)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun setLanguage(language: String): Result<Unit> = runCatching {
        trpc.mutation(
            "user.setLanguage",
            params = mapOf("language" to JsonPrimitive(language)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun setWallpaper(wallpaper: String): Result<Unit> = runCatching {
        trpc.mutation(
            "user.setWallpaper",
            params = mapOf("wallpaper" to JsonPrimitive(wallpaper)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun setTemperatureUnit(unit: String): Result<Unit> = runCatching {
        trpc.mutation(
            "user.setTemperatureUnit",
            params = mapOf("temperatureUnit" to JsonPrimitive(unit)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun enable2fa(totpUri: String): Result<Unit> = runCatching {
        trpc.mutation(
            "user.enable2fa",
            params = mapOf("totpUri" to JsonPrimitive(totpUri)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun disable2fa(): Result<Unit> = runCatching {
        trpc.mutation("user.disable2fa", deserializer = serializer<JsonNull>()).getOrThrow()
    }
}
