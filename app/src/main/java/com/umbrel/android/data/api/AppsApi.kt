package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.AppInfo
import com.umbrel.android.data.models.AppState
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun list(): Result<List<AppInfo>> = runCatching {
        trpc.query("apps.list", deserializer = serializer<List<AppInfo>>()).getOrThrow()
    }

    suspend fun getState(appId: String): Result<AppState> = runCatching {
        trpc.query(
            "apps.state",
            params = mapOf("appId" to JsonPrimitive(appId)),
            deserializer = serializer<AppState>(),
        ).getOrThrow()
    }

    suspend fun install(appId: String): Result<Unit> = runCatching {
        trpc.mutation(
            "apps.install",
            params = mapOf("appId" to JsonPrimitive(appId)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun uninstall(appId: String): Result<Unit> = runCatching {
        trpc.mutation(
            "apps.uninstall",
            params = mapOf("appId" to JsonPrimitive(appId)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun start(appId: String): Result<Unit> = runCatching {
        trpc.mutation(
            "apps.start",
            params = mapOf("appId" to JsonPrimitive(appId)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun stop(appId: String): Result<Unit> = runCatching {
        trpc.mutation(
            "apps.stop",
            params = mapOf("appId" to JsonPrimitive(appId)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun restart(appId: String): Result<Unit> = runCatching {
        trpc.mutation(
            "apps.restart",
            params = mapOf("appId" to JsonPrimitive(appId)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun update(appId: String): Result<Unit> = runCatching {
        trpc.mutation(
            "apps.update",
            params = mapOf("appId" to JsonPrimitive(appId)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }
}
