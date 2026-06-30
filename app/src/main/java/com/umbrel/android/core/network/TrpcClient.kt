package com.umbrel.android.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Lightweight Kotlin-native tRPC client.
 *
 * Communicates with the UmbrelOS backend via tRPC over HTTP POST.
 * All calls go to `http(s)://<host>/trpc`.
 */
@Singleton
class TrpcClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    private var baseUrl: String = ""
    private var authToken: String? = null

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun getBaseUrl(): String = baseUrl

    fun setAuthToken(token: String?) {
        authToken = token
    }

    /** Quick connectivity test — returns true if the server responds at /trpc */
    suspend fun checkConnectivity(): Result<Boolean> = runCatching {
        val result = queryRaw("system.status")
        result is kotlinx.serialization.json.JsonObject
    }

    /** Execute a tRPC query with a typed deserializer */
    suspend fun <T> query(
        procedure: String,
        params: Map<String, JsonElement>? = null,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = execute("query", procedure, params, deserializer)

    /** Execute a tRPC mutation with a typed deserializer */
    suspend fun <T> mutation(
        procedure: String,
        params: Map<String, JsonElement>? = null,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = execute("mutation", procedure, params, deserializer)

    /**
     * Execute a tRPC call and return the raw inner JSON element from result.data.
     * Avoids generic type erasure issues with TrpcSuccess<T>.
     */
    private suspend fun queryRaw(procedure: String): JsonElement = executeRaw("query", procedure, null)

    private suspend fun executeRaw(
        method: String,
        procedure: String,
        params: Map<String, JsonElement>?,
    ): JsonElement {
        val paramList = mutableListOf(json.encodeToJsonElement(procedure))
        if (params != null) {
            paramList.add(json.encodeToJsonElement(params))
        }

        val envelope = TrpcEnvelope(method = method, params = paramList)
        val body = json.encodeToString(TrpcEnvelope.serializer(), envelope)
            .toRequestBody("application/json".toMediaType())

        val requestBuilder = Request.Builder()
            .url("$baseUrl/trpc")
            .post(body)

        authToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from server")

        if (!response.isSuccessful) {
            val errorDetail = try {
                json.decodeFromString<TrpcErrorResponse>(responseBody)
            } catch (_: Exception) {
                null
            }
            throw Exception(errorDetail?.error?.message ?: "Server returned HTTP ${response.code}")
        }

        // Parse as raw JSON to avoid generic type erasure
        val root = json.parseToJsonElement(responseBody).jsonObject
        val resultObj = root["result"]?.jsonObject
            ?: throw Exception("Invalid tRPC response: missing 'result'")
        val data = resultObj["data"]
            ?: throw Exception("Invalid tRPC response: missing 'result.data'")

        return data
    }

    private suspend fun <T> execute(
        method: String,
        procedure: String,
        params: Map<String, JsonElement>?,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = runCatching {
        val data = executeRaw(method, procedure, params)
        json.decodeFromJsonElement(deserializer, data)
    }
}
