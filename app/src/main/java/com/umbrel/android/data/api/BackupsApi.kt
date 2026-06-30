package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.Backup
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupsApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun list(): Result<List<Backup>> = runCatching {
        trpc.query("backups.list", deserializer = serializer<List<Backup>>()).getOrThrow()
    }

    suspend fun create(): Result<Backup> = runCatching {
        trpc.mutation("backups.create", deserializer = serializer<Backup>()).getOrThrow()
    }

    suspend fun restore(id: String): Result<Unit> = runCatching {
        trpc.mutation(
            "backups.restore",
            params = mapOf("id" to JsonPrimitive(id)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        trpc.mutation(
            "backups.delete",
            params = mapOf("id" to JsonPrimitive(id)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }
}
