package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.SystemStatus
import com.umbrel.android.data.models.SoftwareUpdate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun getStatus(): Result<SystemStatus> = runCatching {
        trpc.query("system.status", deserializer = serializer()).getOrThrow()
    }

    suspend fun getSoftwareUpdate(): Result<SoftwareUpdate> = runCatching {
        trpc.query("system.softwareUpdate", deserializer = serializer()).getOrThrow()
    }

    suspend fun reboot(): Result<Unit> = runCatching {
        trpc.mutation("system.reboot", deserializer = serializer<JsonNull>()).getOrThrow()
    }

    suspend fun shutdown(): Result<Unit> = runCatching {
        trpc.mutation("system.shutdown", deserializer = serializer<JsonNull>()).getOrThrow()
    }

    suspend fun applyUpdate(): Result<Unit> = runCatching {
        trpc.mutation("system.applyUpdate", deserializer = serializer<JsonNull>()).getOrThrow()
    }

    suspend fun isTorEnabled(): Result<Boolean> = runCatching {
        trpc.query("system.isTorEnabled", deserializer = serializer<Boolean>()).getOrThrow()
    }

    suspend fun setTorEnabled(enabled: Boolean): Result<Unit> = runCatching {
        trpc.mutation(
            "system.setTorEnabled",
            params = mapOf("enabled" to JsonPrimitive(enabled)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }
}
