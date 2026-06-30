package com.umbrel.android.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
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
 *
 * Auth is handled by the caller providing a JWT token, which is added
 * as an Authorization: Bearer header by OkHttp's AuthInterceptor.
 */
@Singleton
class TrpcClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    private var baseUrl: String = ""
    private var authToken: String? = null

    /** Set the server URL (e.g. "http://192.168.1.100" or "http://umbrel.local") */
    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun getBaseUrl(): String = baseUrl

    /** Set the JWT auth token */
    fun setAuthToken(token: String?) {
        authToken = token
    }

    /** Execute a tRPC query (read operation) */
    suspend fun <T> query(
        procedure: String,
        params: Map<String, JsonElement>? = null,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = execute("query", procedure, params, deserializer)

    /** Execute a tRPC mutation (write operation) */
    suspend fun <T> mutation(
        procedure: String,
        params: Map<String, JsonElement>? = null,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = execute("mutation", procedure, params, deserializer)

    private suspend fun <T> execute(
        method: String,
        procedure: String,
        params: Map<String, JsonElement>?,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = runCatching {
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
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            val errorResponse = try {
                json.decodeFromString<TrpcErrorResponse>(responseBody)
            } catch (_: Exception) {
                null
            }
            throw Exception(errorResponse?.error?.message ?: "HTTP ${response.code}")
        }

        val success = json.decodeFromString<TrpcSuccess<T>>(responseBody)
        success.result.data
    }
}
