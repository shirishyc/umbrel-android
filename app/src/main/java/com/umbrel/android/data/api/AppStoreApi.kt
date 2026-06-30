package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.AppCategory
import com.umbrel.android.data.models.AppInfo
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStoreApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun getRegistry(): Result<List<AppInfo>> = runCatching {
        trpc.query("appStore.registry", deserializer = serializer()).getOrThrow()
    }

    suspend fun getCategories(): Result<List<AppCategory>> = runCatching {
        trpc.query("appStore.categories", deserializer = serializer()).getOrThrow()
    }

    suspend fun search(query: String): Result<List<AppInfo>> = runCatching {
        trpc.query(
            "appStore.search",
            params = mapOf("query" to JsonPrimitive(query)),
            deserializer = serializer(),
        ).getOrThrow()
    }
}
