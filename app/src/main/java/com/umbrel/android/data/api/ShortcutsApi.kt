package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.Shortcut
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutsApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun list(): Result<List<Shortcut>> = runCatching {
        trpc.query("shortcuts.list", deserializer = serializer<List<Shortcut>>()).getOrThrow()
    }

    suspend fun create(url: String, title: String, icon: String? = null): Result<Shortcut> = runCatching {
        val params = mutableMapOf(
            "url" to JsonPrimitive(url),
            "title" to JsonPrimitive(title),
        )
        icon?.let { params["icon"] = JsonPrimitive(it) }
        trpc.mutation("shortcuts.create", params = params, deserializer = serializer<Shortcut>()).getOrThrow()
    }

    suspend fun delete(url: String): Result<Unit> = runCatching {
        trpc.mutation(
            "shortcuts.delete",
            params = mapOf("url" to JsonPrimitive(url)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }
}
