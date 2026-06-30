package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.Notification
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun list(): Result<List<Notification>> = runCatching {
        trpc.query("notifications.list", deserializer = serializer()).getOrThrow()
    }

    suspend fun dismiss(id: String): Result<Unit> = runCatching {
        trpc.mutation(
            "notifications.dismiss",
            params = mapOf("id" to JsonPrimitive(id)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun dismissAll(): Result<Unit> = runCatching {
        trpc.mutation("notifications.dismissAll", deserializer = serializer<JsonNull>()).getOrThrow()
    }
}
