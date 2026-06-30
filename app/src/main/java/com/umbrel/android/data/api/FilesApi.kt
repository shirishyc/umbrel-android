package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.FileEntry
import com.umbrel.android.data.models.FileInfo
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilesApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun list(path: String = "Home"): Result<List<FileEntry>> = runCatching {
        trpc.query(
            "files.list",
            params = mapOf("path" to JsonPrimitive(path)),
            deserializer = serializer(),
        ).getOrThrow()
    }

    suspend fun getInfo(path: String): Result<FileInfo> = runCatching {
        trpc.query(
            "files.getInfo",
            params = mapOf("path" to JsonPrimitive(path)),
            deserializer = serializer(),
        ).getOrThrow()
    }

    suspend fun delete(path: String): Result<Unit> = runCatching {
        trpc.mutation(
            "files.delete",
            params = mapOf("path" to JsonPrimitive(path)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun createDirectory(path: String): Result<Unit> = runCatching {
        trpc.mutation(
            "files.createDirectory",
            params = mapOf("path" to JsonPrimitive(path)),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }

    suspend fun rename(oldPath: String, newPath: String): Result<Unit> = runCatching {
        trpc.mutation(
            "files.rename",
            params = mapOf(
                "oldPath" to JsonPrimitive(oldPath),
                "newPath" to JsonPrimitive(newPath),
            ),
            deserializer = serializer<JsonNull>(),
        ).getOrThrow()
    }
}
